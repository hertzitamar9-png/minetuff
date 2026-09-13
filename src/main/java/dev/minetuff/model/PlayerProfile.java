package dev.minetuff.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private double balance;
    private long tokens;
    private int prestige;
    private int toolLevel;
    private int worldId;
    private long blocksMined;
    private long storedBlocks;
    private long lastDailyEpochDay;
    private final EnumMap<CrateTier, Integer> crateKeys = new EnumMap<>(CrateTier.class);

    public PlayerProfile(UUID uuid, double balance, long tokens, int prestige, int toolLevel, int worldId,
                         long blocksMined, long storedBlocks, long lastDailyEpochDay,
                         Map<CrateTier, Integer> keys) {
        this.uuid = uuid;
        this.balance = Math.max(0.0, balance);
        this.tokens = Math.max(0L, tokens);
        this.prestige = Math.max(0, prestige);
        this.toolLevel = Math.max(1, toolLevel);
        this.worldId = Math.max(1, worldId);
        this.blocksMined = Math.max(0L, blocksMined);
        this.storedBlocks = Math.max(0L, storedBlocks);
        this.lastDailyEpochDay = lastDailyEpochDay;
        for (CrateTier tier : CrateTier.values()) {
            this.crateKeys.put(tier, Math.max(0, keys.getOrDefault(tier, 0)));
        }
    }

    public static PlayerProfile fresh(UUID uuid) {
        return new PlayerProfile(uuid, 0.0, 0L, 0, 1, 1, 0L, 0L, -1L, Map.of());
    }

    public UUID uuid() { return uuid; }
    public synchronized double balance() { return balance; }
    public synchronized long tokens() { return tokens; }
    public synchronized int prestige() { return prestige; }
    public synchronized int toolLevel() { return toolLevel; }
    public synchronized int worldId() { return worldId; }
    public synchronized long blocksMined() { return blocksMined; }
    public synchronized long storedBlocks() { return storedBlocks; }
    public synchronized long lastDailyEpochDay() { return lastDailyEpochDay; }

    public synchronized void addBalance(double amount) {
        if (amount > 0.0) balance += amount;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount < 0.0 || balance < amount) return false;
        balance -= amount;
        return true;
    }

    public synchronized void addTokens(long amount) {
        if (amount > 0L) tokens += amount;
    }

    public synchronized boolean spendTokens(long amount) {
        if (amount < 0L || tokens < amount) return false;
        tokens -= amount;
        return true;
    }

    public synchronized void incrementToolLevel() { toolLevel++; }
    public synchronized void setToolLevel(int level) { toolLevel = Math.max(1, level); }
    public synchronized void incrementPrestige() { prestige++; }
    public synchronized void setPrestige(int value) { prestige = Math.max(0, value); }
    public synchronized void setWorldId(int id) { worldId = Math.max(1, id); }
    public synchronized void addBlocksMined(long amount) { if (amount > 0L) blocksMined += amount; }
    public synchronized void addStoredBlocks(long amount) { if (amount > 0L) storedBlocks += amount; }

    public synchronized long removeStoredBlocks(long amount) {
        long removed = Math.min(storedBlocks, Math.max(0L, amount));
        storedBlocks -= removed;
        return removed;
    }

    public synchronized long takeAllStoredBlocks() {
        long amount = storedBlocks;
        storedBlocks = 0L;
        return amount;
    }

    public synchronized int crateKeys(CrateTier tier) { return crateKeys.getOrDefault(tier, 0); }

    public synchronized void addCrateKey(CrateTier tier, int amount) {
        if (amount > 0) crateKeys.merge(tier, amount, Integer::sum);
    }

    public synchronized boolean useCrateKey(CrateTier tier) {
        int current = crateKeys.getOrDefault(tier, 0);
        if (current <= 0) return false;
        crateKeys.put(tier, current - 1);
        return true;
    }

    public synchronized void setLastDailyEpochDay(long value) { lastDailyEpochDay = value; }

    public synchronized Snapshot snapshot() {
        return new Snapshot(uuid, balance, tokens, prestige, toolLevel, worldId, blocksMined, storedBlocks,
                lastDailyEpochDay, crateKeys.getOrDefault(CrateTier.COMMON, 0),
                crateKeys.getOrDefault(CrateTier.RARE, 0), crateKeys.getOrDefault(CrateTier.EPIC, 0));
    }

    public record Snapshot(UUID uuid, double balance, long tokens, int prestige, int toolLevel, int worldId,
                           long blocksMined, long storedBlocks, long lastDailyEpochDay,
                           int commonKeys, int rareKeys, int epicKeys) {}
}
