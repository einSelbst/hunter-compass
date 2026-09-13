package dev.einselbst.huntercompass;

import dev.einselbst.huntercompass.domain.ItemIdentity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CompassItemService {
    enum GiveResult {
        CREATED,
        ALREADY_PRESENT,
        INVENTORY_FULL
    }

    private final HunterCompassPlugin plugin;
    private final MiniMessage miniMessage;
    private final NamespacedKey markerKey;
    private final NamespacedKey ownerKey;

    CompassItemService(HunterCompassPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.markerKey = new NamespacedKey(plugin, "hunter_compass");
        this.ownerKey = new NamespacedKey(plugin, "owner");
    }

    GiveResult ensureSingle(Player player) {
        boolean found = false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!isPluginCompass(item)) {
                continue;
            }
            if (!found && belongsTo(item, player.getUniqueId())) {
                found = true;
            } else {
                player.getInventory().setItem(slot, null);
            }
        }

        if (found) {
            return GiveResult.ALREADY_PRESENT;
        }

        ItemStack compass = create(player.getUniqueId());
        if (player.getInventory().addItem(compass).isEmpty()) {
            return GiveResult.CREATED;
        }
        return GiveResult.INVENTORY_FULL;
    }

    void removeAll(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isPluginCompass(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    void updateNeedle(Player player, Location destination) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!belongsTo(item, player.getUniqueId())) {
                continue;
            }
            item.editMeta(CompassMeta.class, meta -> {
                meta.setLodestone(destination);
                meta.setLodestoneTracked(false);
            });
        }
    }

    void makeNeedleSpin(Player player) {
        World otherWorld = plugin.getServer().getWorlds().stream()
                .filter(world -> !world.getKey().equals(player.getWorld().getKey()))
                .findFirst()
                .orElse(null);
        Location destination = otherWorld == null
                ? player.getLocation()
                : new Location(otherWorld, 0.5, 64, 0.5);
        updateNeedle(player, destination);
    }

    boolean isPluginCompass(ItemStack item) {
        return item != null
                && item.getType() == Material.COMPASS
                && item.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    boolean belongsTo(ItemStack item, UUID playerId) {
        if (!isPluginCompass(item)) {
            return false;
        }
        Byte marker = item.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String owner = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ItemIdentity.isPluginCompass(marker, owner, playerId);
    }

    private ItemStack create(UUID owner) {
        ItemStack compass = ItemStack.of(Material.COMPASS);
        compass.editMeta(CompassMeta.class, meta -> {
            meta.displayName(miniMessage.deserialize(plugin.getConfig().getString(
                    "compass.name", "<gold><bold>Hunter Compass</bold></gold>")));
            List<Component> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("compass.lore")) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, ItemIdentity.MARKER);
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
            meta.setLodestoneTracked(false);
        });
        return compass;
    }
}
