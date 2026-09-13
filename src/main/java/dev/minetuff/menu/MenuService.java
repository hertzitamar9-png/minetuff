package dev.minetuff.menu;

import dev.minetuff.data.ProfileRepository;
import dev.minetuff.model.CrateTier;
import dev.minetuff.model.PlayerProfile;
import dev.minetuff.tools.ToolMode;
import dev.minetuff.tools.ToolService;
import dev.minetuff.world.WorldCatalog;
import dev.minetuff.world.WorldDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MenuService implements Listener {
    private static final int WORLDS_PER_PAGE = 45;
    private final ProfileRepository profiles;
    private final WorldCatalog catalog;
    private final ToolService tools;

    public MenuService(ProfileRepository profiles, WorldCatalog catalog, ToolService tools) {
        this.profiles = profiles;
        this.catalog = catalog;
        this.tools = tools;
    }

    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.MenuType.MAIN, 0, 45, "MineTuff");
        PlayerProfile profile = profiles.get(player.getUniqueId());
        holder.getInventory().setItem(10, item(Material.DIAMOND_PICKAXE, "Enter Mine",
                "Travel to world " + profile.worldId(), "Your personal Folia mining pod"));
        holder.getInventory().setItem(12, item(Material.COMPASS, "100 Worlds",
                "Browse and travel through progression worlds"));
        holder.getInventory().setItem(14, item(Material.EMERALD, "Shop",
                "Upgrade tools, unlock worlds and buy crate keys"));
        holder.getInventory().setItem(16, item(Material.CHEST, "Crates",
                "Common: " + profile.crateKeys(CrateTier.COMMON),
                "Rare: " + profile.crateKeys(CrateTier.RARE),
                "Epic: " + profile.crateKeys(CrateTier.EPIC)));
        holder.getInventory().setItem(30, item(Material.NETHER_STAR, "Prestige",
                "Current prestige: " + profile.prestige(), "Reset the run for permanent bonuses"));
        holder.getInventory().setItem(32, item(Material.NETHERITE_PICKAXE, "Mining Tools",
                "Active: " + tools.active(player, profile).displayName(),
                "Pickaxe · Hammer · Drill · Laser"));
        holder.getInventory().setItem(34, item(Material.CLOCK, "Daily Reward", "Claim today's money, tokens and crate key"));
        player.openInventory(holder.getInventory());
    }

    public void openWorlds(Player player, int requestedPage) {
        int maxPage = Math.max(0, (catalog.size() - 1) / WORLDS_PER_PAGE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));
        MenuHolder holder = new MenuHolder(MenuHolder.MenuType.WORLDS, page, 54,
                "MineTuff Worlds " + (page + 1) + "/" + (maxPage + 1));
        PlayerProfile profile = profiles.get(player.getUniqueId());
        int start = page * WORLDS_PER_PAGE;
        for (int slot = 0; slot < WORLDS_PER_PAGE; slot++) {
            int id = start + slot + 1;
            if (id > catalog.size()) break;
            WorldDefinition def = catalog.byId(id);
            boolean unlocked = id <= profile.worldId() && profile.prestige() >= def.requiredPrestige();
            Material icon = unlocked ? def.gem() : Material.GRAY_DYE;
            holder.getInventory().setItem(slot, item(icon, "#" + id + " " + def.displayName(),
                    unlocked ? "UNLOCKED" : "LOCKED",
                    "Prestige required: " + def.requiredPrestige(),
                    "Unlock cost: $" + String.format("%,.0f", def.entryCost()),
                    "Sell multiplier: x" + String.format("%.2f", def.sellMultiplier())));
        }
        if (page > 0) holder.getInventory().setItem(45, item(Material.ARROW, "Previous Page"));
        holder.getInventory().setItem(49, item(Material.BARRIER, "Back"));
        if (page < maxPage) holder.getInventory().setItem(53, item(Material.ARROW, "Next Page"));
        player.openInventory(holder.getInventory());
    }

    public void openShop(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.MenuType.SHOP, 0, 27, "MineTuff Shop");
        holder.getInventory().setItem(10, item(Material.NETHERITE_PICKAXE, "Upgrade Tool", "Same action as /upgrade"));
        holder.getInventory().setItem(12, item(Material.ENDER_EYE, "Unlock Next World", "Same action as /shop world"));
        holder.getInventory().setItem(14, item(Material.TRIPWIRE_HOOK, "Common Key", "100 tokens"));
        holder.getInventory().setItem(15, item(Material.TRIPWIRE_HOOK, "Rare Key", "500 tokens"));
        holder.getInventory().setItem(16, item(Material.TRIPWIRE_HOOK, "Epic Key", "2,500 tokens"));
        holder.getInventory().setItem(22, item(Material.BARRIER, "Back"));
        player.openInventory(holder.getInventory());
    }

    public void openCrates(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        MenuHolder holder = new MenuHolder(MenuHolder.MenuType.CRATES, 0, 27, "MineTuff Crates");
        holder.getInventory().setItem(11, item(Material.CHEST, "Common Crate", "Keys: " + profile.crateKeys(CrateTier.COMMON)));
        holder.getInventory().setItem(13, item(Material.ENDER_CHEST, "Rare Crate", "Keys: " + profile.crateKeys(CrateTier.RARE)));
        holder.getInventory().setItem(15, item(Material.SHULKER_BOX, "Epic Crate", "Keys: " + profile.crateKeys(CrateTier.EPIC)));
        holder.getInventory().setItem(22, item(Material.BARRIER, "Back"));
        player.openInventory(holder.getInventory());
    }

    public void openTools(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        MenuHolder holder = new MenuHolder(MenuHolder.MenuType.TOOLS, 0, 27, "MineTuff Tools");
        int[] slots = {10, 12, 14, 16};
        ToolMode[] modes = ToolMode.values();
        Material[] icons = {Material.DIAMOND_PICKAXE, Material.IRON_AXE, Material.IRON_PICKAXE, Material.NETHERITE_PICKAXE};
        for (int i = 0; i < modes.length; i++) {
            ToolMode mode = modes[i];
            boolean unlocked = profile.toolLevel() >= mode.requiredLevel();
            holder.getInventory().setItem(slots[i], item(unlocked ? icons[i] : Material.GRAY_DYE,
                    mode.displayName(), "Requires tool level " + mode.requiredLevel(), unlocked ? "Click to equip" : "LOCKED"));
        }
        holder.getInventory().setItem(22, item(Material.BARRIER, "Back"));
        player.openInventory(holder.getInventory());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.type()) {
            case MAIN -> clickMain(player, slot);
            case WORLDS -> clickWorlds(player, holder.page(), slot);
            case SHOP -> clickShop(player, slot);
            case CRATES -> clickCrates(player, slot);
            case TOOLS -> clickTools(player, slot);
        }
    }

    private void clickMain(Player player, int slot) {
        switch (slot) {
            case 10 -> run(player, "mine");
            case 12 -> openWorlds(player, 0);
            case 14 -> openShop(player);
            case 16 -> openCrates(player);
            case 30 -> run(player, "prestige");
            case 32 -> openTools(player);
            case 34 -> run(player, "daily");
            default -> { }
        }
    }

    private void clickWorlds(Player player, int page, int slot) {
        if (slot < WORLDS_PER_PAGE) {
            int id = page * WORLDS_PER_PAGE + slot + 1;
            if (id <= catalog.size()) run(player, "mine " + id);
            return;
        }
        if (slot == 45) openWorlds(player, page - 1);
        else if (slot == 49) openMain(player);
        else if (slot == 53) openWorlds(player, page + 1);
    }

    private void clickShop(Player player, int slot) {
        switch (slot) {
            case 10 -> run(player, "upgrade");
            case 12 -> run(player, "shop world");
            case 14 -> run(player, "shop common");
            case 15 -> run(player, "shop rare");
            case 16 -> run(player, "shop epic");
            case 22 -> openMain(player);
            default -> { }
        }
    }

    private void clickCrates(Player player, int slot) {
        switch (slot) {
            case 11 -> run(player, "crate common");
            case 13 -> run(player, "crate rare");
            case 15 -> run(player, "crate epic");
            case 22 -> openMain(player);
            default -> { }
        }
    }

    private void clickTools(Player player, int slot) {
        switch (slot) {
            case 10 -> run(player, "tool pickaxe");
            case 12 -> run(player, "tool hammer");
            case 14 -> run(player, "tool drill");
            case 16 -> run(player, "tool laser");
            case 22 -> openMain(player);
            default -> { }
        }
    }

    private void run(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }

    private static ItemStack item(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(Component.text(line));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
