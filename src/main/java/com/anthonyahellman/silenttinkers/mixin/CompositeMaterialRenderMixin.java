package com.anthonyahellman.silenttinkers.mixin;

import com.anthonyahellman.silenttinkers.client.AlloyVisualColorResolver;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.Optional;
import java.util.function.Function;

/** Recolors only composite material layers using the encoded alloy composition. */
@Mixin(value = MaterialModel.class, remap = false)
public abstract class CompositeMaterialRenderMixin {
    @Inject(method = "getMaterialSprite", at = @At("RETURN"), cancellable = true)
    private static void silenttinkers$dynamicCompositeColor(
            Function<Material, TextureAtlasSprite> spriteGetter,
            Material texture,
            MaterialVariantId material,
            CallbackInfoReturnable<TintedSprite> callback) {
        if (!CompositePickHeadCastingRecipe.MATERIAL.equals(material.getId()) || !material.hasVariant()) return;
        try {
            String variant = material.getVariant();
            int color = AlloyVisualColorResolver.resolve(
                    AlloyVariantCodec.decode(variant), AlloyVariantCodec.decodeVisualSource(variant));

            ResourceLocation base = texture.texture();
            Material metal = new Material(InventoryMenu.BLOCK_ATLAS,
                    new ResourceLocation(base.getNamespace(), base.getPath() + "_metal"));
            TextureAtlasSprite sprite = spriteGetter.apply(metal);
            if (MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                sprite = callback.getReturnValue().sprite();
            }
            callback.setReturnValue(new TintedSprite(sprite, color, callback.getReturnValue().emissivity()));
        } catch (IllegalArgumentException ignored) {
            // Invalid variants retain Tinkers' normal fallback rendering.
        }
    }
}
