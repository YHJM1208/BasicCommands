package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** /rtp：异步找落点，找完再传送。 */
public final class RtpCommand extends BaseCommand {

    /** 正在搜索中的玩家，防止连点 /rtp 同时开多路区块加载。 */
    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();

    public RtpCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("rtp");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "rtp") || !cooldownReady(player, "rtp")) {
            return true;
        }

        World world = player.getWorld();
        if (plugin.getConfig().getStringList("rtp.blacklist-worlds").contains(world.getName())) {
            lang.send(sender, "rtp.blacklisted");
            return true;
        }
        if (!searching.add(player.getUniqueId())) {
            lang.send(sender, "rtp.busy");
            return true;
        }

        lang.send(sender, "rtp.searching");
        int[] range = resolveRange(world);
        plugin.rtpFinder().findAsync(world, range[0], range[1],
                        plugin.getConfig().getInt("rtp.max-attempts", 16))
                .whenComplete((target, error) -> {
                    try {
                        plugin.getServer().getScheduler()
                                .runTask(plugin, () -> finish(player, world, target, error));
                    } catch (RuntimeException e) {
                        // 关服过程中调度器已关闭，静默收尾即可
                        searching.remove(player.getUniqueId());
                    }
                });
        return true;
    }

    private void finish(Player player, World world, Location target, Throwable error) {
        searching.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        if (player.getWorld() != world) {
            lang.send(player, "rtp.cancelled");
            return;
        }
        if (error != null || target == null) {
            lang.send(player, "rtp.failed");
            return;
        }
        lang.send(player, "rtp.success",
                "x", target.getBlockX(),
                "y", target.getBlockY(),
                "z", target.getBlockZ(),
                "world", world.getName());
        plugin.teleports().request(player, target);
        startCooldown(player, "rtp");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    /**
     * 距离范围：优先取 rtp.worlds.&lt;世界名&gt;，否则用 rtp.default，
     * 再退回旧版的 rtp.min-range / rtp.max-range（兼容旧配置）。
     */
    private int[] resolveRange(World world) {
        var config = plugin.getConfig();
        int min = config.getInt("rtp.default.min-range", config.getInt("rtp.min-range", 100));
        int max = config.getInt("rtp.default.max-range", config.getInt("rtp.max-range", 5000));

        var worlds = config.getConfigurationSection("rtp.worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                if (!key.equals(world.getName())) {
                    continue;
                }
                min = worlds.getInt(key + ".min-range", min);
                max = worlds.getInt(key + ".max-range", max);
                break;
            }
        }
        return new int[]{min, max};
    }
}
