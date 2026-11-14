package yhjm1208.basiccommands.legacy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * legacy 版数据层：家 + 返回点。
 * 文件格式与新版一致（homes.yml / back.yml），玩家数据可直接沿用。
 */
public final class LegacyStore {

    public static final class Home {
        final String name;
        final String world;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        Home(String name, String world, double x, double y, double z, float yaw, float pitch) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        String name() { return name; }
        String world() { return world; }

        Location toLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }

    public static final class Point {
        final String world;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        Point(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        Location toLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
    }

    private final JavaPlugin plugin;
    private final File homeFile;
    private final File backFile;

    private final Map<UUID, Map<String, Home>> homes = new HashMap<UUID, Map<String, Home>>();
    private final Map<UUID, Point> backGlobal = new HashMap<UUID, Point>();
    private final Map<UUID, Map<String, Point>> backWorlds = new HashMap<UUID, Map<String, Point>>();

    private volatile boolean dirty;
    private volatile boolean saveScheduled;

    public LegacyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.homeFile = new File(plugin.getDataFolder(), "homes.yml");
        this.backFile = new File(plugin.getDataFolder(), "back.yml");
    }

    // ------------------------------------------------------------ 家

    public synchronized Map<String, Home> homes(UUID id) {
        Map<String, Home> map = homes.get(id);
        return map == null ? new LinkedHashMap<String, Home>() : map;
    }

    public synchronized Home getHome(UUID id, String name) {
        Map<String, Home> map = homes.get(id);
        return map == null ? null : map.get(name);
    }

    public synchronized int homeCount(UUID id) {
        Map<String, Home> map = homes.get(id);
        return map == null ? 0 : map.size();
    }

    public synchronized boolean putHome(UUID id, Home home) {
        Map<String, Home> map = homes.get(id);
        if (map == null) {
            map = new LinkedHashMap<String, Home>();
            homes.put(id, map);
        }
        boolean overwritten = map.put(home.name, home) != null;
        markDirty();
        return overwritten;
    }

    public synchronized boolean removeHome(UUID id, String name) {
        Map<String, Home> map = homes.get(id);
        if (map == null || map.remove(name) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    // ------------------------------------------------------------ 返回点

    private boolean perWorldBack() {
        return "per-world".equalsIgnoreCase(plugin.getConfig().getString("back.mode", "global"));
    }

    public synchronized void recordBack(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Point point = new Point(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        if (perWorldBack()) {
            Map<String, Point> map = backWorlds.get(player.getUniqueId());
            if (map == null) {
                map = new HashMap<String, Point>();
                backWorlds.put(player.getUniqueId(), map);
            }
            map.put(point.world, point);
        } else {
            backGlobal.put(player.getUniqueId(), point);
        }
        markDirty();
    }

    public synchronized Point getBack(Player player) {
        if (perWorldBack()) {
            Map<String, Point> map = backWorlds.get(player.getUniqueId());
            return map == null ? null : map.get(player.getWorld().getName());
        }
        return backGlobal.get(player.getUniqueId());
    }

    // ------------------------------------------------------------ 读写

    public synchronized void load() {
        homes.clear();
        backGlobal.clear();
        backWorlds.clear();

        if (homeFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(homeFile);
            ConfigurationSection players = yaml.getConfigurationSection("players");
            if (players != null) {
                for (String rawId : players.getKeys(false)) {
                    UUID id = parseUuid(rawId);
                    if (id == null) {
                        continue;
                    }
                    ConfigurationSection section = players.getConfigurationSection(rawId + ".homes");
                    if (section == null) {
                        continue;
                    }
                    Map<String, Home> map = new LinkedHashMap<String, Home>();
                    for (String name : section.getKeys(false)) {
                        ConfigurationSection node = section.getConfigurationSection(name);
                        if (node == null) {
                            continue;
                        }
                        map.put(name, new Home(name,
                                node.getString("world", "world"),
                                node.getDouble("x"), node.getDouble("y"), node.getDouble("z"),
                                (float) node.getDouble("yaw"), (float) node.getDouble("pitch")));
                    }
                    if (!map.isEmpty()) {
                        homes.put(id, map);
                    }
                }
            }
        }

        if (backFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(backFile);
            ConfigurationSection players = yaml.getConfigurationSection("players");
            if (players != null) {
                for (String rawId : players.getKeys(false)) {
                    UUID id = parseUuid(rawId);
                    if (id == null) {
                        continue;
                    }
                    ConfigurationSection node = players.getConfigurationSection(rawId + ".global");
                    if (node != null) {
                        backGlobal.put(id, new Point(node.getString("world", "world"),
                                node.getDouble("x"), node.getDouble("y"), node.getDouble("z"),
                                (float) node.getDouble("yaw"), (float) node.getDouble("pitch")));
                    }
                    Map<String, Point> map = new HashMap<String, Point>();
                    for (Map<?, ?> raw : players.getMapList(rawId + ".worlds")) {
                        Object world = raw.get("world");
                        if (!(world instanceof String)) {
                            continue;
                        }
                        Point point = new Point((String) world,
                                asDouble(raw.get("x")), asDouble(raw.get("y")), asDouble(raw.get("z")),
                                (float) asDouble(raw.get("yaw")), (float) asDouble(raw.get("pitch")));
                        map.put(point.world, point);
                    }
                    if (!map.isEmpty()) {
                        backWorlds.put(id, map);
                    }
                }
            }
        }
    }

    public synchronized void saveNow() {
        saveHomes();
        saveBack();
        dirty = false;
    }

    private void saveHomes() {
        boolean hasData = false;
        for (Map<String, Home> map : homes.values()) {
            if (!map.isEmpty()) {
                hasData = true;
                break;
            }
        }
        if (!hasData) {
            if (homeFile.exists() && !homeFile.delete()) {
                plugin.getLogger().warning("无法删除空的 homes.yml");
            }
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Home>> entry : homes.entrySet()) {
            for (Home home : entry.getValue().values()) {
                String base = "players." + entry.getKey() + ".homes." + home.name();
                yaml.set(base + ".world", home.world);
                yaml.set(base + ".x", home.x);
                yaml.set(base + ".y", home.y);
                yaml.set(base + ".z", home.z);
                yaml.set(base + ".yaw", home.yaw);
                yaml.set(base + ".pitch", home.pitch);
            }
        }
        save(yaml, homeFile);
    }

    private void saveBack() {
        if (backGlobal.isEmpty() && backWorlds.isEmpty()) {
            if (backFile.exists() && !backFile.delete()) {
                plugin.getLogger().warning("无法删除空的 back.yml");
            }
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Point> entry : backGlobal.entrySet()) {
            writePoint(yaml, "players." + entry.getKey() + ".global", entry.getValue());
        }
        for (Map.Entry<UUID, Map<String, Point>> entry : backWorlds.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Point point : entry.getValue().values()) {
                Map<String, Object> node = new LinkedHashMap<String, Object>();
                node.put("world", point.world);
                node.put("x", point.x);
                node.put("y", point.y);
                node.put("z", point.z);
                node.put("yaw", point.yaw);
                node.put("pitch", point.pitch);
                list.add(node);
            }
            yaml.set("players." + entry.getKey() + ".worlds", list);
        }
        save(yaml, backFile);
    }

    private void writePoint(YamlConfiguration yaml, String base, Point point) {
        yaml.set(base + ".world", point.world);
        yaml.set(base + ".x", point.x);
        yaml.set(base + ".y", point.y);
        yaml.set(base + ".z", point.z);
        yaml.set(base + ".yaw", point.yaw);
        yaml.set(base + ".pitch", point.pitch);
    }

    private void save(YamlConfiguration yaml, File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录：" + parent);
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 " + file.getName() + " 失败：" + e.getMessage());
        }
    }

    private void markDirty() {
        dirty = true;
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                saveScheduled = false;
                if (dirty) {
                    saveNow();
                }
            }
        }, 20L);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }
}
