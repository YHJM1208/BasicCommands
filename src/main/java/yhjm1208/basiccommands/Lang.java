package yhjm1208.basiccommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 语言文件加载器。
 * 文本全部来自 lang/<语言>.yml，用 MiniMessage 渲染，颜色/加粗/点击事件都在文件里改。
 */
public final class Lang {

    private final BasicCommands plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    private YamlConfiguration messages = new YamlConfiguration();
    private Component prefix = Component.empty();
    private String language = "zh_CN";

    Lang(BasicCommands plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String configured = plugin.getConfig().getString("language", "zh_CN");
        String resource = "lang/" + configured + ".yml";
        if (plugin.getResource(resource) == null) {
            plugin.getLogger().warning("未知的语言 " + configured + "，回退到 zh_CN");
            configured = "zh_CN";
            resource = "lang/" + configured + ".yml";
        }
        this.language = configured;

        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        // 只在内存里补缺失的键，绝不覆盖玩家已改过的文件内容
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

        String rawPrefix = messages.getString("prefix", "");
        this.prefix = rawPrefix.isEmpty() ? Component.empty() : mini.deserialize(rawPrefix);
    }

    public String language() {
        return language;
    }

    public Component get(String key, Object... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return Component.text("<missing:" + key + ">");
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String token = "{" + placeholders[i] + "}";
            // 占位符内容来自玩家/世界名，转义后再拼接，避免被当成 MiniMessage 标签
            raw = raw.replace(token, mini.escapeTags(String.valueOf(placeholders[i + 1])));
        }
        return Component.empty().append(prefix).append(mini.deserialize(raw));
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }
}
