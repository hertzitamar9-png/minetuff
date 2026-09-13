package dev.minetuff.mine;

import dev.minetuff.world.WorldDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class MineService {
    public static final int GRID_SIZE = 8;
    private final Plugin plugin;
    private final int podSpacing;
    private final int width;
    private final int depth;
    private final int length;
    private final double resetPercent;
    private final int minY = 64;
    private final Set<String> readyPods = ConcurrentHashMap.newKeySet();
    private final Set<String> rebuildingPods = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, AtomicLong> minedBlocks = new ConcurrentHashMap<>();

    public MineService(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        podSpacing = config.getInt("server.pod-spacing", 256);
        width = config.getInt("server.mine-width", 64);
        depth = config.getInt("server.mine-depth", 32);
        length = config.getInt("server.mine-length", 64);
        resetPercent = config.getDouble("server.reset-percent", 0.65);
    }

    public int podFor(java.util.UUID uuid) {
        return Math.floorMod(uuid.hashCode(), GRID_SIZE * GRID_SIZE);
    }

    public int podAt(Location location) {
        int col = (int) Math.round((double) location.getBlockX() / podSpacing) + 3;
        int row = (int) Math.round((double) location.getBlockZ() / podSpacing) + 3;
        if (col < 0 || col >= GRID_SIZE || row < 0 || row >= GRID_SIZE) return -1;
        int pod = row * GRID_SIZE + col;
        Bounds b = bounds(pod);
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= b.minX && x <= b.maxX && z >= b.minZ && z <= b.maxZ && y >= minY && y <= maxY() ? pod : -1;
    }

    public Location spawn(World world, int pod) {
        Point c = center(pod);
        return new Location(world, c.x + 0.5, maxY() + 7.0, c.z + 0.5, 0.0f, 20.0f);
    }

    public Location previousPortal(World world, int pod) {
        Point c = center(pod);
        return new Location(world, c.x - 14.5, maxY() + 6.0, c.z + 0.5);
    }

    public Location nextPortal(World world, int pod) {
        Point c = center(pod);
        return new Location(world, c.x + 14.5, maxY() + 6.0, c.z + 0.5);
    }

    public void ensurePod(WorldDefinition definition, World world, int pod, Runnable whenReady) {
        String key = key(world, pod);
        if (readyPods.contains(key)) {
            whenReady.run();
            return;
        }
        if (!rebuildingPods.add(key)) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> ensurePod(definition, world, pod, whenReady), 10L);
            return;
        }
        scheduleBuild(definition, world, pod, () -> {
            readyPods.add(key);
            rebuildingPods.remove(key);
            minedBlocks.computeIfAbsent(key, ignored -> new AtomicLong()).set(0L);
            whenReady.run();
        });
    }

    public void recordBreak(WorldDefinition definition, Location location) {
        int pod = podAt(location);
        if (pod < 0) return;
        String key = key(location.getWorld(), pod);
        long broken = minedBlocks.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
        long threshold = Math.max(1L, Math.round((double) volume() * resetPercent));
        if (broken >= threshold && rebuildingPods.add(key)) {
            scheduleBuild(definition, location.getWorld(), pod, () -> {
                minedBlocks.get(key).set(0L);
                rebuildingPods.remove(key);
            });
        }
    }

    public boolean isRebuilding(World world, int pod) {
        return rebuildingPods.contains(key(world, pod));
    }

    private void scheduleBuild(WorldDefinition definition, World world, int pod, Runnable completion) {
        Bounds b = bounds(pod);
        int minChunkX = b.minX >> 4;
        int maxChunkX = b.maxX >> 4;
        int minChunkZ = b.minZ >> 4;
        int maxChunkZ = b.maxZ >> 4;
        int taskCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        AtomicInteger remaining = new AtomicInteger(taskCount);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int cx = chunkX;
                int cz = chunkZ;
                Bukkit.getRegionScheduler().execute(plugin, world, cx, cz, () -> {
                    buildChunkSlice(definition, world, pod, cx, cz);
                    if (remaining.decrementAndGet() == 0) completion.run();
                });
            }
        }
    }

    private void buildChunkSlice(WorldDefinition definition, World world, int pod, int chunkX, int chunkZ) {
        Bounds b = bounds(pod);
        int startX = Math.max(b.minX, chunkX << 4);
        int endX = Math.min(b.maxX, (chunkX << 4) + 15);
        int startZ = Math.max(b.minZ, chunkZ << 4);
        int endZ = Math.min(b.maxZ, (chunkZ << 4) + 15);
        List<Material> palette = definition.minePalette();
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = minY; y <= maxY(); y++) {
                    world.getBlockAt(x, y, z).setType(pick(palette, definition.id(), x, y, z), false);
                }
                world.getBlockAt(x, minY - 1, z).setType(definition.primary(), false);
                world.getBlockAt(x, maxY() + 1, z).setType(definition.accent(), false);
            }
        }
        buildDecorForOwnedBlocks(definition, world, pod, chunkX, chunkZ);
    }

    private void buildDecorForOwnedBlocks(WorldDefinition definition, World world, int pod, int chunkX, int chunkZ) {
        Point c = center(pod);
        int platformY = maxY() + 5;
        for (int x = c.x - 20; x <= c.x + 20; x++) {
            for (int z = c.z - 5; z <= c.z + 5; z++) {
                if ((x >> 4) != chunkX || (z >> 4) != chunkZ) continue;
                Material material = ((x + z) & 1) == 0 ? definition.primary() : definition.accent();
                world.getBlockAt(x, platformY, z).setType(material, false);
                for (int y = platformY + 1; y <= platformY + 3; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        buildPortalPadIfOwned(world, c.x - 14, platformY + 1, c.z, definition.accent(), chunkX, chunkZ);
        buildPortalPadIfOwned(world, c.x + 14, platformY + 1, c.z, definition.gem(), chunkX, chunkZ);
        int[] cornersX = {c.x - (width / 2) - 3, c.x + (width / 2) + 3};
        int[] cornersZ = {c.z - (length / 2) - 3, c.z + (length / 2) + 3};
        for (int x : cornersX) {
            for (int z : cornersZ) {
                if ((x >> 4) != chunkX || (z >> 4) != chunkZ) continue;
                for (int y = minY; y <= platformY + 12; y++) {
                    world.getBlockAt(x, y, z).setType(y % 4 == 0 ? definition.gem() : definition.primary(), false);
                }
            }
        }
    }

    private void buildPortalPadIfOwned(World world, int x, int y, int z, Material material, int chunkX, int chunkZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int bx = x + dx;
                int bz = z + dz;
                if ((bx >> 4) != chunkX || (bz >> 4) != chunkZ) continue;
                world.getBlockAt(bx, y - 1, bz).setType(material, false);
                world.getBlockAt(bx, y, bz).setType(Material.AIR, false);
            }
        }
    }

    private Material pick(List<Material> palette, int worldId, int x, int y, int z) {
        long hash = 1469598103934665603L;
        hash = (hash ^ worldId) * 1099511628211L;
        hash = (hash ^ x) * 1099511628211L;
        hash = (hash ^ y) * 1099511628211L;
        hash = (hash ^ z) * 1099511628211L;
        int roll = Math.floorMod((int) (hash ^ (hash >>> 32)), 100);
        if (roll < 52) return palette.getFirst();
        if (roll < 76) return palette.get(Math.min(1, palette.size() - 1));
        int index = 2 + Math.floorMod(roll + worldId, Math.max(1, palette.size() - 2));
        return palette.get(Math.min(index, palette.size() - 1));
    }

    private long volume() { return (long) width * depth * length; }
    private int maxY() { return minY + depth - 1; }

    private Point center(int pod) {
        int row = pod / GRID_SIZE;
        int col = pod % GRID_SIZE;
        return new Point((col - 3) * podSpacing, (row - 3) * podSpacing);
    }

    private Bounds bounds(int pod) {
        Point c = center(pod);
        int halfW = width / 2;
        int halfL = length / 2;
        return new Bounds(c.x - halfW, c.x + halfW - 1, c.z - halfL, c.z + halfL - 1);
    }

    private static String key(World world, int pod) { return world.getName() + "#" + pod; }
    private record Point(int x, int z) {}
    private record Bounds(int minX, int maxX, int minZ, int maxZ) {}
}
