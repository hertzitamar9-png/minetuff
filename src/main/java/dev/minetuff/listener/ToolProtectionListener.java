package dev.minetuff.listener;

import dev.minetuff.tools.ToolService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

public final class ToolProtectionListener implements Listener {
    private final ToolService tools;

    public ToolProtectionListener(ToolService tools) {
        this.tools = tools;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!tools.isMineTuffTool(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("Your MineTuff mining tool cannot be dropped."));
    }

    @EventHandler
    public void onDamage(PlayerItemDamageEvent event) {
        if (tools.isMineTuffTool(event.getItem())) event.setCancelled(true);
    }
}
