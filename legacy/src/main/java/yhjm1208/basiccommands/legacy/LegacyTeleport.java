package yhjm1208.basiccommands.legacy;

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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * legacy 版传送：倒计时 + 移动/受伤取消 + 同步传送（1.7.10 没有 teleportAsync）。
 */
public final class LegacyTeleport implements Listener {

    private final JavaPlugin plugin;
    private final LegacyLang lang;
    private final LegacyStore store;

    private final Map<UUID, Location> origins = new HashMap<UUID, Location>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<UUID, BukkitTask>();

    public LegacyTeleport(JavaPlugin plugin, LegacyLang lang, LegacyStore store) {
        this.plugin = plugin;
        this.lang = lang;
        this.store = store;
    }

    public void request(Player player, Location destination) {
        request(player, destination, true);
    }

    public void request(Player player, Location destination, boolean recordBack) {
        if (recordBack && plugin.getConfig().getBoolean("back.on-teleport", true)) {
            store.recordBack(player, player.getLocation());
        }
        int seconds = plugin.getConfig().getInt("teleport.countdown-seconds", 3);
        clear(player.getUniqueId());
        if (seconds <= 0) {
            doTeleport(player, destination);
            return;
        }

        origins.put(player.getUniqueId(), player.getLocation());
        lang.send(player, "teleport.countdown", "seconds", Integer.valueOf(seconds));

        final Player target = player;
        final Location dest = destination;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = seconds;

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    clear(target.getUniqueId());
                    doTeleport(target, dest);
                } else {
                    lang.send(target, "teleport.countdown", "seconds", Integer.valueOf(remaining));
                }
            }
        }, 20L, 20L);
        tasks.put(player.getUniqueId(), task);
    }

    public void cancel(Player player, String messageKey) {
        boolean pending = origins.containsKey(player.getUniqueId()) || tasks.containsKey(player.getUniqueId());
        clear(player.getUniqueId());
        if (pending && messageKey != null) {
            lang.send(player, messageKey);
        }
    }

    private void clear(UUID id) {
        BukkitTask task = tasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        origins.remove(id);
    }

    private void doTeleport(Player player, Location destination) {
        if (!player.teleport(destination)) {
            lang.send(player, "teleport.failed");
        }
    }

    public void shutdown() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        origins.clear();
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
        double threshold = plugin.getConfig().getDouble("teleport.move-threshold", 0.5D);
        double dx = to.getX() - origin.getX();
        double dz = to.getZ() - origin.getZ();
        if (dx * dx + dz * dz > threshold * threshold) {
            cancel(event.getPlayer(), "teleport.cancelled-move");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
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
        if (!plugin.getConfig().getBoolean("back.on-death", true)) {
            return;
        }
        Player player = event.getEntity();
        store.recordBack(player, player.getLocation());
        lang.send(player, "back.death-recorded");
    }
}
