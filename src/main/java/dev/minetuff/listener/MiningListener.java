package dev.minetuff.listener;

import dev.minetuff.crates.CrateService;
import dev.minetuff.data.ProfileRepository;
import dev.minetuff.economy.EconomyService;
import dev.minetuff.mine.MineService;
import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.tools.ToolService;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public final class MiningListener implements Listener {
    private final Plugin plugin;
    private final ProfileRepository profiles;
    private final EconomyService economy;
    private final CrateService crates;
    private final MineService mines;
    private final WorldService worlds;
    private final ToolService tools;

    public MiningListener(Plugin plugin, ProfileRepository profiles, EconomyService economy, CrateService crates,
                          MineService mines, WorldService worlds, ToolService tools) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.economy = economy;
        this.crates = crates;
        this.mines = mines;
        this.worlds = worlds;
        this.tools = tools;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Optional<WorldDefinition> definition = worlds.definition(event.getBlock().getWorld());
        if (definition.isEmpty()) return;
        int pod = mines.podAt(event.getBlock().getLocation());
        if (pod < 0 || mines.isRebuilding(event.getBlock().getWorld(), pod)) {
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        PlayerProfile profile = profiles.get(event.getPlayer().getUniqueId());
        EconomyService.MiningReward reward = economy.store(profile, definition.get(), event.getBlock().getType());
        mines.recordBreak(definition.get(), event.getBlock().getLocation());

        Optional<CrateTier> key = crates.rollKey(profile);
        if (key.isPresent()) {
            event.getPlayer().sendActionBar(Component.text("✦ Found a " + key.get().name() + " crate key!"));
        } else if (reward.autoSoldValue() > 0.0) {
            event.getPlayer().sendActionBar(Component.text("Backpack full · auto-sold overflow for $" +
                    String.format("%,.0f", reward.autoSoldValue())));
        }

        WorldDefinition def = definition.get();
        World world = event.getBlock().getWorld();
        Location origin = event.getBlock().getLocation();
        for (ToolService.Offset offset : tools.extraBlocks(event.getPlayer(), profile)) {
            int x = origin.getBlockX() + offset.dx();
            int y = origin.getBlockY() + offset.dy();
            int z = origin.getBlockZ() + offset.dz();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ,
                    () -> mineExtraBlock(def, profile, world, x, y, z));
        }
    }

    private void mineExtraBlock(WorldDefinition definition, PlayerProfile profile, World world, int x, int y, int z) {
        Location location = new Location(world, x, y, z);
        int pod = mines.podAt(location);
        if (pod < 0 || mines.isRebuilding(world, pod)) return;
        Block block = world.getBlockAt(x, y, z);
        if (block.getType().isAir()) return;
        var material = block.getType();
        block.setType(org.bukkit.Material.AIR, false);
        economy.store(profile, definition, material);
        mines.recordBreak(definition, location);
        crates.rollKey(profile);
    }
}
