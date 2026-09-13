package dev.minetuff.command;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.mine.MineService;
import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.world.WorldCatalog;
import dev.minetuff.world.WorldDefinition;
import dev.minetuff.world.WorldService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class AdminCommand implements CommandExecutor {
    private final ProfileRepository profiles;
    private final WorldCatalog catalog;
    private final WorldService worlds;
    private final MineService mines;

    public AdminCommand(ProfileRepository profiles, WorldCatalog catalog, WorldService worlds, MineService mines) {
        this.profiles = profiles;
        this.catalog = catalog;
        this.worlds = worlds;
        this.mines = mines;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("minetuff.admin")) {
            sender.sendMessage(Component.text("You do not have permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) return status(sender);
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> save(sender);
            case "goto" -> goTo(sender, args);
            case "givekey" -> giveKey(sender, args);
            case "addmoney" -> addMoney(sender, args);
            case "setworld" -> setWorld(sender, args);
            case "setprestige" -> setPrestige(sender, args);
            default -> help(sender);
        };
    }

    private boolean status(CommandSender sender) {
        long loadedMiningWorlds = Bukkit.getWorlds().stream().filter(world -> worlds.definition(world).isPresent()).count();
        sender.sendMessage(Component.text("MineTuff status · online " + Bukkit.getOnlinePlayers().size()
                + " · loaded mining worlds " + loadedMiningWorlds + "/" + catalog.size()));
        sender.sendMessage(Component.text("Configured per-world cap: 1000 · pods/world: 64"));
        return true;
    }

    private boolean save(CommandSender sender) {
        profiles.saveAllAsync();
        sender.sendMessage(Component.text("Queued profile save for all loaded players."));
        return true;
    }

    private boolean goTo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("/mtadmin goto is player-only."));
            return true;
        }
        Integer id = parseInt(args, 1);
        if (id == null || id < 1 || id > catalog.size()) {
            sender.sendMessage(Component.text("Usage: /mtadmin goto <1-100>"));
            return true;
        }
        WorldDefinition target = catalog.byId(id);
        worlds.travel(player, target, mines).exceptionally(error -> {
            player.sendMessage(Component.text("Admin travel failed: " + error.getMessage()));
            return false;
        });
        return true;
    }

    private boolean giveKey(CommandSender sender, String[] args) {
        if (args.length < 4) return usage(sender, "/mtadmin givekey <player> <common|rare|epic> <amount>");
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) return usage(sender, "Player must be online.");
        CrateTier tier;
        int amount;
        try {
            tier = CrateTier.valueOf(args[2].toUpperCase(Locale.ROOT));
            amount = Integer.parseInt(args[3]);
        } catch (IllegalArgumentException ex) {
            return usage(sender, "Invalid crate tier or amount.");
        }
        if (amount < 1 || amount > 10000) return usage(sender, "Amount must be 1-10000.");
        profiles.get(target.getUniqueId()).addCrateKey(tier, amount);
        sender.sendMessage(Component.text("Added " + amount + " " + tier.name().toLowerCase(Locale.ROOT) + " keys to " + target.getName() + "."));
        return true;
    }

    private boolean addMoney(CommandSender sender, String[] args) {
        if (args.length < 3) return usage(sender, "/mtadmin addmoney <player> <amount>");
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) return usage(sender, "Player must be online.");
        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (NumberFormatException ex) { return usage(sender, "Invalid amount."); }
        if (!Double.isFinite(amount) || amount <= 0.0 || amount > 9.0e15) return usage(sender, "Amount is out of range.");
        profiles.get(target.getUniqueId()).addBalance(amount);
        sender.sendMessage(Component.text("Added $" + String.format("%,.0f", amount) + " to " + target.getName() + "."));
        return true;
    }

    private boolean setWorld(CommandSender sender, String[] args) {
        if (args.length < 3) return usage(sender, "/mtadmin setworld <player> <1-100>");
        Player target = Bukkit.getPlayerExact(args[1]);
        Integer id = parseInt(args, 2);
        if (target == null || id == null || id < 1 || id > catalog.size()) return usage(sender, "Invalid online player or world id.");
        profiles.get(target.getUniqueId()).setWorldId(id);
        sender.sendMessage(Component.text("Set " + target.getName() + " unlocked world to " + id + "."));
        return true;
    }

    private boolean setPrestige(CommandSender sender, String[] args) {
        if (args.length < 3) return usage(sender, "/mtadmin setprestige <player> <0-100>");
        Player target = Bukkit.getPlayerExact(args[1]);
        Integer prestige = parseInt(args, 2);
        if (target == null || prestige == null || prestige < 0 || prestige > 100) return usage(sender, "Invalid online player or prestige.");
        profiles.get(target.getUniqueId()).setPrestige(prestige);
        sender.sendMessage(Component.text("Set " + target.getName() + " prestige to " + prestige + "."));
        return true;
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(Component.text("/mtadmin status|save|goto|givekey|addmoney|setworld|setprestige"));
        return true;
    }

    private static boolean usage(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message));
        return true;
    }

    private static Integer parseInt(String[] args, int index) {
        if (args.length <= index) return null;
        try { return Integer.parseInt(args[index]); }
        catch (NumberFormatException ex) { return null; }
    }
}
