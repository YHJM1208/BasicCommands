package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /basiccmd —— 无参数时显示插件版本；/basiccmd reload 热加载配置与语言文件。
 */
public final class AdminCommand extends BaseCommand {

    public AdminCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("basiccmd");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showVersion(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!checkPermission(sender, "reload")) {
                return true;
            }
            plugin.reloadPlugin();
            lang.send(sender, "general.reloaded");
            return true;
        }
        lang.send(sender, "general.usage-admin");
        return true;
    }

    @SuppressWarnings("deprecation") // getPluginMeta() 是 1.19.4+；用 getDescription() 兼容 1.19 整条线
    private void showVersion(CommandSender sender) {
        // 无参数时只输出版本号
        lang.send(sender, "general.version", "version", plugin.getDescription().getVersion());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("reload"));
        }
        return List.of();
    }
}
