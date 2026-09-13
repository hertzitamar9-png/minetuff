package dev.minetuff.listener;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.mine.MineService;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.progression.ProgressionService;
import dev.minetuff.world.WorldCatalog;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalListener implements Listener {
    private final ProfileRepository profiles;
    private final ProgressionService progression;
    private final WorldCatalog catalog;
    private final WorldService worlds;
    private final MineService mines;
    private final ConcurrentHashMap<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    public PortalListener(ProfileRepository profiles, ProgressionService progression, WorldCatalog catalog,
                          WorldService worlds, MineService mines) {
        this.profiles = profiles;
        this.progression = progression;
        this.catalog = catalog;
        this.worlds = worlds;
        this.mines = mines;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Optional<WorldDefinition> current = worlds.definition(event.getPlayer().getWorld());
        if (current.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (cooldownUntil.getOrDefault(event.getPlayer().getUniqueId(), 0L) > now) return;

        Player player = event.getPlayer();
        int pod = mines.podFor(player.getUniqueId());
        Location previous = mines.previousPortal(player.getWorld(), pod);
        Location next = mines.nextPortal(player.getWorld(), pod);
        int delta = near(event.getTo(), previous) ? -1 : (near(event.getTo(), next) ? 1 : 0);
        if (delta == 0) return;
        cooldownUntil.put(player.getUniqueId(), now + 2500L);

        int targetId = current.get().id() + delta;
        if (targetId < 1 || targetId > catalog.size()) {
            player.sendActionBar(Component.text(delta < 0 ? "This is the first world." : "You reached the final world."));
            return;
        }
        WorldDefinition target = catalog.byId(targetId);
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (!progression.canTravel(profile, target)) {
            ProgressionService.UnlockResult unlock = progression.unlockNextWorld(profile, target);
            if (!unlock.success()) {
                player.sendMessage(Component.text(unlock.message() + " Cost: $" + String.format("%,.0f", unlock.cost())));
                return;
            }
            player.sendMessage(Component.text(unlock.message()));
        }
        worlds.travel(player, target, mines).exceptionally(error -> {
            player.sendMessage(Component.text("Portal travel failed: " + error.getMessage()));
            return false;
        });
    }

    private static boolean near(Location here, Location portal) {
        return here.getWorld() == portal.getWorld()
                && Math.abs(here.getY() - portal.getY()) <= 2.0
                && here.distanceSquared(portal) <= 8.0;
    }
}
