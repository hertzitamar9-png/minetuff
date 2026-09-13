package dev.minetuff;

import dev.minetuff.command.MineTuffCommand;
import dev.minetuff.crates.CrateService;
import dev.minetuff.data.ProfileRepository;
import dev.minetuff.economy.EconomyService;
import dev.minetuff.hud.HudService;
import dev.minetuff.listener.CapacityListener;
import dev.minetuff.listener.MiningListener;
import dev.minetuff.listener.PortalListener;
import dev.minetuff.listener.SessionListener;
import dev.minetuff.mine.MineService;
import dev.minetuff.progression.ProgressionService;
import dev.minetuff.tools.ToolService;
import dev.minetuff.world.WorldCatalog;
import dev.minetuff.world.WorldService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MineTuffPlugin extends JavaPlugin {
    private ProfileRepository profiles;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        WorldCatalog catalog = new WorldCatalog();
        profiles = new ProfileRepository(this);
        EconomyService economy = new EconomyService(getConfig());
        ProgressionService progression = new ProgressionService(getConfig());
        CrateService crates = new CrateService(getConfig());
        ToolService tools = new ToolService();
        MineService mines = new MineService(this, getConfig());
        WorldService worlds = new WorldService(this, catalog, getConfig());

        Bukkit.getPluginManager().registerEvents(new SessionListener(profiles), this);
        Bukkit.getPluginManager().registerEvents(new CapacityListener(worlds, getConfig()), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this, profiles, economy, crates, mines, worlds, tools), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(profiles, progression, catalog, worlds, mines), this);

        MineTuffCommand commandHandler = new MineTuffCommand(profiles, economy, progression, crates, catalog, worlds, mines, tools);
        for (String name : List.of("mine", "worlds", "balance", "sellall", "upgrade", "tool", "prestige", "crate", "shop", "daily", "stats")) {
            PluginCommand command = getCommand(name);
            if (command == null) throw new IllegalStateException("Missing command in plugin.yml: " + name);
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        long autosaveTicks = Math.max(20L, getConfig().getLong("server.autosave-seconds", 60L) * 20L);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> profiles.saveAllAsync(), autosaveTicks, autosaveTicks);
        new HudService(this, profiles, economy, tools, worlds).start();
        worlds.prewarm();

        getLogger().info("MineTuff enabled with " + catalog.size() + " mining worlds, 64 Folia-distributed pods per world, four mining tool modes, and a 1000-player hard cap per world.");
    }

    @Override
    public void onDisable() {
        if (profiles != null) profiles.saveAllBlocking();
    }
}
