package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;

/**
 * Conservative first-pass policy for physical items that may be treated as one
 * ingot-equivalent by the tagged-fluid dynamic bridge.
 *
 * <p>Blocks, nuggets, planks, panes and arbitrary component items can represent
 * wildly different material amounts. Until SilentTinkers has tag-driven unit
 * conversion for those forms, only obvious ingot/gem/crystal unit items are
 * eligible for automatic melting. Discovery/evaluation still records every
 * other material; this gate affects runtime mutation only.</p>
 */
public final class DynamicBridgeItemPolicy {
    private DynamicBridgeItemPolicy() {}

    public static boolean isOneUnitMaterial(ResourceLocation itemId) {
        String path = itemId.getPath();
        return token(path, "ingot") || token(path, "gem") || token(path, "crystal");
    }

    private static boolean token(String path, String token) {
        return path.equals(token)
                || path.startsWith(token + "_")
                || path.endsWith("_" + token)
                || path.contains("_" + token + "_");
    }
}
