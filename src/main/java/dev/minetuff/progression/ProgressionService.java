package dev.minetuff.progression;

import dev.minetuff.model.PlayerProfile;
import dev.minetuff.world.WorldDefinition;
import org.bukkit.configuration.file.FileConfiguration;

public final class ProgressionService {
    private final double baseToolCost;
    private final double toolGrowth;
    private final int maxToolLevel;
    private final double basePrestigeCost;
    private final double prestigeGrowth;
    private final int maxPrestige;

    public ProgressionService(FileConfiguration config) {
        baseToolCost = config.getDouble("progression.base-tool-upgrade-cost", 2500.0);
        toolGrowth = config.getDouble("progression.tool-upgrade-growth", 1.65);
        maxToolLevel = config.getInt("progression.max-tool-level", 250);
        basePrestigeCost = config.getDouble("progression.base-prestige-cost", 5_000_000.0);
        prestigeGrowth = config.getDouble("progression.prestige-growth", 2.2);
        maxPrestige = config.getInt("progression.max-prestige", 100);
    }

    public double toolUpgradeCost(PlayerProfile profile) {
        int exponent = Math.max(0, profile.toolLevel() - 1);
        return Math.min(9.0e15, baseToolCost * Math.pow(toolGrowth, exponent));
    }

    public UpgradeResult upgradeTool(PlayerProfile profile) {
        if (profile.toolLevel() >= maxToolLevel) return new UpgradeResult(false, "Tool is already maxed.", 0.0);
        double cost = toolUpgradeCost(profile);
        if (!profile.withdraw(cost)) return new UpgradeResult(false, "You need more money.", cost);
        profile.incrementToolLevel();
        return new UpgradeResult(true, "Tool upgraded to level " + profile.toolLevel() + ".", cost);
    }

    public boolean canTravel(PlayerProfile profile, WorldDefinition target) {
        return target.id() <= profile.worldId() && profile.prestige() >= target.requiredPrestige();
    }

    public UnlockResult unlockNextWorld(PlayerProfile profile, WorldDefinition target) {
        if (target.id() <= profile.worldId()) return new UnlockResult(true, "Already unlocked.", 0.0);
        if (target.id() != profile.worldId() + 1) {
            return new UnlockResult(false, "Unlock worlds in order. Your next world is " + (profile.worldId() + 1) + ".", 0.0);
        }
        if (profile.prestige() < target.requiredPrestige()) {
            return new UnlockResult(false, "Prestige " + target.requiredPrestige() + " is required.", target.entryCost());
        }
        if (!profile.withdraw(target.entryCost())) {
            return new UnlockResult(false, "You need more money to unlock this world.", target.entryCost());
        }
        profile.setWorldId(target.id());
        return new UnlockResult(true, "Unlocked " + target.displayName() + ".", target.entryCost());
    }

    public double prestigeCost(PlayerProfile profile) {
        return Math.min(9.0e15, basePrestigeCost * Math.pow(prestigeGrowth, profile.prestige()));
    }

    public int requiredWorldForPrestige(PlayerProfile profile) {
        return Math.min(100, (profile.prestige() + 1) * 10);
    }

    public PrestigeResult prestige(PlayerProfile profile) {
        if (profile.prestige() >= maxPrestige) return new PrestigeResult(false, "Maximum prestige reached.", 0.0, 100);
        int requiredWorld = requiredWorldForPrestige(profile);
        double cost = prestigeCost(profile);
        if (profile.worldId() < requiredWorld) {
            return new PrestigeResult(false, "Reach world " + requiredWorld + " first.", cost, requiredWorld);
        }
        if (!profile.withdraw(cost)) {
            return new PrestigeResult(false, "You need more money to prestige.", cost, requiredWorld);
        }
        profile.incrementPrestige();
        profile.setToolLevel(1);
        profile.setWorldId(1);
        return new PrestigeResult(true, "Prestige " + profile.prestige() + " unlocked. Permanent sell bonus increased.", cost, requiredWorld);
    }

    public int maxToolLevel() { return maxToolLevel; }
    public int maxPrestige() { return maxPrestige; }

    public record UpgradeResult(boolean success, String message, double cost) {}
    public record UnlockResult(boolean success, String message, double cost) {}
    public record PrestigeResult(boolean success, String message, double cost, int requiredWorld) {}
}
