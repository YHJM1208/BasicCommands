package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /spawn —— 传送到出生点。
 * config.yml 的 spawn.mode 决定目标：
 *   world = 玩家当前世界的出生点
 *   main  = 固定主世界（spawn.main-world）的出生点，跨世界也能回
 */
public final class SpawnCommand extends BaseCommand {

    public SpawnCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("spawn");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "spawn") || !cooldownReady(player, "spawn")) {
            return true;
        }

        World world = player.getWorld();
        if ("main".equalsIgnoreCase(plugin.getConfig().getString("spawn.mode", "world"))) {
            String mainWorldName = plugin.getConfig().getString("spawn.main-world", "world");
            World mainWorld = Bukkit.getWorld(mainWorldName);
            if (mainWorld == null) {
                lang.send(sender, "spawn.main-world-missing", "world", mainWorldName);
                return true;
            }
            world = mainWorld;
        }

        Location spawn = world.getSpawnLocation();
        lang.send(sender, "spawn.teleporting", "world", world.getName());
        plugin.teleports().request(player, spawn);
        startCooldown(player, "spawn");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
