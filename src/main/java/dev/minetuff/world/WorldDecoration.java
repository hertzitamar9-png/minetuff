package dev.minetuff.world;

import org.bukkit.Material;
import org.bukkit.World;

public final class WorldDecoration {
    private WorldDecoration() {}

    public static void decorateOwnedChunk(WorldDefinition def, World world, int centerX, int centerZ,
                                          int platformY, int mineWidth, int mineLength,
                                          int chunkX, int chunkZ) {
        int stage = def.stage();
        int variant = Math.floorMod(def.id() - 1, 5);
        int radius = 23 + stage * 2 + Math.floorMod(def.id(), 4);
        int height = 7 + stage * 2 + Math.floorMod(def.id() * 3, 6);

        buildSignatureSpire(def, world, centerX, centerZ - radius, platformY, height, chunkX, chunkZ);
        buildSignatureSpire(def, world, centerX, centerZ + radius, platformY, height - 1, chunkX, chunkZ);

        switch (variant) {
            case 0 -> buildCrown(def, world, centerX, centerZ, platformY, radius, chunkX, chunkZ);
            case 1 -> buildGate(def, world, centerX, centerZ, platformY, radius, height, chunkX, chunkZ);
            case 2 -> buildCrystals(def, world, centerX, centerZ, platformY, radius, height, chunkX, chunkZ);
            case 3 -> buildRibs(def, world, centerX, centerZ, platformY, radius, height, chunkX, chunkZ);
            default -> buildBeaconRing(def, world, centerX, centerZ, platformY, radius, height, chunkX, chunkZ);
        }

        int marker = 2 + Math.floorMod(def.id(), 7);
        for (int i = 0; i < marker; i++) {
            int dx = -mineWidth / 2 - 8 - i * 2;
            int dz = -mineLength / 2 - 5 + i * 4;
            setOwned(world, centerX + dx, platformY + 1 + (i % 3), centerZ + dz,
                    i % 2 == 0 ? def.gem() : def.accent(), chunkX, chunkZ);
        }
    }

    private static void buildSignatureSpire(WorldDefinition def, World world, int x, int z, int y,
                                             int height, int chunkX, int chunkZ) {
        for (int dy = 0; dy <= height; dy++) {
            int width = dy < 2 ? 2 : 1;
            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > width + 1) continue;
                    Material material = dy == height ? def.gem()
                            : (dy % 4 == 0 ? def.accent() : def.primary());
                    setOwned(world, x + dx, y + dy, z + dz, material, chunkX, chunkZ);
                }
            }
        }
    }

    private static void buildCrown(WorldDefinition def, World world, int cx, int cz, int y, int radius,
                                   int chunkX, int chunkZ) {
        for (int step = 0; step < 24; step++) {
            double angle = Math.PI * 2.0 * step / 24.0;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int z = cz + (int) Math.round(Math.sin(angle) * radius);
            int h = 3 + Math.floorMod(step + def.id(), 5);
            for (int dy = 0; dy <= h; dy++) {
                setOwned(world, x, y + dy, z, dy == h ? def.gem() : def.accent(), chunkX, chunkZ);
            }
        }
    }

    private static void buildGate(WorldDefinition def, World world, int cx, int cz, int y, int radius, int height,
                                  int chunkX, int chunkZ) {
        int z = cz - radius;
        for (int dx = -7; dx <= 7; dx++) {
            int columnHeight = Math.max(2, height - Math.abs(dx));
            for (int dy = 0; dy <= columnHeight; dy++) {
                if (Math.abs(dx) < 4 && dy < columnHeight - 2) continue;
                setOwned(world, cx + dx, y + dy, z,
                        (dx + dy) % 3 == 0 ? def.gem() : def.primary(), chunkX, chunkZ);
            }
        }
    }

    private static void buildCrystals(WorldDefinition def, World world, int cx, int cz, int y, int radius, int height,
                                      int chunkX, int chunkZ) {
        int[][] points = {{radius, 0}, {-radius, 0}, {0, radius}, {0, -radius},
                {radius - 5, radius - 5}, {-radius + 5, radius - 5}};
        for (int i = 0; i < points.length; i++) {
            int h = Math.max(4, height - i);
            for (int dy = 0; dy <= h; dy++) {
                int shrink = dy > h - 3 ? 0 : 1;
                for (int dx = -shrink; dx <= shrink; dx++) {
                    for (int dz = -shrink; dz <= shrink; dz++) {
                        setOwned(world, cx + points[i][0] + dx, y + dy, cz + points[i][1] + dz,
                                dy > h - 3 ? def.gem() : def.accent(), chunkX, chunkZ);
                    }
                }
            }
        }
    }

    private static void buildRibs(WorldDefinition def, World world, int cx, int cz, int y, int radius, int height,
                                  int chunkX, int chunkZ) {
        for (int side : new int[]{-1, 1}) {
            for (int i = -radius; i <= radius; i += 4) {
                int arch = Math.max(1, height - Math.abs(i) / 3);
                int x = cx + side * (radius - 2);
                int z = cz + i;
                for (int dy = 0; dy <= arch; dy++) {
                    setOwned(world, x, y + dy, z, dy == arch ? def.gem() : def.primary(), chunkX, chunkZ);
                }
            }
        }
    }

    private static void buildBeaconRing(WorldDefinition def, World world, int cx, int cz, int y, int radius, int height,
                                        int chunkX, int chunkZ) {
        for (int step = 0; step < 12; step++) {
            double angle = Math.PI * 2.0 * step / 12.0 + def.id() * 0.07;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int z = cz + (int) Math.round(Math.sin(angle) * radius);
            int h = 2 + Math.floorMod(step + def.stage(), Math.max(3, height));
            for (int dy = 0; dy <= h; dy++) {
                Material material = dy == h ? def.gem() : (dy % 2 == 0 ? def.primary() : def.accent());
                setOwned(world, x, y + dy, z, material, chunkX, chunkZ);
            }
        }
    }

    private static void setOwned(World world, int x, int y, int z, Material material, int chunkX, int chunkZ) {
        if ((x >> 4) != chunkX || (z >> 4) != chunkZ) return;
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
