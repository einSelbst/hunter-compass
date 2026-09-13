package dev.einselbst.huntercompass;

import dev.einselbst.huntercompass.domain.HuntClock;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Locale;

final class HuntBossBarService {
    private final HunterCompassPlugin plugin;
    private final EventStateStore state;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final BossBar bossBar = BossBar.bossBar(
            miniMessage.deserialize("<gold>HunterCompass</gold>"),
            1.0f,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
    );

    HuntBossBarService(HunterCompassPlugin plugin, EventStateStore state) {
        this.plugin = plugin;
        this.state = state;
    }

    void show(Player player) {
        if (state.isActive() && plugin.getConfig().getBoolean("boss-bar.enabled", true)) {
            player.showBossBar(bossBar);
        }
    }

    void showToOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            show(player);
        }
    }

    void hideFromOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    void update(long nowMillis) {
        if (!state.isActive()) {
            return;
        }
        if (!plugin.getConfig().getBoolean("boss-bar.enabled", true)) {
            hideFromOnlinePlayers();
            return;
        }

        int day = HuntClock.displayDay(state.startedAtMillis(), state.deadlineMillis(), nowMillis);
        int totalDays = HuntClock.totalDays(state.startedAtMillis(), state.deadlineMillis());
        String template = plugin.getConfig().getString(
                "boss-bar.format",
                "<gold>Jagd auf <red><bold>{target}</bold></red> <dark_gray>•</dark_gray> <yellow>Tag {day}/{total-days}</yellow>"
        );
        String rendered = template
                .replace("{target}", state.targetName())
                .replace("{day}", Integer.toString(day))
                .replace("{total-days}", Integer.toString(totalDays));

        bossBar.name(miniMessage.deserialize(rendered));
        bossBar.progress(HuntClock.remainingFraction(state.startedAtMillis(), state.deadlineMillis(), nowMillis));
        bossBar.color(configuredColor());
        showToOnlinePlayers();
    }

    private BossBar.Color configuredColor() {
        String value = plugin.getConfig().getString("boss-bar.color", "RED");
        try {
            return BossBar.Color.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BossBar.Color.RED;
        }
    }
}
