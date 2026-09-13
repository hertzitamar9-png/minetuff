package dev.minetuff.progression;

import dev.minetuff.model.PlayerProfile;
import dev.minetuff.world.WorldCatalog;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionServiceTest {
    private static ProgressionService service() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("progression.base-tool-upgrade-cost", 100.0);
        config.set("progression.tool-upgrade-growth", 2.0);
        config.set("progression.max-tool-level", 250);
        config.set("progression.base-prestige-cost", 1000.0);
        config.set("progression.prestige-growth", 2.0);
        config.set("progression.max-prestige", 100);
        return new ProgressionService(config);
    }

    @Test
    void upgradesConsumeMoneyAndRaiseToolLevel() {
        PlayerProfile profile = PlayerProfile.fresh(UUID.randomUUID());
        profile.addBalance(500.0);
        ProgressionService.UpgradeResult result = service().upgradeTool(profile);
        assertTrue(result.success());
        assertEquals(2, profile.toolLevel());
        assertEquals(400.0, profile.balance(), 0.0001);
    }

    @Test
    void worldsUnlockOnlyInOrder() {
        PlayerProfile profile = PlayerProfile.fresh(UUID.randomUUID());
        profile.addBalance(1_000_000.0);
        ProgressionService service = service();
        WorldCatalog catalog = new WorldCatalog();
        assertFalse(service.unlockNextWorld(profile, catalog.byId(3)).success());
        assertTrue(service.unlockNextWorld(profile, catalog.byId(2)).success());
        assertEquals(2, profile.worldId());
    }

    @Test
    void prestigeResetsRunAndKeepsPermanentPrestige() {
        PlayerProfile profile = PlayerProfile.fresh(UUID.randomUUID());
        profile.setWorldId(10);
        profile.setToolLevel(40);
        profile.addBalance(10_000.0);
        ProgressionService.PrestigeResult result = service().prestige(profile);
        assertTrue(result.success());
        assertEquals(1, profile.prestige());
        assertEquals(1, profile.worldId());
        assertEquals(1, profile.toolLevel());
    }
}
