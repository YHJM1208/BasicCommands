package yhjm1208.basiccommands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** 随机传送落点搜索：在边界内取随机点，再校验脚下与头顶。 */
public final class RtpFinder {

    private static final Set<Material> UNSAFE_GROUND = EnumSet.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.POWDER_SNOW,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.SWEET_BERRY_BUSH,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.WITHER_ROSE,
            Material.POINTED_DRIPSTONE,
            Material.NETHER_PORTAL,
            Material.END_PORTAL);

    /**
     * 注意：会同步加载目标区块（getHighestBlockYAt），因此调用方应保证调用频率受冷却限制。
     *
     * @return 安全落点，找不到时返回 null
     */
    public Location find(World world, int minRange, int maxRange, int attempts) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        double radiusLimit = border.getSize() / 2.0 - 16;
        if (radiusLimit < 1) {
            return null;
        }
        double min = Math.max(0, Math.min(minRange, radiusLimit));
        double max = Math.max(min, Math.min(maxRange, radiusLimit));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double radius = min + random.nextDouble() * (max - min);
            int x = (int) Math.floor(center.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(center.getZ() + Math.sin(angle) * radius);

            if (!border.isInside(new Location(world, x + 0.5, 64, z + 0.5))) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
                continue;
            }
            Block ground = world.getBlockAt(x, y, z);
            if (!isSafeGround(ground)) {
                continue;
            }
            if (!isClear(world.getBlockAt(x, y + 1, z)) || !isClear(world.getBlockAt(x, y + 2, z))) {
                continue;
            }
            return new Location(world, x + 0.5, y + 1, z + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
        }
        return null;
    }

    private boolean isSafeGround(Block block) {
        Material type = block.getType();
        if (!type.isSolid() || block.isLiquid()) {
            return false;
        }
        return !UNSAFE_GROUND.contains(type);
    }

    private boolean isClear(Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }
}
