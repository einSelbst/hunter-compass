package dev.einselbst.huntercompass;

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
    private BukkitTask trackingTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        state = new EventStateStore(this);
        state.load();
        items = new CompassItemService(this);
        tracking = new TrackingService(this, state, items);

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
        }
    }

    @Override
    public void onDisable() {
        if (trackingTask != null) {
            trackingTask.cancel();
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
        trackingTask = getServer().getScheduler().runTaskTimer(this, tracking::update, 1L, interval);
    }

    void stopEvent() {
        state.stop();
        for (Player player : getServer().getOnlinePlayers()) {
            items.removeAll(player);
            player.sendActionBar(Component.empty());
        }
    }

    void announceVictory(Player hunter, Player target) {
        String message = getConfig().getString(
                "messages.victory",
                "<gold><bold>{hunter}</bold></gold> caught <red><bold>{target}</bold></red>!"
        ).replace("{hunter}", hunter.getName()).replace("{target}", target.getName());
        getServer().broadcast(miniMessage.deserialize(message));
    }
}
