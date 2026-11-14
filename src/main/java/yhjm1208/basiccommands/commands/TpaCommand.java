package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import yhjm1208.basiccommands.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /tpa /tph /tpaccept /tpdeny */
public final class TpaCommand extends BaseCommand {

    public TpaCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("tpa", "tph", "tpaccept", "tpdeny");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> sendRequest(sender, args, TpaManager.Kind.TPA);
            case "tph" -> sendRequest(sender, args, TpaManager.Kind.TPH);
            case "tpaccept" -> respond(sender, args, true);
            case "tpdeny" -> respond(sender, args, false);
            default -> false;
        };
    }

    private boolean sendRequest(CommandSender sender, String[] args, TpaManager.Kind kind) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String permission = kind == TpaManager.Kind.TPA ? "tpa" : "tph";
        if (!checkPermission(sender, permission)) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, kind == TpaManager.Kind.TPA ? "tpa.usage" : "tpa.usage-tph");
            return true;
        }
        if (!cooldownReady(player, permission)) {
            return true;
        }
        Player target = findOnline(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            lang.send(sender, "tpa.self");
            return true;
        }
        if (!crossWorldAllowed(player, target)) {
            lang.send(sender, "tpa.cross-world-denied", "player", target.getName());
            return true;
        }

        // 配置为“无需同意”时，tph 直接把对方拉过来
        if (kind == TpaManager.Kind.TPH
                && !plugin.getConfig().getBoolean("tpa.tph-requires-accept", true)) {
            lang.send(sender, "tpa.sent-tph", "player", target.getName());
            lang.send(target, "tpa.accepted-tph-pulled", "player", player.getName());
            plugin.teleports().request(target, player.getLocation());
            startCooldown(player, permission);
            return true;
        }

        plugin.tpa().create(player, target, kind);
        lang.send(sender, kind == TpaManager.Kind.TPA ? "tpa.sent" : "tpa.sent-tph",
                "player", target.getName());
        lang.send(target, kind == TpaManager.Kind.TPA ? "tpa.received" : "tpa.received-tph",
                "player", player.getName());
        startCooldown(player, permission);
        return true;
    }

    private boolean respond(CommandSender sender, String[] args, boolean accept) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "tpa")) {
            return true;
        }

        TpaManager.Request request;
        if (args.length > 0) {
            Player from = plugin.getServer().getPlayerExact(args[0]);
            if (from == null) {
                lang.send(sender, "tpa.none", "player", args[0]);
                return true;
            }
            request = plugin.tpa().peek(player.getUniqueId(), from.getUniqueId());
            if (request == null) {
                lang.send(sender, "tpa.none", "player", from.getName());
                return true;
            }
        } else {
            request = plugin.tpa().peekLatest(player.getUniqueId());
            if (request == null) {
                lang.send(sender, "tpa.no-pending");
                return true;
            }
        }

        plugin.tpa().remove(request.to(), request.from());
        Player from = plugin.getServer().getPlayer(request.from());
        if (from == null || !from.isOnline()) {
            lang.send(sender, "tpa.expired", "player", args.length > 0 ? args[0] : "?");
            return true;
        }
        if (plugin.tpa().expired(request, plugin.getConfig().getInt("tpa.timeout-seconds", 60))) {
            lang.send(sender, "tpa.expired", "player", from.getName());
            return true;
        }

        if (!accept) {
            lang.send(sender, "tpa.denied", "player", from.getName());
            lang.send(from, "tpa.denied-target", "player", player.getName());
            return true;
        }

        // 请求发出后对方可能换了世界，接受时再校验一次
        if (!crossWorldAllowed(player, from)) {
            lang.send(sender, "tpa.cross-world-denied", "player", from.getName());
            return true;
        }

        if (request.kind() == TpaManager.Kind.TPA) {
            lang.send(sender, "tpa.accepted", "player", from.getName());
            lang.send(from, "tpa.accepted-target", "player", player.getName());
            plugin.teleports().request(from, player.getLocation());
        } else {
            lang.send(sender, "tpa.accepted-tph", "player", from.getName());
            lang.send(from, "tpa.accepted-tph-target", "player", player.getName());
            plugin.teleports().request(player, from.getLocation());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return filter(args[0], onlinePlayerNames());
    }

    /** 跨世界请求是否允许，由 config.yml 的 tpa.cross-world 控制。 */
    private boolean crossWorldAllowed(Player first, Player second) {
        if (plugin.getConfig().getBoolean("tpa.cross-world", true)) {
            return true;
        }
        return first.getWorld().equals(second.getWorld());
    }
}
