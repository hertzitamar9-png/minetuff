package dev.minetuff.data;

import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileRepository {
    private final Plugin plugin;
    private final File playersDirectory;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public ProfileRepository(Plugin plugin) {
        this.plugin = plugin;
        this.playersDirectory = new File(plugin.getDataFolder(), "players");
        if (!playersDirectory.exists() && !playersDirectory.mkdirs()) {
            plugin.getLogger().warning("Could not create player data directory: " + playersDirectory);
        }
    }

    public PlayerProfile get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void unload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile != null) saveAsync(profile);
    }

    public void saveAsync(PlayerProfile profile) {
        PlayerProfile.Snapshot snapshot = profile.snapshot();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> saveSnapshot(snapshot));
    }

    public void saveAllAsync() {
        for (PlayerProfile profile : cache.values()) saveAsync(profile);
    }

    public void saveAllBlocking() {
        for (PlayerProfile profile : cache.values()) saveSnapshot(profile.snapshot());
    }

    private PlayerProfile load(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.isFile()) return PlayerProfile.fresh(uuid);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        EnumMap<CrateTier, Integer> keys = new EnumMap<>(CrateTier.class);
        keys.put(CrateTier.COMMON, yaml.getInt("crates.common", 0));
        keys.put(CrateTier.RARE, yaml.getInt("crates.rare", 0));
        keys.put(CrateTier.EPIC, yaml.getInt("crates.epic", 0));
        return new PlayerProfile(uuid,
                yaml.getDouble("balance", 0.0),
                yaml.getLong("tokens", 0L),
                yaml.getInt("prestige", 0),
                yaml.getInt("tool-level", 1),
                yaml.getInt("world-id", 1),
                yaml.getLong("blocks-mined", 0L),
                yaml.getLong("stored-blocks", 0L),
                yaml.getLong("last-daily-epoch-day", -1L),
                keys);
    }

    private void saveSnapshot(PlayerProfile.Snapshot s) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("balance", s.balance());
        yaml.set("tokens", s.tokens());
        yaml.set("prestige", s.prestige());
        yaml.set("tool-level", s.toolLevel());
        yaml.set("world-id", s.worldId());
        yaml.set("blocks-mined", s.blocksMined());
        yaml.set("stored-blocks", s.storedBlocks());
        yaml.set("last-daily-epoch-day", s.lastDailyEpochDay());
        yaml.set("crates.common", s.commonKeys());
        yaml.set("crates.rare", s.rareKeys());
        yaml.set("crates.epic", s.epicKeys());
        try {
            yaml.save(fileFor(s.uuid()));
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save profile " + s.uuid() + ": " + ex.getMessage());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playersDirectory, uuid + ".yml");
    }
}
