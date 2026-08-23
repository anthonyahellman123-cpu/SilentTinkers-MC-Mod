package com.anthonyahellman.silenttinkers.mixin;

import com.anthonyahellman.silenttinkers.compat.silentgear.CompositeStarChargeService;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows validated composite parts/tools into the charger's input GUI slot. */
@Mixin(targets = "net.silentchaos512.gear.block.charger.ChargerContainer$1")
public abstract class ChargerInputSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void silenttinkers$allowCompositeInput(ItemStack stack,
                                                    CallbackInfoReturnable<Boolean> callback) {
        if (CompositeStarChargeService.canBeginCharging(stack)) callback.setReturnValue(true);
    }
}
