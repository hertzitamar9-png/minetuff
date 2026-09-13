package dev.minetuff.listener;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CapacityListener implements Listener {
    private final WorldService worlds;
    private final int maxPlayersPerWorld;
    private final Object lock = new Object();
    private final Map<String, Integer> occupancy = new HashMap<>();
    private final Map<String, Integer> reservations = new HashMap<>();
    private final Map<UUID, String> playerReservations = new HashMap<>();

    public CapacityListener(WorldService worlds, FileConfiguration config) {
        this.worlds = worlds;
        this.maxPlayersPerWorld = Math.max(1, config.getInt("server.max-players-per-world", 1000));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void reserveTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        World from = event.getFrom().getWorld();
        World to = event.getTo().getWorld();
        if (from == to || worlds.definition(to).isEmpty()) return;
        if (!reserve(event.getPlayer().getUniqueId(), to.getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("That mining world is at its " + maxPlayersPerWorld + " player capacity."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void releaseCancelledReservation(PlayerTeleportEvent event) {
        if (!event.isCancelled()) return;
        releaseReservation(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();
        if (worlds.definition(world).isPresent()) increment(world.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        releaseReservation(event.getPlayer().getUniqueId());
        World world = event.getPlayer().getWorld();
        if (worlds.definition(world).isPresent()) decrement(world.getName());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        Player player = event.getPlayer();
        World to = player.getWorld();
        if (worlds.definition(from).isPresent()) decrement(from.getName());
        if (worlds.definition(to).isPresent()) {
            consumeReservation(player.getUniqueId(), to.getName());
            increment(to.getName());
        } else {
            releaseReservation(player.getUniqueId());
        }
    }

    public int occupancy(String worldName) {
        synchronized (lock) { return occupancy.getOrDefault(worldName, 0); }
    }

    private boolean reserve(UUID playerId, String worldName) {
        synchronized (lock) {
            releaseReservationLocked(playerId);
            int used = occupancy.getOrDefault(worldName, 0) + reservations.getOrDefault(worldName, 0);
            if (used >= maxPlayersPerWorld) return false;
            playerReservations.put(playerId, worldName);
            reservations.merge(worldName, 1, Integer::sum);
            return true;
        }
    }

    private void consumeReservation(UUID playerId, String worldName) {
        synchronized (lock) {
            String reserved = playerReservations.remove(playerId);
            if (reserved == null) return;
            reservations.computeIfPresent(reserved, (key, value) -> Math.max(0, value - 1));
            if (!reserved.equals(worldName)) reservations.remove(reserved, 0);
        }
    }

    private void releaseReservation(UUID playerId) {
        synchronized (lock) { releaseReservationLocked(playerId); }
    }

    private void releaseReservationLocked(UUID playerId) {
        String reserved = playerReservations.remove(playerId);
        if (reserved == null) return;
        reservations.computeIfPresent(reserved, (key, value) -> Math.max(0, value - 1));
    }

    private void increment(String worldName) {
        synchronized (lock) { occupancy.merge(worldName, 1, Integer::sum); }
    }

    private void decrement(String worldName) {
        synchronized (lock) { occupancy.computeIfPresent(worldName, (key, value) -> Math.max(0, value - 1)); }
    }
}
