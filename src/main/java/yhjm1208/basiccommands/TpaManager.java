package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 传送请求管理器：内存态，带超时清理，不落盘。 */
public final class TpaManager {

    public enum Kind { TPA, TPH }

    public record Request(UUID from, UUID to, Kind kind, long createdAt) { }

    private final BasicCommands plugin;
    /** to -> (from -> request) */
    private final Map<UUID, Map<UUID, Request>> pending = new HashMap<>();
    private BukkitTask cleanupTask;

    TpaManager(BasicCommands plugin) {
        this.plugin = plugin;
    }

    void start() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 100L, 100L);
    }

    void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        pending.clear();
    }

    public void create(Player from, Player to, Kind kind) {
        pending.computeIfAbsent(to.getUniqueId(), k -> new LinkedHashMap<>())
                .put(from.getUniqueId(),
                        new Request(from.getUniqueId(), to.getUniqueId(), kind, System.currentTimeMillis()));
    }

    public Request peek(UUID to, UUID from) {
        Map<UUID, Request> incoming = pending.get(to);
        return incoming == null ? null : incoming.get(from);
    }

    /** 取该玩家最近一条未处理请求。 */
    public Request peekLatest(UUID to) {
        Map<UUID, Request> incoming = pending.get(to);
        if (incoming == null || incoming.isEmpty()) {
            return null;
        }
        Request latest = null;
        for (Request request : incoming.values()) {
            if (latest == null || request.createdAt() > latest.createdAt()) {
                latest = request;
            }
        }
        return latest;
    }

    public void remove(UUID to, UUID from) {
        Map<UUID, Request> incoming = pending.get(to);
        if (incoming == null) {
            return;
        }
        incoming.remove(from);
        if (incoming.isEmpty()) {
            pending.remove(to);
        }
    }

    public boolean expired(Request request, int timeoutSeconds) {
        return System.currentTimeMillis() - request.createdAt() > timeoutSeconds * 1000L;
    }

    private void cleanup() {
        int timeoutSeconds = plugin.getConfig().getInt("tpa.timeout-seconds", 60);
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> {
            entry.getValue().values().removeIf(request ->
                    now - request.createdAt() > timeoutSeconds * 1000L);
            return entry.getValue().isEmpty();
        });
    }
}
