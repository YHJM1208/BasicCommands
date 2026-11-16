package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import yhjm1208.basiccommands.Features;
import yhjm1208.basiccommands.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** 命令基类：统一的权限、冷却、玩家校验与补全过滤。 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final BasicCommands plugin;
    protected final Lang lang;

    protected BaseCommand(BasicCommands plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
    }

    protected void register(String... names) {
        for (String name : names) {
            PluginCommand command = plugin.getCommand(name);
            if (command == null) {
                plugin.getLogger().warning("命令 /" + name + " 已被其他插件占用，跳过注册");
                continue;
            }
            command.setExecutor((sender, cmd, label, args) -> {
                // 功能总开关：命令保留，禁用时只回一句提示，不把命令变成未知命令
                if (!Features.allows(plugin.getConfig(), cmd.getName())) {
                    lang.send(sender, "general.feature-disabled");
                    return true;
                }
                return this.onCommand(sender, cmd, label, args);
            });
            command.setTabCompleter(this);
        }
    }

    protected Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        lang.send(sender, "general.player-only");
        return null;
    }

    protected boolean checkPermission(CommandSender sender, String node) {
        if (sender.hasPermission("basiccmd." + node)) {
            return true;
        }
        lang.send(sender, "general.no-permission");
        return false;
    }

    /** @return true 表示冷却已结束，可以执行 */
    protected boolean cooldownReady(Player player, String key) {
        if (player.hasPermission("basiccmd.cooldown.bypass")) {
            return true;
        }
        long remaining = plugin.cooldowns().remainingSeconds(player.getUniqueId(), key);
        if (remaining <= 0) {
            return true;
        }
        lang.send(player, "general.cooldown", "seconds", remaining);
        return false;
    }

    protected void startCooldown(Player player, String key) {
        if (player.hasPermission("basiccmd.cooldown.bypass")) {
            return;
        }
        plugin.cooldowns().set(player.getUniqueId(), key, plugin.getConfig().getInt("cooldowns." + key, 0));
    }

    protected Player findOnline(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (target == null) {
            lang.send(sender, "general.player-not-found", "player", name);
        }
        return target;
    }

    protected static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** 解析分页参数，非法时返回 -1，由调用方统一提示。 */
    protected static int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected static List<String> filter(String prefix, Collection<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    protected static List<String> onlinePlayerNames() {
        return org.bukkit.Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
