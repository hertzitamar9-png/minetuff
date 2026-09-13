package dev.minetuff.tools;

import dev.minetuff.model.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolService {
    private final ConcurrentHashMap<UUID, ToolMode> activeModes = new ConcurrentHashMap<>();
    private final NamespacedKey toolKey;

    public ToolService(Plugin plugin) {
        toolKey = new NamespacedKey(plugin, "mining_tool");
    }

    public ToolMode active(Player player, PlayerProfile profile) {
        ToolMode selected = activeModes.getOrDefault(player.getUniqueId(), ToolMode.PICKAXE);
        if (profile.toolLevel() < selected.requiredLevel()) {
            activeModes.put(player.getUniqueId(), ToolMode.PICKAXE);
            return ToolMode.PICKAXE;
        }
        return selected;
    }

    public SelectResult select(Player player, PlayerProfile profile, ToolMode mode) {
        if (profile.toolLevel() < mode.requiredLevel()) {
            return new SelectResult(false, mode.displayName() + " unlocks at tool level " + mode.requiredLevel() + ".");
        }
        activeModes.put(player.getUniqueId(), mode);
        syncItem(player, profile);
        return new SelectResult(true, "Active tool: " + mode.displayName() + ".");
    }

    public void syncItem(Player player, PlayerProfile profile) {
        ToolMode mode = active(player, profile);
        ItemStack tool = buildItem(mode, profile.toolLevel());
        int existingSlot = -1;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (isMineTuffTool(storage[i])) {
                existingSlot = i;
                break;
            }
        }
        if (existingSlot >= 0) {
            player.getInventory().setItem(existingSlot, tool);
            return;
        }
        int empty = player.getInventory().firstEmpty();
        if (empty >= 0) player.getInventory().setItem(empty, tool);
        else player.getInventory().setItem(0, tool);
    }

    public boolean isMineTuffTool(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        String marker = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        return "1".equals(marker);
    }

    public List<Offset> extraBlocks(Player player, PlayerProfile profile) {
        ToolMode mode = active(player, profile);
        if (mode == ToolMode.PICKAXE) return List.of();
        BlockFace facing = player.getFacing();
        return switch (mode) {
            case HAMMER -> wallOffsets(facing, 1);
            case DRILL -> drillOffsets(facing, 5);
            case LASER -> wallOffsets(facing, 3);
            case PICKAXE -> List.of();
        };
    }

    private ItemStack buildItem(ToolMode mode, int level) {
        Material material = switch (mode) {
            case PICKAXE -> Material.DIAMOND_PICKAXE;
            case HAMMER -> Material.NETHERITE_PICKAXE;
            case DRILL -> Material.GOLDEN_PICKAXE;
            case LASER -> Material.NETHERITE_HOE;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("MineTuff " + mode.displayName() + " · Lv." + level));
        meta.lore(List.of(
                Component.text("Permanent mining tool"),
                Component.text("Mode unlock level: " + mode.requiredLevel()),
                Component.text("Upgrade with /upgrade")
        ));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addEnchant(Enchantment.EFFICIENCY, Math.min(20, 1 + level / 12), true);
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, "1");
        item.setItemMeta(meta);
        return item;
    }

    private static List<Offset> wallOffsets(BlockFace facing, int depth) {
        List<Offset> offsets = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    Offset offset;
                    if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                        int dir = facing == BlockFace.EAST ? 1 : -1;
                        offset = new Offset(d * dir, b, a);
                    } else {
                        int dir = facing == BlockFace.SOUTH ? 1 : -1;
                        offset = new Offset(a, b, d * dir);
                    }
                    if (offset.dx == 0 && offset.dy == 0 && offset.dz == 0) continue;
                    offsets.add(offset);
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<Offset> drillOffsets(BlockFace facing, int length) {
        List<Offset> offsets = new ArrayList<>();
        for (int d = 1; d < length; d++) {
            int dx = 0;
            int dz = 0;
            if (facing == BlockFace.EAST) dx = d;
            else if (facing == BlockFace.WEST) dx = -d;
            else if (facing == BlockFace.SOUTH) dz = d;
            else dz = -d;
            offsets.add(new Offset(dx, 0, dz));
        }
        return List.copyOf(offsets);
    }

    public void clear(UUID playerId) { activeModes.remove(playerId); }

    public record Offset(int dx, int dy, int dz) {}
    public record SelectResult(boolean success, String message) {}
}
