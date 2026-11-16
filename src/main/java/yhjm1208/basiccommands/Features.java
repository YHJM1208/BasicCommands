package yhjm1208.basiccommands;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * 功能总开关。
 * 分组写在 config.yml 的 {@code features.<组>.enabled}，缺省为 true；
 * 关掉后命令仍然注册（不会变成未知命令），只是执行时直接回一句「已禁用」。
 */
public final class Features {

    public static final String HOMES = "homes";
    public static final String WARPS = "warps";
    public static final String TPA = "tpa";
    public static final String SPAWN = "spawn";
    public static final String BACK = "back";
    public static final String RTP = "rtp";

    private Features() {
    }

    /** 命令名 → 功能组；返回 null 表示不受总开关控制（如 /basiccmd）。 */
    public static String groupOf(String commandName) {
        switch (commandName.toLowerCase(Locale.ROOT)) {
            case "sethome":
            case "delhome":
            case "home":
            case "homes":
                return HOMES;
            case "warp":
            case "warps":
            case "setwarp":
            case "delwarp":
                return WARPS;
            case "tpa":
            case "tph":
            case "tpaccept":
            case "tpdeny":
                return TPA;
            case "spawn":
                return SPAWN;
            case "back":
                return BACK;
            case "rtp":
                return RTP;
            default:
                return null;
        }
    }

    /** 该功能组是否开启；配置里没写就当开启。 */
    public static boolean enabled(FileConfiguration config, String group) {
        return config.getBoolean("features." + group + ".enabled", true);
    }

    /** 这条命令当前是否允许执行。 */
    public static boolean allows(FileConfiguration config, String commandName) {
        String group = groupOf(commandName);
        return group == null || enabled(config, group);
    }
}
