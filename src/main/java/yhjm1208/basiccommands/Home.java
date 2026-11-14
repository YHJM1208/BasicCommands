package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** 一个家的坐标快照。世界名以字符串保存，重启/删世界后不会崩。 */
public record Home(String name, String world, double x, double y, double z, float yaw, float pitch) {

    /** 世界不存在时返回 null，由调用方给出友好提示。 */
    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
