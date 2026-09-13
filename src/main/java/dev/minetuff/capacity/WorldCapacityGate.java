package dev.minetuff.capacity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WorldCapacityGate {
    private final int maxPlayersPerWorld;
    private final Map<String, Integer> occupancy = new HashMap<>();
    private final Map<String, Integer> reservations = new HashMap<>();
    private final Map<UUID, String> playerReservations = new HashMap<>();

    public WorldCapacityGate(int maxPlayersPerWorld) {
        if (maxPlayersPerWorld < 1) throw new IllegalArgumentException("maxPlayersPerWorld must be positive");
        this.maxPlayersPerWorld = maxPlayersPerWorld;
    }

    public synchronized boolean reserve(UUID playerId, String worldName) {
        releaseReservationLocked(playerId);
        int used = occupancy.getOrDefault(worldName, 0) + reservations.getOrDefault(worldName, 0);
        if (used >= maxPlayersPerWorld) return false;
        playerReservations.put(playerId, worldName);
        reservations.merge(worldName, 1, Integer::sum);
        return true;
    }

    public synchronized void enter(UUID playerId, String worldName) {
        consumeReservationLocked(playerId);
        occupancy.merge(worldName, 1, Integer::sum);
    }

    public synchronized void leave(String worldName) {
        occupancy.computeIfPresent(worldName, (key, value) -> Math.max(0, value - 1));
    }

    public synchronized void releaseReservation(UUID playerId) {
        releaseReservationLocked(playerId);
    }

    public synchronized int occupancy(String worldName) {
        return occupancy.getOrDefault(worldName, 0);
    }

    public synchronized int reserved(String worldName) {
        return reservations.getOrDefault(worldName, 0);
    }

    public synchronized int used(String worldName) {
        return occupancy(worldName) + reserved(worldName);
    }

    public int maxPlayersPerWorld() {
        return maxPlayersPerWorld;
    }

    private void consumeReservationLocked(UUID playerId) {
        String reservedWorld = playerReservations.remove(playerId);
        if (reservedWorld == null) return;
        decrementReservation(reservedWorld);
    }

    private void releaseReservationLocked(UUID playerId) {
        String reservedWorld = playerReservations.remove(playerId);
        if (reservedWorld == null) return;
        decrementReservation(reservedWorld);
    }

    private void decrementReservation(String worldName) {
        reservations.computeIfPresent(worldName, (key, value) -> {
            int next = value - 1;
            return next <= 0 ? null : next;
        });
    }
}
