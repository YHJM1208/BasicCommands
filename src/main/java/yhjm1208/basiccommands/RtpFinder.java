package yhjm1208.basiccommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机传送落点搜索。
 * 候选点先异步加载目标区块（getChunkAtAsync），加载完成后回主线程再读方块判断是否安全，
 * 主线程不再因为生成新区块而卡顿；每次 /rtp 只占一个异步搜索名额。
 */
public final class RtpFinder {

    /** 单次搜索最长等待（秒），超时按“找不到落点”处理，避免玩家一直卡在搜索中。 */
    private static final long SEARCH_TIMEOUT_SECONDS = 10L;

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

    private final BasicCommands plugin;

    RtpFinder(BasicCommands plugin) {
        this.plugin = plugin;
    }

    /**
     * 异步寻找安全落点：区块加载在异步线程完成，方块判断回主线程。
     *
     * @return 完成时给出安全落点；找不到或超时为 null
     */
    public CompletableFuture<Location> findAsync(World world, int minRange, int maxRange, int attempts) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        double radiusLimit = border.getSize() / 2.0 - 16;
        if (radiusLimit < 1) {
            future.complete(null);
            return future;
        }
        double min = Math.max(0, Math.min(minRange, radiusLimit));
        double max = Math.max(min, Math.min(maxRange, radiusLimit));

        // 超时兜底：区块一直加载不出来时不把玩家晾在“正在搜索”
        Bukkit.getScheduler().runTaskLater(plugin, () -> future.complete(null), SEARCH_TIMEOUT_SECONDS * 20L);
        attempt(world, border, center, min, max, Math.max(1, attempts), future);
        return future;
    }

    private void attempt(World world, WorldBorder border, Location center,
                         double min, double max, int remaining, CompletableFuture<Location> future) {
        if (future.isDone()) {
            return;
        }
        if (remaining <= 0) {
            future.complete(null);
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0, Math.PI * 2);
        double radius = min + random.nextDouble() * (max - min);
        int x = (int) Math.floor(center.getX() + Math.cos(angle) * radius);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * radius);

        if (!border.isInside(new Location(world, x + 0.5, 64, z + 0.5))) {
            attempt(world, border, center, min, max, remaining - 1, future);
            return;
        }

        CompletableFuture<org.bukkit.Chunk> load;
        try {
            load = world.getChunkAtAsync(x >> 4, z >> 4, true);
        } catch (RuntimeException e) {
            future.complete(null);
            return;
        }
        load.thenAccept(chunk -> {
            if (!plugin.isEnabled() || future.isDone()) {
                return;
            }
            // 区块加载完成后的方块读取统一回主线程执行
            try {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (future.isDone()) {
                        return;
                    }
                    Location found = validate(world, x, z);
                    if (found != null) {
                        future.complete(found);
                        return;
                    }
                    attempt(world, border, center, min, max, remaining - 1, future);
                });
            } catch (RuntimeException e) {
                future.complete(null);
            }
        }).exceptionally(error -> {
            plugin.getLogger().warning("随机传送加载区块失败：" + error.getMessage());
            future.complete(null);
            return null;
        });
    }

    private Location validate(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
            return null;
        }
        Block ground = world.getBlockAt(x, y, z);
        if (!isSafeGround(ground)) {
            return null;
        }
        if (!isClear(world.getBlockAt(x, y + 1, z)) || !isClear(world.getBlockAt(x, y + 2, z))) {
            return null;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5,
                ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
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
