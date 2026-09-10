package com.anthonyahellman.silenttinkers.modifier;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Attribute-level bridge for SilentCompat's Elementium pixie trait. */
public final class PixieModifier extends Modifier implements AttributesModifierHook {
    private static final ResourceLocation PIXIE_SPAWN_CHANCE =
            new ResourceLocation("botania", "pixie_spawn_chance");
    private static final double MAIN_HAND_CHANCE_PER_LEVEL = 0.0125;
    private static final double OFF_HAND_CHANCE_PER_LEVEL = 0.00625;
    private static final UUID MAIN_HAND_UUID = uuid("mainhand");
    private static final UUID OFF_HAND_UUID = uuid("offhand");
    private static final Set<ResourceLocation> LOGGED_MISSING_ATTRIBUTES = ConcurrentHashMap.newKeySet();

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot,
                              BiConsumer<Attribute, AttributeModifier> consumer) {
        double chance;
        UUID uuid;
        if (slot == EquipmentSlot.MAINHAND) {
            chance = MAIN_HAND_CHANCE_PER_LEVEL * modifier.getLevel();
            uuid = MAIN_HAND_UUID;
        } else if (slot == EquipmentSlot.OFFHAND) {
            chance = OFF_HAND_CHANCE_PER_LEVEL * modifier.getLevel();
            uuid = OFF_HAND_UUID;
        } else {
            return;
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(PIXIE_SPAWN_CHANCE);
        if (attribute == null) {
            if (LOGGED_MISSING_ATTRIBUTES.add(PIXIE_SPAWN_CHANCE)) {
                SilentTinkersMod.LOGGER.warn(
                        "[SilentTinkers:TRAIT_ADAPTER_UNAVAILABLE] trait=silentcompat:pixie targetAttribute={} -- Botania pixie behavior is not available",
                        PIXIE_SPAWN_CHANCE);
            }
            return;
        }
        consumer.accept(attribute, new AttributeModifier(uuid, "silenttinkers.pixie_spawn_chance",
                chance, AttributeModifier.Operation.ADDITION));
    }

    static double chanceForLevel(int level, EquipmentSlot slot) {
        if (level < 1 || level > 4) throw new IllegalArgumentException("level must be from 1 to 4");
        return switch (slot) {
            case MAINHAND -> MAIN_HAND_CHANCE_PER_LEVEL * level;
            case OFFHAND -> OFF_HAND_CHANCE_PER_LEVEL * level;
            default -> 0.0;
        };
    }

    private static UUID uuid(String slot) {
        return UUID.nameUUIDFromBytes(("silenttinkers:pixie/" + slot).getBytes(StandardCharsets.UTF_8));
    }
}
