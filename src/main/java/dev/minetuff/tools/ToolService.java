package dev.minetuff.tools;

import dev.minetuff.model.PlayerProfile;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolService {
    private final ConcurrentHashMap<UUID, ToolMode> activeModes = new ConcurrentHashMap<>();

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
        return new SelectResult(true, "Active tool: " + mode.displayName() + ".");
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
