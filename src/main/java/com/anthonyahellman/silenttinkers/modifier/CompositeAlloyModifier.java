package com.anthonyahellman.silenttinkers.modifier;

import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.MaterialIngredient;
import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.config.TraitAccess;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.List;
import java.util.Optional;

/** Replaces the composite head's placeholder contribution with Silent Gear's evaluated stats. */
public final class CompositeAlloyModifier extends Modifier implements ToolStatsModifierHook, ModifierTraitHook {
    private static final float PLACEHOLDER_DURABILITY = 1.0f;
    private static final float PLACEHOLDER_MINING_SPEED = 1.0f;
    private static final float PLACEHOLDER_MELEE_DAMAGE = 1.0f;
    private static final String ENCODED_VARIANT_PREFIX = "v1.";

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.MODIFIER_TRAITS);
    }

    /**
     * Adds the real registered Tinkers traits for alloy ingredients that also
     * exist as Tinkers materials. This deliberately asks Tinkers' live material
     * registry instead of copying trait names, so addon modifier behavior stays
     * owned by the addon that registered it.
     */
    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier, TraitBuilder builder,
                          boolean firstEncounter) {
        if (!firstEncounter) {
            return;
        }
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        for (MaterialVariant material : context.getMaterials()) {
            if (!material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                continue;
            }
            String variant = material.getVariant().getVariant();
            // TConstruct creates base/display material instances whose variant is just
            // "silenttinkers:composite_alloy". Those do not carry an alloy payload and
            // must not be fed to the v1 codec used by real cast composite parts.
            if (!variant.startsWith(ENCODED_VARIANT_PREFIX)) {
                continue;
            }
            AlloyComposition composition;
            try {
                composition = AlloyVariantCodec.decode(variant);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            for (MaterialIngredient ingredient : composition.ingredients()) {
                TraitAccess access = SilentTinkersConfig.traitThresholds()
                        .accessFor(100.0 * ingredient.units() / composition.totalUnits());
                if (access == TraitAccess.NONE) {
                    continue;
                }
                resolveTinkersMaterial(registry, ingredient.materialId()).ifPresent(materialId -> {
                    List<ModifierEntry> traits = registry.getTraits(materialId, HeadMaterialStats.ID);
                    int allowed = switch (access) {
                        case NONE -> 0;
                        case PRIMARY -> 1;
                        case SECONDARY -> 2;
                        case FULL -> traits.size();
                    };
                    traits.stream().limit(allowed).forEach(builder::add);
                });
            }
        }
    }

    private static Optional<MaterialId> resolveTinkersMaterial(IMaterialRegistry registry,
                                                                ResourceLocation sourceId) {
        MaterialId exact = new MaterialId(sourceId);
        if (registry.getMaterial(exact) != IMaterial.UNKNOWN) {
            return Optional.of(exact);
        }

        // Silent Gear's built-in IDs commonly use the silentgear namespace,
        // while the equivalent Tinkers materials use tconstruct with the same path.
        MaterialId tconstruct = new MaterialId("tconstruct", sourceId.getPath());
        if (registry.getMaterial(tconstruct) != IMaterial.UNKNOWN) {
            return Optional.of(tconstruct);
        }

        // Cross-addon bridges often retain the material path but use their own
        // namespace. Accept that handshake only when the path is unique, so a
        // pack with two unrelated materials named alike never gets a random trait.
        List<MaterialId> samePath = registry.getAllMaterials().stream()
                .map(IMaterial::getIdentifier)
                .filter(id -> id.getPath().equals(sourceId.getPath()))
                .distinct()
                .toList();
        if (samePath.size() == 1) {
            return Optional.of(samePath.get(0));
        }
        return Optional.empty();
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        for (MaterialVariant material : context.getMaterials()) {
            if (!material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                continue;
            }
            String variant = material.getVariant().getVariant();
            if (!variant.startsWith(ENCODED_VARIANT_PREFIX)) {
                continue;
            }
            Optional<AlloyStatSnapshot> decoded;
            try {
                decoded = AlloyVariantCodec.decodeStats(variant);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            decoded.ifPresent(stats -> apply(stats, builder));
        }
    }

    private static void apply(AlloyStatSnapshot stats, ModifierStatsBuilder builder) {
        ToolStats.DURABILITY.add(builder, stats.durability() - PLACEHOLDER_DURABILITY);
        ToolStats.MINING_SPEED.add(builder, stats.miningSpeed() - PLACEHOLDER_MINING_SPEED);
        ToolStats.ATTACK_DAMAGE.add(builder, stats.meleeDamage() - PLACEHOLDER_MELEE_DAMAGE);
        ToolStats.ATTACK_SPEED.add(builder, stats.attackSpeed());

        Tier tier = TierSortingRegistry.byName(stats.harvestTier());
        if (tier != null) {
            ToolStats.HARVEST_TIER.update(builder, tier);
        }
    }
}
