package com.anthonyahellman.silenttinkers.mixin;

import com.anthonyahellman.silenttinkers.compat.silentgear.CompositeStarChargeService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.silentchaos512.gear.api.GearApi;
import net.silentchaos512.gear.block.charger.ChargerTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Narrow extensions to Silent Gear's existing starlight charger lifecycle. */
@Mixin(value = ChargerTileEntity.class, remap = false)
public abstract class ChargerTileEntityMixin {
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void silenttinkers$allowCompositeInput(int slot, ItemStack stack,
                                                    CallbackInfoReturnable<Boolean> callback) {
        if (slot == 0 && CompositeStarChargeService.canBeginCharging(stack)) callback.setReturnValue(true);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/silentchaos512/gear/api/GearApi;isMaterial(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean silenttinkers$treatCompositeAsMaterial(ItemStack stack) {
        return GearApi.isMaterial(stack) || CompositeStarChargeService.canBeginCharging(stack);
    }

    @Inject(method = "canCharge", at = @At("HEAD"), cancellable = true)
    private static void silenttinkers$allowCompositeCharge(ItemStack stack,
                                                            CallbackInfoReturnable<Boolean> callback) {
        if (CompositeStarChargeService.isComposite(stack)) {
            callback.setReturnValue(CompositeStarChargeService.canBeginCharging(stack));
        }
    }

    @Inject(method = "getWorkTime", at = @At("HEAD"), cancellable = true)
    private void silenttinkers$compositeWorkTime(ItemStack stack,
                                                 CallbackInfoReturnable<Integer> callback) {
        if (CompositeStarChargeService.isComposite(stack)) {
            callback.setReturnValue(CompositeStarChargeService.workTime(stack));
        }
    }

    @Inject(method = "getMaterialChargeLevel", at = @At("HEAD"), cancellable = true)
    private void silenttinkers$readCompositeCharge(ItemStack stack,
                                                   CallbackInfoReturnable<Integer> callback) {
        if (CompositeStarChargeService.isComposite(stack)) {
            callback.setReturnValue(CompositeStarChargeService.chargeLevel(stack));
        }
    }

    @Inject(method = "chargeMaterial", at = @At("HEAD"), cancellable = true)
    private void silenttinkers$chargeComposite(ItemStack output, int level, CallbackInfo callback) {
        if (CompositeStarChargeService.isComposite(output)) {
            CompositeStarChargeService.applyCharge(output, level);
            callback.cancel();
        }
    }
}
