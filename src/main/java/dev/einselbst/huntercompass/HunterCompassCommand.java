package dev.einselbst.huntercompass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HunterCompassCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "status", "give", "remove", "reload");

    private final HunterCompassPlugin plugin;
    private final EventStateStore state;
    private final CompassItemService items;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    HunterCompassCommand(HunterCompassPlugin plugin, EventStateStore state, CompassItemService items) {
        this.plugin = plugin;
        this.state = state;
        this.items = items;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("huntercompass.admin")) {
            reply(sender, "<red>You do not have permission.</red>");
            return true;
        }
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args);
            case "stop" -> stop(sender);
            case "status" -> status(sender);
            case "give" -> give(sender, args);
            case "remove" -> remove(sender, args);
            case "reload" -> reload(sender);
            default -> {
                showHelp(sender, label);
                yield true;
            }
        };
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 2) {
            reply(sender, "<red>Usage: /huntercompass start <target></red>");
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            reply(sender, "<red>The target must be online when the event starts.</red>");
            return true;
        }

        plugin.stopEvent();
        state.start(target.getUniqueId(), target.getName());
        int enrolled = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getUniqueId().equals(target.getUniqueId())
                    || !player.hasPermission("huntercompass.hunter")) {
                continue;
            }
            state.addHunter(player.getUniqueId());
            items.ensureSingle(player);
            enrolled++;
        }
        reply(sender, "<green>Hunt started.</green> Target: <gold>" + target.getName()
                + "</gold>; hunters: <yellow>" + enrolled + "</yellow>.");
        return true;
    }

    private boolean stop(CommandSender sender) {
        if (!state.isActive()) {
            reply(sender, "<yellow>No hunt is active.</yellow>");
            return true;
        }
        plugin.stopEvent();
        reply(sender, "<green>Hunt stopped and Hunter Compasses removed.</green>");
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!state.isActive()) {
            reply(sender, "<yellow>No hunt is active.</yellow>");
            return true;
        }
        boolean online = plugin.getServer().getPlayer(state.targetId()) != null;
        reply(sender, "<green>Active</green> target: <gold>" + state.targetName()
                + "</gold> (" + (online ? "online" : "offline") + "), hunters: <yellow>"
                + state.hunters().size() + "</yellow>.");
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!state.isActive()) {
            reply(sender, "<red>Start an event before issuing compasses.</red>");
            return true;
        }
        if (args.length != 2) {
            reply(sender, "<red>Usage: /huntercompass give <player|all></red>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int count = 0;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getUniqueId().equals(state.targetId())) {
                    continue;
                }
                state.addHunter(player.getUniqueId());
                if (items.ensureSingle(player) != CompassItemService.GiveResult.INVENTORY_FULL) {
                    count++;
                }
            }
            reply(sender, "<green>Hunter Compasses ensured for " + count + " online player(s).</green>");
            return true;
        }

        Player player = plugin.getServer().getPlayerExact(args[1]);
        if (player == null) {
            reply(sender, "<red>That player is not online.</red>");
            return true;
        }
        if (player.getUniqueId().equals(state.targetId())) {
            reply(sender, "<red>The target cannot be enrolled as a hunter.</red>");
            return true;
        }
        state.addHunter(player.getUniqueId());
        CompassItemService.GiveResult result = items.ensureSingle(player);
        if (result == CompassItemService.GiveResult.INVENTORY_FULL) {
            reply(sender, "<red>" + player.getName() + " has no free inventory slot.</red>");
        } else {
            reply(sender, "<green>Hunter Compass ensured for " + player.getName() + ".</green>");
        }
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            reply(sender, "<red>Usage: /huntercompass remove <player|all></red>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            state.clearHunters();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                items.removeAll(player);
            }
            reply(sender, "<green>All hunters and Hunter Compasses removed.</green>");
            return true;
        }
        Player player = plugin.getServer().getPlayerExact(args[1]);
        if (player == null) {
            reply(sender, "<red>That player must be online to remove their compass.</red>");
            return true;
        }
        state.removeHunter(player.getUniqueId());
        items.removeAll(player);
        player.sendActionBar(Component.empty());
        reply(sender, "<green>Removed " + player.getName() + " from the hunt.</green>");
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadPlugin();
        reply(sender, "<green>Configuration reloaded.</green>");
        return true;
    }

    private void showHelp(CommandSender sender, String label) {
        reply(sender, "<yellow>/" + label + " start [target]</yellow> – start and enroll eligible online players");
        reply(sender, "<yellow>/" + label + " stop</yellow> – stop and remove compasses");
        reply(sender, "<yellow>/" + label + " status</yellow> – show the current event");
        reply(sender, "<yellow>/" + label + " give [player|all]</yellow> – enroll and issue a compass");
        reply(sender, "<yellow>/" + label + " remove [player|all]</yellow> – remove hunters");
        reply(sender, "<yellow>/" + label + " reload</yellow> – reload config.yml");
    }

    private void reply(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("messages.prefix", "<gold>[HunterCompass]</gold> ");
        sender.sendMessage(miniMessage.deserialize(prefix + message));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("huntercompass.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return matches(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return matches(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            List<String> choices = new ArrayList<>();
            choices.add("all");
            choices.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
            return matches(choices, args[1]);
        }
        return List.of();
    }

    private List<String> matches(List<String> candidates, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
