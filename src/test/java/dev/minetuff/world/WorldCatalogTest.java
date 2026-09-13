package dev.minetuff.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorldCatalogTest {
    @Test
    void catalogContainsExactlyOneHundredUniqueWorlds() {
        WorldCatalog catalog = new WorldCatalog();
        assertEquals(100, catalog.size());

        Set<String> names = new HashSet<>();
        Set<String> displayNames = new HashSet<>();
        for (WorldDefinition world : catalog.all()) {
            assertTrue(names.add(world.worldName()), "duplicate world name: " + world.worldName());
            assertTrue(displayNames.add(world.displayName()), "duplicate display name: " + world.displayName());
            assertFalse(world.minePalette().isEmpty());
            assertNotNull(world.primary());
            assertNotNull(world.accent());
            assertNotNull(world.gem());
            assertTrue(world.sellMultiplier() >= 1.0);
        }
    }

    @Test
    void progressionIsOrderedAndCoversFiveStages() {
        WorldCatalog catalog = new WorldCatalog();
        double lastEntryCost = -1.0;
        Set<Integer> stages = new HashSet<>();
        for (WorldDefinition world : catalog.all()) {
            assertEquals(world.id(), catalog.byId(world.id()).id());
            assertTrue(world.entryCost() >= lastEntryCost);
            assertTrue(world.requiredPrestige() >= 0);
            stages.add(world.stage());
            lastEntryCost = world.entryCost();
        }
        assertEquals(Set.of(1, 2, 3, 4, 5), stages);
        assertEquals(0.0, catalog.byId(1).entryCost());
        assertEquals(100, catalog.byId(100).id());
    }
}
