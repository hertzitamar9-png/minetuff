package dev.minetuff.command;

import dev.minetuff.crates.CrateService;
import dev.minetuff.data.ProfileRepository;
import dev.minetuff.economy.EconomyService;
import dev.minetuff.mine.MineService;
import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.progression.ProgressionService;
import dev.minetuff.world.WorldCatalog;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MineTuffCommand implements CommandExecutor, TabCompleter {
    private final ProfileRepository profiles;
    private final EconomyService economy;
    private final ProgressionService progression;
    private final CrateService crates;
    private final WorldCatalog catalog;
    private final WorldService worlds;
    private final MineService mines;

    public MineTuffCommand(ProfileRepository profiles, EconomyService economy, ProgressionService progression,
                           CrateService crates, WorldCatalog catalog, WorldService worlds, MineService mines) {
        this.profiles = profiles;
        this.economy = economy;
        this.progression = progression;
        this.crates = crates;
        this.catalog = catalog;
        this.worlds = worlds;
        this.mines = mines;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players.");
            return true;
        }
        PlayerProfile profile = profiles.get(player.getUniqueId());
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "mine" -> mine(player, profile, args);
            case "worlds" -> listWorlds(player, profile, args);
            case "balance" -> balance(player, profile);
            case "sellall" -> sell(player, profile);
            case "upgrade" -> upgrade(player, profile);
            case "prestige" -> prestige(player, profile);
            case "crate" -> crate(player, profile, args);
            case "shop" -> shop(player, profile, args);
            case "daily" -> daily(player, profile);
            case "stats" -> stats(player, profile);
            default -> false;
        };
    }

    private boolean mine(Player player, PlayerProfile profile, String[] args) {
        int id = profile.worldId();
        if (args.length > 0) {
            try { id = Integer.parseInt(args[0]); }
            catch (NumberFormatException ex) {
                player.sendMessage(Component.text("World must be a number from 1 to " + catalog.size() + "."));
                return true;
            }
        }
        if (id < 1 || id > catalog.size()) {
            player.sendMessage(Component.text("World must be between 1 and " + catalog.size() + "."));
            return true;
        }
        WorldDefinition target = catalog.byId(id);
        if (!progression.canTravel(profile, target)) {
            ProgressionService.UnlockResult unlock = progression.unlockNextWorld(profile, target);
            if (!unlock.success()) {
                player.sendMessage(Component.text(unlock.message() + " Cost: $" + money(unlock.cost())));
                return true;
            }
            player.sendMessage(Component.text(unlock.message()));
        }
        worlds.travel(player, target, mines).exceptionally(error -> {
            player.sendMessage(Component.text("Travel failed: " + error.getMessage()));
            return false;
        });
        return true;
    }

    private boolean listWorlds(Player player, PlayerProfile profile, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try { page = Math.max(1, Integer.parseInt(args[0])); }
            catch (NumberFormatException ignored) { page = 1; }
        }
        int pages = (int) Math.ceil(catalog.size() / 10.0);
        page = Math.min(page, pages);
        int start = (page - 1) * 10;
        player.sendMessage(Component.text("MineTuff Worlds · page " + page + "/" + pages));
        for (int i = start; i < Math.min(start + 10, catalog.size()); i++) {
            WorldDefinition def = catalog.byId(i + 1);
            String state = def.id() <= profile.worldId() && profile.prestige() >= def.requiredPrestige() ? "UNLOCKED" : "LOCKED";
            player.sendMessage(Component.text("#" + def.id() + " " + def.displayName() + " · " + state
                    + " · P" + def.requiredPrestige() + " · $" + money(def.entryCost())));
        }
        return true;
    }

    private boolean balance(Player player, PlayerProfile profile) {
        player.sendMessage(Component.text("Balance: $" + money(profile.balance()) + " · Tokens: " + profile.tokens()));
        return true;
    }

    private boolean sell(Player player, PlayerProfile profile) {
        WorldDefinition def = worlds.definition(player.getWorld()).orElse(catalog.byId(profile.worldId()));
        long units = profile.storedBlocks();
        if (units <= 0L) {
            player.sendMessage(Component.text("Your backpack is empty."));
            return true;
        }
        double earned = economy.sellAll(profile, def);
        player.sendMessage(Component.text("Sold " + units + " ore units for $" + money(earned) + "."));
        return true;
    }

    private boolean upgrade(Player player, PlayerProfile profile) {
        ProgressionService.UpgradeResult result = progression.upgradeTool(profile);
        player.sendMessage(Component.text(result.message() + (result.cost() > 0.0 ? " Cost: $" + money(result.cost()) : "")));
        return true;
    }

    private boolean prestige(Player player, PlayerProfile profile) {
        ProgressionService.PrestigeResult result = progression.prestige(profile);
        player.sendMessage(Component.text(result.message() + " Prestige cost: $" + money(result.cost())));
        return true;
    }

    private boolean crate(Player player, PlayerProfile profile, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("keys")) {
            player.sendMessage(Component.text("Keys · common " + profile.crateKeys(CrateTier.COMMON)
                    + " · rare " + profile.crateKeys(CrateTier.RARE)
                    + " · epic " + profile.crateKeys(CrateTier.EPIC)));
            return true;
        }
        CrateTier tier;
        try { tier = CrateTier.valueOf(args[0].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text("Use /crate common, /crate rare, or /crate epic."));
            return true;
        }
        CrateService.OpenResult result = crates.open(profile, tier);
        player.sendMessage(Component.text(result.message()));
        return true;
    }

    private boolean shop(Player player, PlayerProfile profile, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Shop · /shop tool · /shop world · /shop common|rare|epic"));
            player.sendMessage(Component.text("Key prices: common 100 tokens · rare 500 · epic 2500"));
            return true;
        }
        if (args[0].equalsIgnoreCase("tool")) return upgrade(player, profile);
        if (args[0].equalsIgnoreCase("world")) {
            int next = profile.worldId() + 1;
            if (next > catalog.size()) {
                player.sendMessage(Component.text("All worlds are unlocked for this prestige path."));
                return true;
            }
            WorldDefinition target = catalog.byId(next);
            ProgressionService.UnlockResult result = progression.unlockNextWorld(profile, target);
            player.sendMessage(Component.text(result.message() + " Cost: $" + money(result.cost())));
            return true;
        }
        CrateTier tier;
        try { tier = CrateTier.valueOf(args[0].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text("Unknown shop item."));
            return true;
        }
        long price = switch (tier) { case COMMON -> 100L; case RARE -> 500L; case EPIC -> 2500L; };
        if (!profile.spendTokens(price)) {
            player.sendMessage(Component.text("You need " + price + " tokens."));
            return true;
        }
        profile.addCrateKey(tier, 1);
        player.sendMessage(Component.text("Bought one " + tier.name().toLowerCase(Locale.ROOT) + " crate key."));
        return true;
    }

    private boolean daily(Player player, PlayerProfile profile) {
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        if (profile.lastDailyEpochDay() >= today) {
            player.sendMessage(Component.text("You already claimed today's reward."));
            return true;
        }
        double money = 25_000.0 * (1.0 + profile.prestige() * 0.25);
        long tokens = 75L + profile.prestige() * 5L;
        profile.addBalance(money);
        profile.addTokens(tokens);
        profile.addCrateKey(CrateTier.COMMON, 1);
        profile.setLastDailyEpochDay(today);
        player.sendMessage(Component.text("Daily reward: $" + money(money) + " · " + tokens + " tokens · 1 common key"));
        return true;
    }

    private boolean stats(Player player, PlayerProfile profile) {
        player.sendMessage(Component.text("Stats · Prestige " + profile.prestige() + "/" + progression.maxPrestige()
                + " · World " + profile.worldId() + "/" + catalog.size()
                + " · Tool " + profile.toolLevel() + "/" + progression.maxToolLevel()));
        player.sendMessage(Component.text("Blocks mined: " + String.format("%,d", profile.blocksMined())
                + " · Backpack: " + profile.storedBlocks() + "/" + economy.backpackCapacity(profile)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("crate")) options.addAll(List.of("keys", "common", "rare", "epic"));
        else if (command.getName().equalsIgnoreCase("shop")) options.addAll(List.of("tool", "world", "common", "rare", "epic"));
        else if (command.getName().equalsIgnoreCase("mine")) {
            for (int i = 1; i <= catalog.size(); i++) options.add(Integer.toString(i));
        }
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }

    private static String money(double value) { return String.format("%,.0f", value); }
}
