package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.GearApi;
import net.silentchaos512.gear.api.material.IMaterialInstance;
import net.silentchaos512.gear.gear.material.MaterialModifiers;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapts SilentTinkers' bounded material variant to Silent Gear's native
 * starlight-charging calculation. The charger remains the owner of its
 * structure, catalyst, time, and stored-energy rules; this class only teaches
 * it how to read and rewrite a composite Tinkers material.
 */
public final class CompositeStarChargeService {
    private CompositeStarChargeService() {}

    public static boolean isComposite(ItemStack stack) {
        return !compositeVariants(stack).isEmpty();
    }

    public static boolean canBeginCharging(ItemStack stack) {
        List<String> variants = compositeVariants(stack);
        if (variants.isEmpty()) return false;
        for (String variant : variants) {
            try {
                if (AlloyVariantCodec.decodeStarChargeLevel(variant) > 0) return false;
                ItemStack source = sourceStack(variant);
                if (source.isEmpty() || !GearApi.isMaterial(source)) return false;
            } catch (IllegalArgumentException | LinkageError exception) {
                return false;
            }
        }
        return true;
    }

    public static int chargeLevel(ItemStack stack) {
        int level = 0;
        for (String variant : compositeVariants(stack)) {
            try {
                level = Math.max(level, AlloyVariantCodec.decodeStarChargeLevel(variant));
            } catch (IllegalArgumentException exception) {
                return 0;
            }
        }
        return level;
    }

    /** Matches Silent Gear's normal 100 ticks per material tier rule. */
    public static int workTime(ItemStack stack) {
        int tier = 1;
        for (String variant : compositeVariants(stack)) {
            try {
                IMaterialInstance material = GearApi.getMaterial(sourceStack(variant));
                if (material != null) tier = Math.max(tier, material.getTier());
            } catch (IllegalArgumentException | LinkageError ignored) {
                // Eligibility already fails closed; keep this defensive fallback.
            }
        }
        return 100 * tier;
    }

    /** Rewrites every composite material in the part/tool and rebuilds tool stats. */
    public static boolean applyCharge(ItemStack output, int level) {
        if (level <= 0 || !canBeginCharging(output)) return false;
        try {
            if (output.getItem() instanceof IMaterialItem materialItem) {
                MaterialVariantId current = materialItem.getMaterial(output);
                MaterialVariantId charged = chargeVariant(current, level);
                materialItem.setMaterialForced(output, charged);
                writePartPayload(output, charged);
                logSuccess(output, level, 1);
                return true;
            }

            if (output.getItem() instanceof IModifiable) {
                ToolStack tool = ToolStack.from(output);
                MaterialNBT materials = tool.getMaterials();
                int changed = 0;
                for (int index = 0; index < materials.size(); index++) {
                    MaterialVariant current = materials.get(index);
                    if (!current.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) continue;
                    MaterialVariantId charged = chargeVariant(current.getVariant(), level);
                    materials = materials.replaceMaterial(index, charged);
                    changed++;
                }
                if (changed == 0) return false;
                tool.setMaterials(materials);
                logSuccess(output, level, changed);
                return true;
            }
        } catch (RuntimeException | LinkageError exception) {
            SilentTinkersMod.LOGGER.error(
                    "[SilentTinkers:STARCHARGE_FAILED] item={} level={} -- composite output was left unchanged",
                    output.getItem(), level, exception);
        }
        return false;
    }

    private static MaterialVariantId chargeVariant(MaterialVariantId current, int level) {
        if (!current.getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
            throw new IllegalArgumentException("Not a SilentTinkers composite material");
        }
        String encoded = current.getVariant();
        AlloyComposition composition = AlloyVariantCodec.decode(encoded);
        ItemStack source = sourceStack(encoded);

        MaterialModifiers.STARCHARGED.write(MaterialModifiers.STARCHARGED.create(level), source);
        Optional<AlloyStatSnapshot> chargedStats = SilentGearStatReader.read(source);
        if (chargedStats.isEmpty()) {
            throw new IllegalStateException("Silent Gear did not evaluate the charged source material");
        }
        Optional<SourceVisualIdentity> chargedVisual = SourceVisualIdentity.capture(source);
        String charged = AlloyVariantCodec.encode(composition, level, chargedStats, chargedVisual);
        return MaterialVariantId.create(CompositePickHeadCastingRecipe.MATERIAL, charged);
    }

    private static void writePartPayload(ItemStack output, MaterialVariantId charged) {
        String encoded = charged.getVariant();
        AlloyPayload.write(output,
                AlloyVariantCodec.decode(encoded),
                AlloyVariantCodec.decodeStarChargeLevel(encoded),
                AlloyVariantCodec.decodeStats(encoded),
                AlloyVariantCodec.decodeVisualSource(encoded));
    }

    private static ItemStack sourceStack(String variant) {
        return AlloyVariantCodec.decodeVisualSource(variant)
                .map(SourceVisualIdentity::reconstruct)
                .orElse(ItemStack.EMPTY);
    }

    private static List<String> compositeVariants(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        if (stack.getItem() instanceof IMaterialItem materialItem) {
            MaterialVariantId material = materialItem.getMaterial(stack);
            return material.getId().equals(CompositePickHeadCastingRecipe.MATERIAL)
                    ? List.of(material.getVariant()) : List.of();
        }
        if (stack.getItem() instanceof IModifiable) {
            List<String> result = new ArrayList<>();
            for (MaterialVariant material : ToolStack.from(stack).getMaterials()) {
                if (material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                    result.add(material.getVariant().getVariant());
                }
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private static void logSuccess(ItemStack output, int level, int materials) {
        SilentTinkersMod.LOGGER.info(
                "[SilentTinkers:STARCHARGE_APPLIED] item={} level={} compositeMaterials={} statsRebuilt=true",
                output.getItem(), level, materials);
    }
}
