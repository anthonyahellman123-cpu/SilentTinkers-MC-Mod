package com.anthonyahellman.silenttinkers.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Optional;

/**
 * Captures the item identity that existed before SilentTinkers converts it into
 * molten composite alloy. This is intentionally visual/source metadata rather
 * than a hard-coded color: client rendering can later ask the original item how
 * it should look and carry that appearance into the new frame.
 *
 * <p>The original item ID is always preserved. A bounded copy of its item tag is
 * kept when practical so dynamic items such as Silent Gear alloys can retain the
 * NBT that may influence their rendered appearance. If the tag is unexpectedly
 * huge we keep the item ID but omit the tag instead of bloating every fluid and
 * tool-part payload.</p>
 */
public record SourceVisualIdentity(ResourceLocation itemId, Optional<CompoundTag> itemTag) {
    private static final String ITEM_KEY = "Item";
    private static final String TAG_KEY = "Tag";
    private static final String SILENT_GEAR_GRADE_KEY = "SGear_Grade";
    private static final int MAX_TAG_TEXT_LENGTH = 32_768;

    public SourceVisualIdentity {
        Objects.requireNonNull(itemId, "itemId");
        itemTag = Objects.requireNonNull(itemTag, "itemTag").map(CompoundTag::copy);
    }

    @Override
    public Optional<CompoundTag> itemTag() {
        return itemTag.map(CompoundTag::copy);
    }

    public static Optional<SourceVisualIdentity> capture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return Optional.empty();

        CompoundTag sourceTag = stack.getTag();
        Optional<CompoundTag> boundedTag = Optional.empty();
        if (sourceTag != null) {
            CompoundTag copy = sourceTag.copy();
            if (copy.toString().length() <= MAX_TAG_TEXT_LENGTH) {
                boundedTag = Optional.of(copy);
            }
        }
        return Optional.of(new SourceVisualIdentity(itemId, boundedTag));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ITEM_KEY, itemId.toString());
        itemTag.ifPresent(value -> tag.put(TAG_KEY, value.copy()));
        return tag;
    }

    /** Returns the native Silent Gear grade carried by the source stack. */
    public Optional<String> silentGearGrade() {
        return itemTag.filter(tag -> tag.contains(SILENT_GEAR_GRADE_KEY, Tag.TAG_STRING))
                .map(tag -> tag.getString(SILENT_GEAR_GRADE_KEY))
                .filter(value -> !value.isBlank() && !"NONE".equalsIgnoreCase(value));
    }

    public static Optional<SourceVisualIdentity> load(CompoundTag tag) {
        if (tag == null || !tag.contains(ITEM_KEY, Tag.TAG_STRING)) return Optional.empty();
        ResourceLocation itemId = ResourceLocation.tryParse(tag.getString(ITEM_KEY));
        if (itemId == null) return Optional.empty();
        Optional<CompoundTag> itemTag = tag.contains(TAG_KEY, Tag.TAG_COMPOUND)
                ? Optional.of(tag.getCompound(TAG_KEY).copy())
                : Optional.empty();
        return Optional.of(new SourceVisualIdentity(itemId, itemTag));
    }

    /** Rebuilds a one-count source stack for future client tint/model sampling. */
    public ItemStack reconstruct() {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR && !itemId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        itemTag.ifPresent(value -> stack.setTag(value.copy()));
        return stack;
    }
}
