package dev.minetuff.world;

import dev.minetuff.mine.MineService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class WorldService {
    private final Plugin plugin;
    private final WorldCatalog catalog;
    private final int eagerCreate;

    public WorldService(Plugin plugin, WorldCatalog catalog, FileConfiguration config) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.eagerCreate = Math.max(0, Math.min(catalog.size(), config.getInt("worlds.eager-create-first", 3)));
    }

    public void prewarm() {
        if (eagerCreate <= 0) return;
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> prewarmStep(1));
    }

    private void prewarmStep(int id) {
        if (id > eagerCreate) return;
        getOrCreate(catalog.byId(id));
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> prewarmStep(id + 1), 5L);
    }

    public Optional<WorldDefinition> definition(World world) {
        return catalog.byWorldName(world.getName());
    }

    public World getOrCreate(WorldDefinition definition) {
        World loaded = Bukkit.getWorld(definition.worldName());
        if (loaded != null) return loaded;
        WorldCreator creator = new WorldCreator(definition.worldName());
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        World world = creator.createWorld();
        if (world == null) throw new IllegalStateException("Failed to create world " + definition.worldName());
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setPVP(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setTime(6000L);
        world.setAutoSave(true);
        return world;
    }

    public CompletableFuture<Boolean> travel(Player player, WorldDefinition definition, MineService mines) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                World world = getOrCreate(definition);
                int pod = mines.podFor(player.getUniqueId());
                mines.ensurePod(definition, world, pod, () -> {
                    player.getScheduler().execute(plugin, () -> {
                        player.sendMessage(Component.text("Entering " + definition.displayName() + " · pod " + (pod + 1) + "/64"));
                        player.teleportAsync(mines.spawn(world, pod)).whenComplete((success, error) -> {
                            if (error != null) result.completeExceptionally(error);
                            else result.complete(Boolean.TRUE.equals(success));
                        });
                    }, () -> result.complete(false), 1L);
                });
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }
}
