package dev.minetuff.listener;

import dev.minetuff.crates.CrateService;
import dev.minetuff.data.ProfileRepository;
import dev.minetuff.economy.EconomyService;
import dev.minetuff.mine.MineService;
import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;

public final class MiningListener implements Listener {
    private final ProfileRepository profiles;
    private final EconomyService economy;
    private final CrateService crates;
    private final MineService mines;
    private final WorldService worlds;

    public MiningListener(ProfileRepository profiles, EconomyService economy, CrateService crates,
                          MineService mines, WorldService worlds) {
        this.profiles = profiles;
        this.economy = economy;
        this.crates = crates;
        this.mines = mines;
        this.worlds = worlds;
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
    }
}
