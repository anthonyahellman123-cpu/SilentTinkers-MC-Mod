package com.anthonyahellman.silenttinkers.client;

import com.anthonyahellman.silenttinkers.material.SourceVisualIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only appearance sampler for source items carried by SilentTinkers.
 *
 * <p>First asks Minecraft's real item-color handlers for tint layers, which
 * keeps dynamic mod items authoritative. If the item has no tint handler, it
 * samples the baked model's particle sprite so normal textured ingots still
 * produce a useful representative color. This intentionally reads the current
 * client resources instead of maintaining a hard-coded material color table.</p>
 */
public final class SourceVisualColorResolver {
    public static final int FALLBACK_ARGB = 0xFFB768FF;
    private static final int MAX_TINT_LAYERS = 8;
    private static final Map<SourceVisualIdentity, Integer> CACHE = new ConcurrentHashMap<>();

    private SourceVisualColorResolver() {}

    public static int resolve(SourceVisualIdentity identity) {
        return CACHE.computeIfAbsent(identity, SourceVisualColorResolver::sample);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static int sample(SourceVisualIdentity identity) {
        ItemStack source = identity.reconstruct();
        if (source.isEmpty()) return FALLBACK_ARGB;

        Minecraft minecraft = Minecraft.getInstance();
        int tintColor = sampleItemTints(minecraft.getItemColors(), source);
        if (tintColor != -1) return 0xFF000000 | tintColor;

        try {
            BakedModel model = minecraft.getItemRenderer().getModel(
                    source, minecraft.level, minecraft.player, 0);
            TextureAtlasSprite sprite = model.getParticleIcon();
            int sampled = sampleSprite(sprite);
            if (sampled != -1) return sampled;
        } catch (RuntimeException ignored) {
            // Dynamic/custom renderers may not expose a useful baked particle
            // sprite. Falling back is safer than breaking fluid rendering.
        }
        return FALLBACK_ARGB;
    }

    private static int sampleItemTints(ItemColors colors, ItemStack stack) {
        long red = 0;
        long green = 0;
        long blue = 0;
        int count = 0;
        for (int layer = 0; layer < MAX_TINT_LAYERS; layer++) {
            int color = colors.getColor(stack, layer);
            if (color == -1) continue;
            red += (color >> 16) & 0xFF;
            green += (color >> 8) & 0xFF;
            blue += color & 0xFF;
            count++;
        }
        if (count == 0) return -1;
        return ((int) (red / count) << 16)
                | ((int) (green / count) << 8)
                | (int) (blue / count);
    }

    private static int sampleSprite(TextureAtlasSprite sprite) {
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        if (width <= 0 || height <= 0) return -1;

        long red = 0;
        long green = 0;
        long blue = 0;
        long alphaWeight = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Forge's 1.20.x helper returns NativeImage's ABGR packed color.
                int abgr = sprite.getPixelRGBA(0, x, y);
                int alpha = (abgr >>> 24) & 0xFF;
                if (alpha < 16) continue;
                int r = abgr & 0xFF;
                int g = (abgr >>> 8) & 0xFF;
                int b = (abgr >>> 16) & 0xFF;
                red += (long) r * alpha;
                green += (long) g * alpha;
                blue += (long) b * alpha;
                alphaWeight += alpha;
            }
        }
        if (alphaWeight == 0) return -1;
        int r = (int) (red / alphaWeight);
        int g = (int) (green / alphaWeight);
        int b = (int) (blue / alphaWeight);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
