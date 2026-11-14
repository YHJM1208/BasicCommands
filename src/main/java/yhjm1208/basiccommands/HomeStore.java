package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 家的持久化。主线程只读写内存模型，落盘异步 + 1 秒防抖，关服时同步刷一次。
 */
public final class HomeStore {

    private final BasicCommands plugin;
    private final File file;
    private final Map<UUID, Map<String, Home>> data = new ConcurrentHashMap<>();

    private volatile boolean dirty;
    private volatile boolean saveScheduled;

    HomeStore(BasicCommands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    public synchronized void load() {
        data.clear();
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
            ConfigurationSection homes = players.getConfigurationSection(rawId + ".homes");
            if (homes == null) {
                continue;
            }
            Map<String, Home> map = new LinkedHashMap<>();
            for (String name : homes.getKeys(false)) {
                ConfigurationSection node = homes.getConfigurationSection(name);
                if (node == null) {
                    continue;
                }
                map.put(name, new Home(
                        name,
                        node.getString("world", "world"),
                        node.getDouble("x"),
                        node.getDouble("y"),
                        node.getDouble("z"),
                        (float) node.getDouble("yaw"),
                        (float) node.getDouble("pitch")));
            }
            if (!map.isEmpty()) {
                data.put(id, map);
            }
        }
    }

    public synchronized void saveNow() {
        boolean hasData = false;
        for (Map<String, Home> homes : data.values()) {
            if (!homes.isEmpty()) {
                hasData = true;
                break;
            }
        }
        if (!hasData) {
            // 没有数据时不要留下一个空文件
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("无法删除空的 homes.yml");
            }
            dirty = false;
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        data.forEach((id, homes) -> {
            for (Home home : homes.values()) {
                String base = "players." + id + ".homes." + home.name();
                yaml.set(base + ".world", home.world());
                yaml.set(base + ".x", home.x());
                yaml.set(base + ".y", home.y());
                yaml.set(base + ".z", home.z());
                yaml.set(base + ".yaw", home.yaw());
                yaml.set(base + ".pitch", home.pitch());
            }
        });
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录：" + parent);
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("保存 homes.yml 失败：" + e.getMessage());
        }
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

    public synchronized Map<String, Home> homes(UUID id) {
        Map<String, Home> homes = data.get(id);
        return homes == null ? Collections.emptyMap() : homes;
    }

    public synchronized Home get(UUID id, String name) {
        Map<String, Home> homes = data.get(id);
        return homes == null ? null : homes.get(name);
    }

    public synchronized boolean put(UUID id, Home home) {
        boolean overwritten = data.computeIfAbsent(id, k -> new LinkedHashMap<>())
                .put(home.name(), home) != null;
        scheduleSave();
        return overwritten;
    }

    public synchronized boolean remove(UUID id, String name) {
        Map<String, Home> homes = data.get(id);
        if (homes == null || homes.remove(name) == null) {
            return false;
        }
        scheduleSave();
        return true;
    }

    public synchronized int count(UUID id) {
        Map<String, Home> homes = data.get(id);
        return homes == null ? 0 : homes.size();
    }
}
