package dev.minetuff.world;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class WorldCatalog {
    private static final String[] STAGES = {"Frontier", "Deepworks", "Citadel", "Ascendant", "Mythic"};

    private static final Theme[] THEMES = {
            new Theme("Sakura Gorge", Material.CHERRY_PLANKS, Material.PINK_CONCRETE, Material.AMETHYST_BLOCK),
            new Theme("Ember Dwarven Hold", Material.DEEPSLATE_BRICKS, Material.RED_NETHER_BRICKS, Material.GOLD_BLOCK),
            new Theme("Atlantis Enclave", Material.PRISMARINE_BRICKS, Material.SEA_LANTERN, Material.DIAMOND_BLOCK),
            new Theme("Brasswater Borough", Material.CUT_COPPER, Material.EXPOSED_CUT_COPPER, Material.COPPER_BLOCK),
            new Theme("Amethyst Chasm", Material.AMETHYST_BLOCK, Material.CALCITE, Material.PURPUR_BLOCK),
            new Theme("Frostspire Citadel", Material.PACKED_ICE, Material.BLUE_ICE, Material.QUARTZ_BLOCK),
            new Theme("Saffron Sun Palace", Material.SANDSTONE, Material.ORANGE_TERRACOTTA, Material.GOLD_BLOCK),
            new Theme("Verdant Lost City", Material.MOSSY_STONE_BRICKS, Material.MOSS_BLOCK, Material.EMERALD_BLOCK),
            new Theme("Corsair Crescent", Material.DARK_OAK_PLANKS, Material.BLACKSTONE, Material.GOLD_BLOCK),
            new Theme("Mycelium Hollow", Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.SHROOMLIGHT),
            new Theme("Nocturne Cathedral", Material.POLISHED_BLACKSTONE_BRICKS, Material.CRYING_OBSIDIAN, Material.LAPIS_BLOCK),
            new Theme("Selene Research Base", Material.IRON_BLOCK, Material.SMOOTH_QUARTZ, Material.LIGHT_BLUE_CONCRETE),
            new Theme("Sugarplum Carnival", Material.PINK_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE),
            new Theme("Red Mesa Frontier", Material.RED_SANDSTONE, Material.TERRACOTTA, Material.RAW_GOLD_BLOCK),
            new Theme("Aether Archipelago", Material.QUARTZ_BLOCK, Material.LIGHT_BLUE_STAINED_GLASS, Material.DIAMOND_BLOCK),
            new Theme("Hollowfen Village", Material.MUD_BRICKS, Material.MANGROVE_PLANKS, Material.SCULK),
            new Theme("Infinite Athenaeum", Material.BOOKSHELF, Material.CHISELED_BOOKSHELF, Material.LAPIS_BLOCK),
            new Theme("Sunreef Lagoon", Material.WARPED_PLANKS, Material.PRISMARINE, Material.SEA_LANTERN),
            new Theme("Ironwood Railworks", Material.BRICKS, Material.IRON_BLOCK, Material.COPPER_BLOCK),
            new Theme("Stormwing Dragon Roost", Material.END_STONE_BRICKS, Material.OBSIDIAN, Material.PURPUR_BLOCK)
    };

    private final List<WorldDefinition> worlds;

    public WorldCatalog() {
        List<WorldDefinition> built = new ArrayList<>(100);
        int id = 1;
        for (int stage = 0; stage < STAGES.length; stage++) {
            for (Theme theme : THEMES) {
                String slug = slugify(theme.name()) + "_" + (stage + 1);
                String display = STAGES[stage] + " " + theme.name();
                int requiredPrestige = Math.max(0, (id - 1) / 10);
                double entryCost = id == 1 ? 0.0 : Math.min(5.0e12, 1500.0 * Math.pow(1.32, id - 2));
                double multiplier = 1.0 + ((id - 1) * 0.11) + (stage * 0.25);
                built.add(new WorldDefinition(id, slug, display, theme.name(), stage + 1,
                        requiredPrestige, entryCost, multiplier, theme.primary(), theme.accent(), theme.gem(),
                        paletteFor(id, theme.gem())));
                id++;
            }
        }
        worlds = Collections.unmodifiableList(built);
    }

    public List<WorldDefinition> all() { return worlds; }

    public WorldDefinition byId(int id) {
        if (id < 1 || id > worlds.size()) return worlds.getFirst();
        return worlds.get(id - 1);
    }

    public Optional<WorldDefinition> byWorldName(String worldName) {
        return worlds.stream().filter(def -> def.worldName().equalsIgnoreCase(worldName)).findFirst();
    }

    public int size() { return worlds.size(); }

    private static List<Material> paletteFor(int id, Material gem) {
        List<Material> result = new ArrayList<>();
        result.add(Material.STONE);
        result.add(id > 5 ? Material.DEEPSLATE : Material.COBBLESTONE);
        result.add(Material.COAL_ORE);
        if (id >= 8) result.add(Material.COPPER_ORE);
        if (id >= 15) result.add(Material.IRON_ORE);
        if (id >= 25) result.add(Material.REDSTONE_ORE);
        if (id >= 35) result.add(Material.LAPIS_ORE);
        if (id >= 45) result.add(Material.GOLD_ORE);
        if (id >= 60) result.add(Material.DIAMOND_ORE);
        if (id >= 80) result.add(Material.EMERALD_ORE);
        if (!result.contains(gem)) result.add(gem);
        return List.copyOf(result);
    }

    private static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private record Theme(String name, Material primary, Material accent, Material gem) {}
}
