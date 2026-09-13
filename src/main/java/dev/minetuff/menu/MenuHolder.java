package dev.minetuff.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final int page;
    private final Inventory inventory;

    public MenuHolder(MenuType type, int page, int size, String title) {
        this.type = type;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
    }

    public MenuType type() { return type; }
    public int page() { return page; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public enum MenuType {
        MAIN,
        WORLDS,
        SHOP,
        CRATES,
        TOOLS
    }
}
