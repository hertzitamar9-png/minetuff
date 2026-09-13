package dev.minetuff.hud;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.economy.EconomyService;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.tools.ToolService;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class HudService {
    private final Plugin plugin;
    private final ProfileRepository profiles;
    private final EconomyService economy;
    private final ToolService tools;
    private final WorldService worlds;

    public HudService(Plugin plugin, ProfileRepository profiles, EconomyService economy,
                      ToolService tools, WorldService worlds) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.economy = economy;
        this.tools = tools;
        this.worlds = worlds;
    }

    public void start() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().execute(plugin, () -> render(player), null, 1L);
            }
        }, 40L, 40L);
    }

    private void render(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        String worldName = worlds.definition(player.getWorld())
                .map(WorldDefinition::displayName)
                .orElse("Hub");
        String text = "⛏ " + worldName
                + "  |  $" + compact(profile.balance())
                + "  |  P" + profile.prestige()
                + "  |  " + tools.active(player, profile).displayName() + " " + profile.toolLevel()
                + "  |  Bag " + compact(profile.storedBlocks()) + "/" + compact(economy.backpackCapacity(profile));
        player.sendActionBar(Component.text(text));
    }

    private static String compact(double value) {
        if (value >= 1_000_000_000_000.0) return String.format("%.1fT", value / 1_000_000_000_000.0);
        if (value >= 1_000_000_000.0) return String.format("%.1fB", value / 1_000_000_000.0);
        if (value >= 1_000_000.0) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000.0) return String.format("%.1fK", value / 1_000.0);
        return String.format("%.0f", value);
    }
}
