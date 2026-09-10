package com.anthonyahellman.silenttinkers.registry;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.modifier.CompositeAlloyModifier;
import com.anthonyahellman.silenttinkers.modifier.PixieModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public final class ModModifiers {
    public static final ModifierDeferredRegister MODIFIERS =
            ModifierDeferredRegister.create(SilentTinkersMod.MOD_ID);

    public static final StaticModifier<CompositeAlloyModifier> COMPOSITE_ALLOY =
            MODIFIERS.register("composite_alloy", CompositeAlloyModifier::new);
    public static final StaticModifier<PixieModifier> PIXIE =
            MODIFIERS.register("pixie", PixieModifier::new);

    private ModModifiers() {}
}
