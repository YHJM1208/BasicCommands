package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一的传送入口：倒计时 + 移动/受伤取消 + 异步传送。
 * 所有传送（/home、/tpa、/rtp）都走这里，保证行为一致。
 */
public final class TeleportService implements Listener {

    private final BasicCommands plugin;
    private final Lang lang;

    private final Map<UUID, Location> origins = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    TeleportService(BasicCommands plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
    }

    public void request(Player player, Location destination) {
        request(player, destination, true);
    }

    /**
     * @param recordBack 是否在传送前记录返回点。/back 自己调用时应传 false，避免循环。
     */
    public void request(Player player, Location destination, boolean recordBack) {
        if (recordBack && plugin.getConfig().getBoolean("back.on-teleport", true)) {
            plugin.back().record(player, player.getLocation());
        }
        int seconds = plugin.getConfig().getInt("teleport.countdown-seconds", 3);
        clear(player.getUniqueId());
        if (seconds <= 0) {
            doTeleport(player, destination);
            return;
        }

        origins.put(player.getUniqueId(), player.getLocation());
        lang.send(player, "teleport.countdown", "seconds", seconds);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = seconds;

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    clear(player.getUniqueId());
                    doTeleport(player, destination);
                } else {
                    lang.send(player, "teleport.countdown", "seconds", remaining);
                }
            }
        }, 20L, 20L);
        countdownTasks.put(player.getUniqueId(), task);
    }

    public boolean isPending(UUID player) {
        return origins.containsKey(player) || countdownTasks.containsKey(player);
    }

    public void cancel(Player player, String messageKey) {
        boolean pending = isPending(player.getUniqueId());
        clear(player.getUniqueId());
        if (pending && messageKey != null) {
            lang.send(player, messageKey);
        }
    }

    private void clear(UUID player) {
        BukkitTask task = countdownTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
        origins.remove(player);
    }

    private void doTeleport(Player player, Location destination) {
        player.teleportAsync(destination).thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!Boolean.TRUE.equals(success)) {
                lang.send(player, "teleport.failed");
            }
        })).exceptionally(throwable -> {
            plugin.getLogger().warning("传送到 " + destination.getWorld().getName()
                    + " 失败：" + throwable.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> lang.send(player, "teleport.failed"));
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location origin = origins.get(event.getPlayer().getUniqueId());
        if (origin == null || !plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        double threshold = plugin.getConfig().getDouble("teleport.move-threshold", 0.5);
        double dx = to.getX() - origin.getX();
        double dz = to.getZ() - origin.getZ();
        if (dx * dx + dz * dz > threshold * threshold) {
            cancel(event.getPlayer(), "teleport.cancelled-move");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (origins.containsKey(player.getUniqueId())
                && plugin.getConfig().getBoolean("teleport.cancel-on-damage", true)) {
            cancel(player, "teleport.cancelled-damage");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clear(event.getEntity().getUniqueId());
    }

    void shutdown() {
        countdownTasks.values().forEach(BukkitTask::cancel);
        countdownTasks.clear();
        origins.clear();
    }
}
