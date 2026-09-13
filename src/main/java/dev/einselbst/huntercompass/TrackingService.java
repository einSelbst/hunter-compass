package dev.einselbst.huntercompass;

import dev.einselbst.huntercompass.domain.BlockPosition;
import dev.einselbst.huntercompass.domain.CompassGuidance;
import dev.einselbst.huntercompass.domain.CompassTargetCalculator;
import dev.einselbst.huntercompass.domain.DimensionId;
import dev.einselbst.huntercompass.domain.DimensionNames;
import dev.einselbst.huntercompass.domain.GuidanceMode;
import dev.einselbst.huntercompass.domain.TrackingText;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

final class TrackingService {
    private final HunterCompassPlugin plugin;
    private final EventStateStore state;
    private final CompassItemService items;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    TrackingService(HunterCompassPlugin plugin, EventStateStore state, CompassItemService items) {
        this.plugin = plugin;
        this.state = state;
        this.items = items;
    }

    void update() {
        if (!state.isActive()) {
            return;
        }

        Player target = plugin.getServer().getPlayer(state.targetId());
        for (UUID hunterId : state.hunters()) {
            Player hunter = plugin.getServer().getPlayer(hunterId);
            if (hunter == null || hunter.getUniqueId().equals(state.targetId())) {
                continue;
            }

            items.ensureSingle(hunter);
            if (target == null) {
                items.makeNeedleSpin(hunter);
                sendOfflineActionBar(hunter);
                continue;
            }

            DimensionId hunterDimension = dimensionOf(hunter.getWorld());
            DimensionId targetDimension = dimensionOf(target.getWorld());
            BlockPosition hunterPosition = positionOf(hunter.getLocation());
            BlockPosition targetPosition = positionOf(target.getLocation());
            CompassGuidance guidance = CompassTargetCalculator.calculate(
                    hunterDimension,
                    hunterPosition,
                    targetDimension,
                    targetPosition
            );

            World needleWorld = guidance.mode() == GuidanceMode.UNAVAILABLE
                    ? target.getWorld()
                    : hunter.getWorld();
            BlockPosition needle = guidance.needleTarget();
            items.updateNeedle(hunter, new Location(
                    needleWorld,
                    needle.x() + 0.5,
                    needle.y(),
                    needle.z() + 0.5
            ));
            sendTrackingActionBar(hunter, target.getName(), targetPosition, targetDimension, guidance.mode());
        }
    }

    private void sendTrackingActionBar(
            Player hunter,
            String targetName,
            BlockPosition targetPosition,
            DimensionId targetDimension,
            GuidanceMode mode
    ) {
        DimensionNames names = new DimensionNames(
                plugin.getConfig().getString("dimensions.overworld", "Overworld"),
                plugin.getConfig().getString("dimensions.nether", "Nether"),
                plugin.getConfig().getString("dimensions.end", "The End"),
                plugin.getConfig().getString("dimensions.other", "Other")
        );
        String labelPath = switch (mode) {
            case EXACT -> "direction-labels.exact";
            case SCALED_PORTAL -> "direction-labels.portal";
            case UNAVAILABLE -> "direction-labels.unavailable";
        };
        String direction = plugin.getConfig().getString(labelPath, mode.name());
        String template = plugin.getConfig().getString(
                "action-bar.format",
                "<gold>{target}</gold> | X {x} Y {y} Z {z} | {dimension} | {direction}"
        );
        String rendered = TrackingText.format(
                template,
                targetName,
                targetPosition,
                names.displayName(targetDimension),
                direction
        );
        hunter.sendActionBar(miniMessage.deserialize(rendered));
    }

    private void sendOfflineActionBar(Player hunter) {
        String template = plugin.getConfig().getString(
                "action-bar.offline-format",
                "<gold>{target}</gold> | <red>{offline}</red>"
        );
        String offline = plugin.getConfig().getString("direction-labels.offline", "target offline");
        hunter.sendActionBar(miniMessage.deserialize(TrackingText.formatOffline(
                template,
                state.targetName(),
                offline
        )));
    }

    static DimensionId dimensionOf(World world) {
        return DimensionId.fromKey(world.getKey().asString());
    }

    static BlockPosition positionOf(Location location) {
        return BlockPosition.from(location.getX(), location.getY(), location.getZ());
    }
}
