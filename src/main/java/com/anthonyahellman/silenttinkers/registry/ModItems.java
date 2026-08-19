package com.anthonyahellman.silenttinkers.registry;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.item.CompositeAlloySampleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SilentTinkersMod.MOD_ID);

    public static final RegistryObject<Item> COMPOSITE_ALLOY_SAMPLE = ITEMS.register(
            "composite_alloy_sample", () -> new CompositeAlloySampleItem(new Item.Properties().stacksTo(64)));

    private ModItems() {}
}
