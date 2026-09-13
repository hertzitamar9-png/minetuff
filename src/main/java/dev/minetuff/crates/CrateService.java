package dev.minetuff.crates;

import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateService {
    private final double commonChance;
    private final double rareChance;
    private final double epicChance;
    private final int maxToolLevel;

    public CrateService(FileConfiguration config) {
        commonChance = config.getDouble("crates.common-key-chance", 0.004);
        rareChance = config.getDouble("crates.rare-key-chance", 0.0008);
        epicChance = config.getDouble("crates.epic-key-chance", 0.00015);
        maxToolLevel = config.getInt("progression.max-tool-level", 250);
    }

    public Optional<CrateTier> rollKey(PlayerProfile profile) {
        double roll = ThreadLocalRandom.current().nextDouble();
        CrateTier tier = null;
        if (roll < epicChance) tier = CrateTier.EPIC;
        else if (roll < epicChance + rareChance) tier = CrateTier.RARE;
        else if (roll < epicChance + rareChance + commonChance) tier = CrateTier.COMMON;
        if (tier != null) profile.addCrateKey(tier, 1);
        return Optional.ofNullable(tier);
    }

    public OpenResult open(PlayerProfile profile, CrateTier tier) {
        if (!profile.useCrateKey(tier)) return new OpenResult(false, "You do not have a " + tier.name().toLowerCase() + " key.");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int outcome = random.nextInt(100);
        if (tier == CrateTier.COMMON) {
            if (outcome < 75) {
                double money = random.nextDouble(15_000.0, 60_000.0);
                profile.addBalance(money);
                return new OpenResult(true, "Common crate: +$" + format(money));
            }
            long tokens = random.nextLong(20L, 80L);
            profile.addTokens(tokens);
            return new OpenResult(true, "Common crate: +" + tokens + " tokens");
        }
        if (tier == CrateTier.RARE) {
            if (outcome < 55) {
                double money = random.nextDouble(150_000.0, 650_000.0);
                profile.addBalance(money);
                return new OpenResult(true, "Rare crate: +$" + format(money));
            }
            if (outcome < 85) {
                long tokens = random.nextLong(150L, 500L);
                profile.addTokens(tokens);
                return new OpenResult(true, "Rare crate: +" + tokens + " tokens");
            }
            int levels = addToolLevels(profile, 1);
            return new OpenResult(true, "Rare crate: +" + levels + " tool level");
        }
        if (outcome < 45) {
            double money = random.nextDouble(2_000_000.0, 12_000_000.0);
            profile.addBalance(money);
            return new OpenResult(true, "Epic crate: +$" + format(money));
        }
        if (outcome < 75) {
            long tokens = random.nextLong(1_000L, 4_000L);
            profile.addTokens(tokens);
            return new OpenResult(true, "Epic crate: +" + tokens + " tokens");
        }
        int levels = addToolLevels(profile, 3);
        return new OpenResult(true, "Epic crate: +" + levels + " tool levels");
    }

    private int addToolLevels(PlayerProfile profile, int wanted) {
        int before = profile.toolLevel();
        profile.setToolLevel(Math.min(maxToolLevel, before + wanted));
        return profile.toolLevel() - before;
    }

    private static String format(double value) {
        return String.format("%,.0f", value);
    }

    public record OpenResult(boolean success, String message) {}
}
