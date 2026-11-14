package yhjm1208.basiccommands.legacy;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * legacy 版语言加载器：文本用 & 色码，不用 Adventure。
 */
public final class LegacyLang {

    private final JavaPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String prefix = "";
    private String language = "zh_CN";

    public LegacyLang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String configured = plugin.getConfig().getString("language", "zh_CN");
        String resource = "lang/" + configured + ".yml";
        if (plugin.getResource(resource) == null) {
            plugin.getLogger().warning("未知语言 " + configured + "，回退到 zh_CN");
            configured = "zh_CN";
            resource = "lang/" + configured + ".yml";
        }
        this.language = configured;

        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取语言文件失败：" + e.getMessage());
        }

        this.prefix = color(messages.getString("prefix", ""));
    }

    public String language() {
        return language;
    }

    public String get(String key, Object... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return ChatColor.RED + "<missing:" + key + ">";
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return prefix + color(raw);
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
