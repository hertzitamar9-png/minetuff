package dev.minetuff.listener;

import dev.minetuff.capacity.WorldCapacityGate;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class CapacityListener implements Listener {
    private final WorldService worlds;
    private final WorldCapacityGate gate;

    public CapacityListener(WorldService worlds, FileConfiguration config) {
        this.worlds = worlds;
        this.gate = new WorldCapacityGate(Math.max(1, config.getInt("server.max-players-per-world", 1000)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void reserveTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        World from = event.getFrom().getWorld();
        World to = event.getTo().getWorld();
        if (from == to || worlds.definition(to).isEmpty()) return;
        if (!gate.reserve(event.getPlayer().getUniqueId(), to.getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("That mining world is at its " + gate.maxPlayersPerWorld() + " player capacity."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void releaseCancelledReservation(PlayerTeleportEvent event) {
        if (event.isCancelled()) gate.releaseReservation(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();
        if (worlds.definition(world).isPresent()) gate.enter(event.getPlayer().getUniqueId(), world.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gate.releaseReservation(event.getPlayer().getUniqueId());
        World world = event.getPlayer().getWorld();
        if (worlds.definition(world).isPresent()) gate.leave(world.getName());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        Player player = event.getPlayer();
        World to = player.getWorld();
        if (worlds.definition(from).isPresent()) gate.leave(from.getName());
        if (worlds.definition(to).isPresent()) gate.enter(player.getUniqueId(), to.getName());
        else gate.releaseReservation(player.getUniqueId());
    }

    public int occupancy(String worldName) {
        return gate.occupancy(worldName);
    }
}
