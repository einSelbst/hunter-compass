package dev.einselbst.huntercompass;

import dev.einselbst.huntercompass.domain.HuntClock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HunterCompassPlugin extends JavaPlugin {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private EventStateStore state;
    private CompassItemService items;
    private TrackingService tracking;
    private HuntBossBarService bossBar;
    private BukkitTask trackingTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        state = new EventStateStore(this);
        state.load();
        items = new CompassItemService(this);
        tracking = new TrackingService(this, state, items);
        bossBar = new HuntBossBarService(this, state);

        HunterCompassCommand command = new HunterCompassCommand(this, state, items);
        PluginCommand pluginCommand = getCommand("huntercompass");
        if (pluginCommand == null) {
            throw new IllegalStateException("huntercompass command is missing from plugin.yml");
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new HunterCompassListener(this, state, items), this);
        scheduleTracking();

        if (state.isActive()) {
            getLogger().info("Restored active event for target " + state.targetName()
                    + " with " + state.hunters().size() + " hunter(s).");
            bossBar.showToOnlinePlayers();
        }
    }

    @Override
    public void onDisable() {
        if (trackingTask != null) {
            trackingTask.cancel();
        }
        if (bossBar != null) {
            bossBar.hideFromOnlinePlayers();
        }
    }

    void reloadPlugin() {
        reloadConfig();
        scheduleTracking();
    }

    void scheduleTracking() {
        if (trackingTask != null) {
            trackingTask.cancel();
        }
        long interval = Math.max(1L, getConfig().getLong("update-interval-ticks", 10L));
        trackingTask = getServer().getScheduler().runTaskTimer(this, this::updateEvent, 1L, interval);
    }

    void startEvent(Player target) {
        stopEvent();
        long startedAtMillis = System.currentTimeMillis();
        int durationDays = Math.max(1, getConfig().getInt("duration-real-days", 7));
        state.start(
                target.getUniqueId(),
                target.getName(),
                startedAtMillis,
                HuntClock.deadline(startedAtMillis, durationDays)
        );
        bossBar.update(startedAtMillis);
    }

    void stopEvent() {
        bossBar.hideFromOnlinePlayers();
        state.stop();
        for (Player player : getServer().getOnlinePlayers()) {
            items.removeAll(player);
            player.sendActionBar(Component.empty());
        }
    }

    void showBossBar(Player player) {
        bossBar.show(player);
    }

    boolean finishIfExpired() {
        if (!state.isActive() || !HuntClock.isExpired(state.deadlineMillis(), System.currentTimeMillis())) {
            return false;
        }
        announceTargetVictory(state.targetName());
        stopEvent();
        return true;
    }

    private void updateEvent() {
        if (finishIfExpired()) {
            return;
        }
        if (state.isActive()) {
            bossBar.update(System.currentTimeMillis());
            tracking.update();
        }
    }

    void announceVictory(Player hunter, Player target) {
        String message = getConfig().getString(
                "messages.victory",
                "<gold><bold>{hunter}</bold></gold> caught <red><bold>{target}</bold></red>!"
        ).replace("{hunter}", hunter.getName()).replace("{target}", target.getName());
        getServer().broadcast(miniMessage.deserialize(message));
    }

    private void announceTargetVictory(String targetName) {
        int durationDays = HuntClock.totalDays(state.startedAtMillis(), state.deadlineMillis());
        String message = getConfig().getString(
                "messages.target-victory",
                "<green><bold>{target}</bold></green> survived {days} real days and won the hunt!"
        ).replace("{target}", targetName).replace("{days}", Integer.toString(durationDays));
        getServer().broadcast(miniMessage.deserialize(message));
    }
}
