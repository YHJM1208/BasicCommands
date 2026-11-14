package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /back 的返回点记录，持久化到 back.yml。
 * 两种模式由 config.yml 的 back.mode 决定：
 *   global    —— 所有世界共用一个返回点
 *   per-world —— 每个世界各自记录，只在对应世界生效
 * 两种模式的数据分开存储，切换模式不会丢历史点。
 */
public final class BackService implements Listener {

    public record Point(String world, double x, double y, double z, float yaw, float pitch) {

        public Location toLocation() {
            var bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld == null) {
                return null;
            }
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }

    private final BasicCommands plugin;
    private final File file;
    private final Map<UUID, Point> global = new HashMap<>();
    private final Map<UUID, Map<String, Point>> perWorld = new HashMap<>();

    private volatile boolean dirty;
    private volatile boolean saveScheduled;

    BackService(BasicCommands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "back.yml");
    }

    public boolean isPerWorldMode() {
        return "per-world".equalsIgnoreCase(plugin.getConfig().getString("back.mode", "global"));
    }

    // ---------------------------------------------------------------- 读写

    public void load() {
        global.clear();
        perWorld.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String rawId : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(rawId);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Point single = readPoint(players.getConfigurationSection(rawId + ".global"));
            if (single != null) {
                global.put(id, single);
            }
            Map<String, Point> worlds = new HashMap<>();
            // 用列表而不是 map，避免世界名里带 "." 时被 YAML 路径切断
            for (Map<?, ?> raw : players.getMapList(rawId + ".worlds")) {
                Point point = readPoint(raw);
                if (point != null) {
                    worlds.put(point.world(), point);
                }
            }
            if (!worlds.isEmpty()) {
                perWorld.put(id, worlds);
            }
        }
    }

    public synchronized void saveNow() {
        if (global.isEmpty() && perWorld.isEmpty()) {
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("无法删除空的 back.yml");
            }
            dirty = false;
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        global.forEach((id, point) -> writePoint(yaml, "players." + id + ".global", point));

        perWorld.forEach((id, worlds) -> {
            List<Map<String, Object>> list = new ArrayList<>();
            worlds.values().forEach(point -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("world", point.world());
                node.put("x", point.x());
                node.put("y", point.y());
                node.put("z", point.z());
                node.put("yaw", point.yaw());
                node.put("pitch", point.pitch());
                list.add(node);
            });
            yaml.set("players." + id + ".worlds", list);
        });

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录：" + parent);
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("保存 back.yml 失败：" + e.getMessage());
        }
    }

    private void writePoint(YamlConfiguration yaml, String base, Point point) {
        yaml.set(base + ".world", point.world());
        yaml.set(base + ".x", point.x());
        yaml.set(base + ".y", point.y());
        yaml.set(base + ".z", point.z());
        yaml.set(base + ".yaw", point.yaw());
        yaml.set(base + ".pitch", point.pitch());
    }

    private Point readPoint(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return new Point(section.getString("world", "world"),
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    private Point readPoint(Map<?, ?> raw) {
        Object world = raw.get("world");
        if (!(world instanceof String worldName)) {
            return null;
        }
        return new Point(worldName,
                asDouble(raw.get("x")), asDouble(raw.get("y")), asDouble(raw.get("z")),
                (float) asDouble(raw.get("yaw")), (float) asDouble(raw.get("pitch")));
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private void scheduleSave() {
        dirty = true;
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            saveScheduled = false;
            if (dirty) {
                saveNow();
            }
        }, 20L);
    }

    // ---------------------------------------------------------------- 记录

    public void record(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Point point = new Point(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        synchronized (this) {
            if (isPerWorldMode()) {
                perWorld.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                        .put(point.world(), point);
            } else {
                global.put(player.getUniqueId(), point);
            }
        }
        scheduleSave();
    }

    /** 取当前可用的返回点；per-world 模式下只返回玩家当前世界的记录。 */
    public synchronized Point get(Player player) {
        if (isPerWorldMode()) {
            Map<String, Point> points = perWorld.get(player.getUniqueId());
            return points == null ? null : points.get(player.getWorld().getName());
        }
        return global.get(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("back.on-death", true)) {
            return;
        }
        Player player = event.getEntity();
        record(player, player.getLocation());
        plugin.lang().send(player, "back.death-recorded");
    }
}
