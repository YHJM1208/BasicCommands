package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BackService;
import yhjm1208.basiccommands.BasicCommands;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /back —— 返回上一个位置（死亡点或传送前的点） */
public final class BackCommand extends BaseCommand {

    public BackCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("back");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "back") || !cooldownReady(player, "back")) {
            return true;
        }
        BackService.Point point = plugin.back().get(player);
        if (point == null) {
            lang.send(sender, "back.none");
            return true;
        }
        Location destination = point.toLocation();
        if (destination == null) {
            lang.send(sender, "back.world-gone");
            return true;
        }
        lang.send(sender, "back.teleporting", "world", point.world());
        // 不覆盖返回点，避免 /back 自我循环
        plugin.teleports().request(player, destination, false);
        startCooldown(player, "back");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
