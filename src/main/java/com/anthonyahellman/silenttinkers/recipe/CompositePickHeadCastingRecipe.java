package com.anthonyahellman.silenttinkers.recipe;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyPayload;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
import com.anthonyahellman.silenttinkers.registry.ModFluids;
import com.anthonyahellman.silenttinkers.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.AbstractCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.Optional;

/** First real Tinkers part produced from a dynamic Silent Gear alloy. */
public final class CompositePickHeadCastingRecipe extends AbstractCastingRecipe {
    public static final MaterialId MATERIAL = new MaterialId(SilentTinkersMod.MOD_ID, "composite_alloy");
    private static final int COST = 2 * FluidValues.INGOT;
    private static final TagKey<Item> PICK_HEAD_CASTS = TagKey.create(
            Registries.ITEM, new ResourceLocation("tconstruct", "casts/multi_use/pick_head"));

    public CompositePickHeadCastingRecipe(ResourceLocation id) {
        super(TinkerRecipeTypes.CASTING_TABLE.get(), id, "silent_gear_alloys",
                Ingredient.of(PICK_HEAD_CASTS), false, false);
    }

    @Override
    public boolean matches(ICastingContainer inventory, Level level) {
        return getCast().test(inventory.getStack())
                && inventory.getFluid() == ModFluids.MOLTEN_COMPOSITE_ALLOY.get()
                && AlloyPayload.read(inventory.getFluidTag()).isPresent();
    }

    @Override
    public int getFluidAmount(ICastingContainer inventory) {
        return COST;
    }

    @Override
    public int getCoolingTime(ICastingContainer inventory) {
        return 100;
    }

    @Override
    public ItemStack assemble(ICastingContainer inventory, RegistryAccess access) {
        return AlloyPayload.read(inventory.getFluidTag()).map(composition -> {
            int starChargeLevel = AlloyPayload.readStarChargeLevel(inventory.getFluidTag());
            Optional<AlloyStatSnapshot> sourceStats = AlloyPayload.readStats(inventory.getFluidTag());
            Optional<SourceVisualIdentity> visualSource = AlloyPayload.readVisualSource(inventory.getFluidTag());
            String encoded = AlloyVariantCodec.encode(composition, starChargeLevel, sourceStats);
            MaterialVariantId variant = MaterialVariantId.create(MATERIAL, encoded);

            // This is a deliberately synthetic MaterialVariantId. Tinkers' normal
            // withMaterial() path validates a part's material before writing it.
            // The base composite material is valid, but forcing the known-safe
            // variant here removes validation as a place where our payload could
            // be silently collapsed back to the plain composite material.
            ItemStack part = TinkerToolParts.pickHead.get().withMaterialForDisplay(variant);
            AlloyPayload.write(part, composition, starChargeLevel, sourceStats, visualSource);

            MaterialVariantId stored = IMaterialItem.getMaterialFromStack(part);
            if (!variant.equals(stored)) {
                SilentTinkersMod.LOGGER.error(
                        "[SilentTinkers:CAST_VARIANT_MISMATCH] requested={} stored={} composition={}",
                        variant, stored, composition.fingerprint());
                return part;
            }

            // Verify the exact serialized material string, not just the extra
            // SilentTinkers payload tag. The assembled Tinkers tool only receives
            // the MaterialVariantId, so this is the boundary that must preserve
            // composition/stats for the later modifier hook to recover them.
            try {
                boolean compositionMatches = composition.fingerprint().equals(
                        AlloyVariantCodec.decode(stored.getVariant()).fingerprint());
                Optional<AlloyStatSnapshot> storedVariantStats = AlloyVariantCodec.decodeStats(stored.getVariant());
                boolean statsMatch = sourceStats.equals(storedVariantStats);
                if (!compositionMatches || !statsMatch) {
                    SilentTinkersMod.LOGGER.error(
                            "[SilentTinkers:CAST_VARIANT_PAYLOAD_MISMATCH] material={} compositionMatches={} statsMatch={} sourceStatsPresent={} storedVariantStatsPresent={}",
                            stored, compositionMatches, statsMatch, sourceStats.isPresent(), storedVariantStats.isPresent());
                } else {
                    SilentTinkersMod.LOGGER.info(
                            "[SilentTinkers:CAST_VARIANT_STORED] material={} composition={} statsPresent={} visualSource={} variantPayloadVerified=true",
                            stored, composition.fingerprint(), sourceStats.isPresent(),
                            visualSource.map(value -> value.itemId().toString()).orElse("NONE"));
                }
            } catch (IllegalArgumentException exception) {
                SilentTinkersMod.LOGGER.error(
                        "[SilentTinkers:CAST_VARIANT_PAYLOAD_INVALID] material={} -- stored Tinkers variant could not be decoded after casting",
                        stored, exception);
            }
            return part;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return TinkerToolParts.pickHead.get().withMaterialForDisplay(MATERIAL);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPOSITE_PICK_HEAD_CASTING.get();
    }
}
