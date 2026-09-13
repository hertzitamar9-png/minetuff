package dev.minetuff.world;

import org.bukkit.Material;

import java.util.List;

public record WorldDefinition(
        int id,
        String slug,
        String displayName,
        String family,
        int stage,
        int requiredPrestige,
        double entryCost,
        double sellMultiplier,
        Material primary,
        Material accent,
        Material gem,
        List<Material> minePalette
) {
    public String worldName() {
        return "mt_" + String.format("%03d", id) + "_" + slug;
    }
}
