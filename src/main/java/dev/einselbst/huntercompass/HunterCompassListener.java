package dev.einselbst.huntercompass;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

final class HunterCompassListener implements Listener {
    private final HunterCompassPlugin plugin;
    private final EventStateStore state;
    private final CompassItemService items;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    HunterCompassListener(HunterCompassPlugin plugin, EventStateStore state, CompassItemService items) {
        this.plugin = plugin;
        this.state = state;
        this.items = items;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!state.isActive()) {
            return;
        }
        Player player = event.getPlayer();
        plugin.showBossBar(player);
        if (player.getUniqueId().equals(state.targetId())) {
            items.removeAll(player);
            return;
        }
        if (!state.isHunter(player.getUniqueId())
                && plugin.getConfig().getBoolean("auto-enroll-eligible-players", true)
                && player.hasPermission("huntercompass.hunter")) {
            state.addHunter(player.getUniqueId());
        }
        if (state.isHunter(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> items.ensureSingle(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!items.isPluginCompass(event.getItemDrop().getItemStack())
                || !plugin.getConfig().getBoolean("compass.prevent-dropping", true)) {
            return;
        }
        event.setCancelled(true);
        String message = plugin.getConfig().getString(
                "messages.drop-blocked",
                "<red>The Hunter Compass cannot be dropped.</red>"
        );
        event.getPlayer().sendActionBar(miniMessage.deserialize(message));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (items.isPluginCompass(event.getItem().getItemStack())
                && !items.belongsTo(event.getItem().getItemStack(), player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onContainerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !plugin.getConfig().getBoolean("compass.prevent-container-storage", true)
                || isPlayerInventoryView(event.getView().getTopInventory().getType())) {
            return;
        }

        boolean involvesCompass = items.isPluginCompass(event.getCurrentItem())
                || items.isPluginCompass(event.getCursor());
        if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            involvesCompass |= items.isPluginCompass(player.getInventory().getItem(event.getHotbarButton()));
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            involvesCompass |= items.isPluginCompass(player.getInventory().getItemInOffHand());
        }
        if (involvesCompass) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onContainerDrag(InventoryDragEvent event) {
        if (!plugin.getConfig().getBoolean("compass.prevent-container-storage", true)
                || isPlayerInventoryView(event.getView().getTopInventory().getType())
                || !items.isPluginCompass(event.getOldCursor())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTargetDeath(PlayerDeathEvent event) {
        if (!state.isActive() || !event.getPlayer().getUniqueId().equals(state.targetId())) {
            return;
        }
        if (plugin.finishIfExpired()) {
            return;
        }
        Player killer = event.getPlayer().getKiller();
        if (killer == null || !state.isHunter(killer.getUniqueId())) {
            return;
        }
        plugin.announceVictory(killer, event.getPlayer());
        plugin.getServer().getScheduler().runTask(plugin, plugin::stopEvent);
    }

    private boolean isPlayerInventoryView(InventoryType type) {
        return type == InventoryType.CRAFTING || type == InventoryType.CREATIVE;
    }
}
