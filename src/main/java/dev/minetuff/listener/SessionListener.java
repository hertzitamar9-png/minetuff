package dev.minetuff.listener;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.tools.ToolService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionListener implements Listener {
    private final ProfileRepository profiles;
    private final ToolService tools;

    public SessionListener(ProfileRepository profiles, ToolService tools) {
        this.profiles = profiles;
        this.tools = tools;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        profiles.preload(event.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerProfile profile = profiles.get(event.getPlayer().getUniqueId());
        tools.syncItem(event.getPlayer(), profile);
        event.getPlayer().sendMessage(Component.text("MineTuff · Prestige " + profile.prestige()
                + " · World " + profile.worldId() + " · Tool " + profile.toolLevel()));
        event.getPlayer().sendMessage(Component.text("Use /menu for the full server interface or /mine to enter your mining pod."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tools.clear(event.getPlayer().getUniqueId());
        profiles.unload(event.getPlayer().getUniqueId());
    }
}
