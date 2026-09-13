package dev.minetuff.economy;

import dev.minetuff.model.PlayerProfile;
import dev.minetuff.world.WorldDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public final class EconomyService {
    private final long backpackBase;
    private final long backpackPerTool;
    private final double prestigeBonus;

    public EconomyService(FileConfiguration config) {
        backpackBase = config.getLong("economy.backpack-base-capacity", 5000L);
        backpackPerTool = config.getLong("economy.backpack-per-tool-level", 750L);
        prestigeBonus = config.getDouble("progression.prestige-money-bonus-per-level", 0.12);
    }

    public long backpackCapacity(PlayerProfile profile) {
        return backpackBase + (backpackPerTool * profile.toolLevel()) + (profile.prestige() * 25000L);
    }

    public long oreUnits(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> 3L;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> 5L;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> 8L;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> 10L;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 13L;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> 18L;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> 40L;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 55L;
            case AMETHYST_BLOCK -> 22L;
            case GOLD_BLOCK, RAW_GOLD_BLOCK -> 28L;
            case DIAMOND_BLOCK -> 70L;
            case EMERALD_BLOCK -> 90L;
            default -> 1L;
        };
    }

    public MiningReward store(PlayerProfile profile, WorldDefinition world, Material material) {
        long units = oreUnits(material);
        long capacity = backpackCapacity(profile);
        long available = Math.max(0L, capacity - profile.storedBlocks());
        long stored = Math.min(units, available);
        long overflow = units - stored;
        profile.addStoredBlocks(stored);
        profile.addBlocksMined(1L);
        double autoSold = 0.0;
        if (overflow > 0L) {
            autoSold = saleValue(profile, world, overflow) * 0.85;
            profile.addBalance(autoSold);
        }
        return new MiningReward(stored, overflow, autoSold);
    }

    public double sellAll(PlayerProfile profile, WorldDefinition world) {
        long units = profile.takeAllStoredBlocks();
        double value = saleValue(profile, world, units);
        profile.addBalance(value);
        return value;
    }

    public double saleValue(PlayerProfile profile, WorldDefinition world, long units) {
        if (units <= 0L) return 0.0;
        double permanentMultiplier = 1.0 + (profile.prestige() * prestigeBonus);
        double toolBonus = 1.0 + Math.min(2.5, profile.toolLevel() * 0.008);
        return units * world.sellMultiplier() * permanentMultiplier * toolBonus;
    }

    public record MiningReward(long storedUnits, long overflowUnits, double autoSoldValue) {}
}
