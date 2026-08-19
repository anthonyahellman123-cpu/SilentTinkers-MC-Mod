package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Calls Silent Gear's public material API reflectively. This keeps the addon
 * build independent while still letting Silent Gear and its addons perform the
 * authoritative ratio, grade, and starcharge calculations at runtime.
 */
public final class SilentGearStatReader {
    private static final String GEAR_API = "net.silentchaos512.gear.api.GearApi";
    private static final String MATERIAL_INSTANCE = "net.silentchaos512.gear.api.material.IMaterialInstance";
    private static final String PART_TYPE = "net.silentchaos512.gear.api.part.PartType";
    private static final String ITEM_STAT = "net.silentchaos512.gear.api.stats.IItemStat";
    private static final String ITEM_STATS = "net.silentchaos512.gear.api.stats.ItemStats";

    private SilentGearStatReader() {}

    public static Optional<AlloyStatSnapshot> read(ItemStack stack) {
        try {
            Class<?> gearApi = Class.forName(GEAR_API);
            Object material = gearApi.getMethod("getMaterial", ItemStack.class).invoke(null, stack);
            if (material == null) {
                return Optional.empty();
            }

            Class<?> materialType = Class.forName(MATERIAL_INSTANCE);
            Class<?> partTypeClass = Class.forName(PART_TYPE);
            Class<?> statClass = Class.forName(ITEM_STAT);
            Class<?> statsClass = Class.forName(ITEM_STATS);
            Object mainPart = partTypeClass.getField("MAIN").get(null);
            Method getStat = materialType.getMethod("getStat", partTypeClass, statClass);

            float durability = readStat(material, mainPart, getStat, statsClass, "DURABILITY");
            float miningSpeed = readStat(material, mainPart, getStat, statsClass, "HARVEST_SPEED");
            float meleeDamage = readStat(material, mainPart, getStat, statsClass, "MELEE_DAMAGE");
            float attackSpeed = readStat(material, mainPart, getStat, statsClass, "ATTACK_SPEED");

            Tier tier = (Tier) materialType.getMethod("getHarvestTier").invoke(material);
            ResourceLocation tierId = TierSortingRegistry.getName(tier);
            if (tierId == null) {
                return Optional.empty();
            }
            return Optional.of(new AlloyStatSnapshot(
                    durability, miningSpeed, meleeDamage, attackSpeed, tierId));
        } catch (ReflectiveOperationException | LinkageError | ClassCastException | IllegalArgumentException exception) {
            SilentTinkersMod.LOGGER.warn("Could not evaluate Silent Gear alloy stats", exception);
            return Optional.empty();
        }
    }

    private static float readStat(
            Object material, Object mainPart, Method getStat, Class<?> statsClass, String fieldName)
            throws ReflectiveOperationException {
        Field field = statsClass.getField(fieldName);
        Object stat = field.get(null);
        Object value = getStat.invoke(material, mainPart, stat);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Silent Gear stat " + fieldName + " was not numeric");
        }
        return number.floatValue();
    }
}
