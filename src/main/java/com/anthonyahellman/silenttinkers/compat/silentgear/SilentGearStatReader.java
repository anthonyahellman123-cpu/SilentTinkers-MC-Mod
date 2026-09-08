package com.anthonyahellman.silenttinkers.compat.silentgear;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
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
    private static final String GRADE_KEY = "SGear_Grade";

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

            /*
             * Some Silent Gear material items reach Tinkers with the grade tag intact while
             * IMaterialInstance#getStat still reports the ungraded material values. Compare
             * against an explicitly ungraded copy before applying the documented grade bonus;
             * this both repairs that path and prevents double-applying a bonus when Silent Gear
             * has already evaluated it itself.
             */
            int gradeBonus = readGradeBonus(stack);
            if (gradeBonus > 0) {
                ItemStack ungradedStack = stack.copy();
                if (ungradedStack.hasTag()) {
                    ungradedStack.getOrCreateTag().remove(GRADE_KEY);
                }
                Object ungradedMaterial = gearApi.getMethod("getMaterial", ItemStack.class)
                        .invoke(null, ungradedStack);
                if (ungradedMaterial != null) {
                    durability = repairMissingGrade(durability,
                            readStat(ungradedMaterial, mainPart, getStat, statsClass, "DURABILITY"),
                            statsClass, "DURABILITY", gradeBonus);
                    miningSpeed = repairMissingGrade(miningSpeed,
                            readStat(ungradedMaterial, mainPart, getStat, statsClass, "HARVEST_SPEED"),
                            statsClass, "HARVEST_SPEED", gradeBonus);
                    meleeDamage = repairMissingGrade(meleeDamage,
                            readStat(ungradedMaterial, mainPart, getStat, statsClass, "MELEE_DAMAGE"),
                            statsClass, "MELEE_DAMAGE", gradeBonus);
                    attackSpeed = repairMissingGrade(attackSpeed,
                            readStat(ungradedMaterial, mainPart, getStat, statsClass, "ATTACK_SPEED"),
                            statsClass, "ATTACK_SPEED", gradeBonus);
                }
            }

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

    private static int readGradeBonus(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains(GRADE_KEY)) return 0;
        return switch (stack.getOrCreateTag().getString(GRADE_KEY).toUpperCase(Locale.ROOT)) {
            case "E" -> 1;
            case "D" -> 2;
            case "C" -> 3;
            case "B" -> 4;
            case "A" -> 5;
            case "S" -> 10;
            case "SS" -> 15;
            case "SSS" -> 25;
            case "MAX" -> 30;
            default -> 0;
        };
    }

    private static float repairMissingGrade(float evaluated, float ungraded, Class<?> statsClass,
                                            String fieldName, int bonusPercent)
            throws ReflectiveOperationException {
        Object stat = statsClass.getField(fieldName).get(null);
        Method affectedByGrades = stat.getClass().getMethod("isAffectedByGrades");
        boolean affected = Boolean.TRUE.equals(affectedByGrades.invoke(stat));
        if (!affected || Math.abs(evaluated - ungraded) > 0.0001f) return evaluated;
        return evaluated + Math.abs(evaluated) * bonusPercent / 100.0f;
    }
}
