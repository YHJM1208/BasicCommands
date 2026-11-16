package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全服地标的持久化。写盘策略与家一致：主线程只改内存，异步 + 1 秒防抖落盘，关服时同步刷一次。
 */
public final class WarpStore {

    private final BasicCommands plugin;
    private final File file;
    private final Map<String, Warp> data = new ConcurrentHashMap<>();

    private volatile boolean dirty;
    private volatile boolean saveScheduled;

    WarpStore(BasicCommands plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
    }

    public synchronized void load() {
        data.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection warps = yaml.getConfigurationSection("warps");
        if (warps == null) {
            return;
        }
        for (String name : warps.getKeys(false)) {
            ConfigurationSection node = warps.getConfigurationSection(name);
            if (node == null) {
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT);
            data.put(key, new Warp(key,
                    node.getString("world", "world"),
                    node.getDouble("x"),
                    node.getDouble("y"),
                    node.getDouble("z"),
                    (float) node.getDouble("yaw"),
                    (float) node.getDouble("pitch")));
        }
    }

    public synchronized void saveNow() {
        if (data.isEmpty()) {
            // 没有地标时不要留下一个空文件
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("无法删除空的 warps.yml");
            }
            dirty = false;
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Warp warp : all()) {
            String base = "warps." + warp.name();
            yaml.set(base + ".world", warp.world());
            yaml.set(base + ".x", warp.x());
            yaml.set(base + ".y", warp.y());
            yaml.set(base + ".z", warp.z());
            yaml.set(base + ".yaw", warp.yaw());
            yaml.set(base + ".pitch", warp.pitch());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录：" + parent);
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("保存 warps.yml 失败：" + e.getMessage());
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

    /** 按名字排序后的全部地标，分页显示用。 */
    public synchronized List<Warp> all() {
        List<Warp> list = new ArrayList<>(data.values());
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return list;
    }

    public synchronized Warp get(String name) {
        return data.get(name.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean exists(String name) {
        return data.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public synchronized void put(Warp warp) {
        data.put(warp.name().toLowerCase(Locale.ROOT), warp);
        scheduleSave();
    }

    public synchronized boolean remove(String name) {
        if (data.remove(name.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        scheduleSave();
        return true;
    }

    public synchronized int count() {
        return data.size();
    }
}
