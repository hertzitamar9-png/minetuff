package dev.minetuff.listener;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.model.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionListener implements Listener {
    private final ProfileRepository profiles;

    public SessionListener(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        profiles.preload(event.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerProfile profile = profiles.get(event.getPlayer().getUniqueId());
        event.getPlayer().sendMessage(Component.text("MineTuff · Prestige " + profile.prestige()
                + " · World " + profile.worldId() + " · Tool " + profile.toolLevel()));
        event.getPlayer().sendMessage(Component.text("Use /mine to enter your mining pod and /worlds to browse all 100 worlds."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.unload(event.getPlayer().getUniqueId());
    }
}
