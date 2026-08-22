package com.anthonyahellman.silenttinkers.compat.tconstruct;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.Optional;

/** Read-only adapter for native Tinkers melee/harvest material stats. */
public final class TinkersStatReader {
    private TinkersStatReader() {}

    public static Optional<TinkersStatSnapshot> read(ResourceLocation materialId) {
        if (!MaterialRegistry.isFullyLoaded()) return Optional.empty();

        MaterialId id = new MaterialId(materialId);
        Optional<HeadMaterialStats> head = MaterialRegistry.getInstance().getMaterialStats(id, HeadMaterialStats.ID);
        if (head.isEmpty()) return Optional.empty();

        HeadMaterialStats headStats = head.get();
        ResourceLocation tierId = TierSortingRegistry.getName(headStats.tier());
        if (tierId == null) return Optional.empty();

        HandleMaterialStats handle = MaterialRegistry.getInstance()
                .getMaterialStats(id, HandleMaterialStats.ID)
                .orElse(new HandleMaterialStats(0f, 0f, 0f, 0f));

        return Optional.of(new TinkersStatSnapshot(
                headStats.durability(),
                headStats.miningSpeed(),
                headStats.attack(),
                tierId,
                handle.durability(),
                handle.miningSpeed(),
                handle.meleeSpeed(),
                handle.attackDamage()));
    }
}
