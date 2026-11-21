package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Coloured backgrounds of origins in the choose-origin screen.
 * For an origin "namespace:path" it looks for:
 *   better_origins:textures/gui/origin_style/namespace/path_slot.png   (26x104: normal, selected, hovered, selected+hovered)
 *   better_origins:textures/gui/origin_style/namespace/path_plate.png  (150x26: strip behind the origin name)
 * If a texture doesn't exist (for example an addon origin) the normal Origins look is used.
 * Resource packs can add or replace these textures.
 */
@OnlyIn(Dist.CLIENT)
public final class OriginStyles {
    public static final int SLOT_SIZE = 26, SLOT_TEXTURE_HEIGHT = 104, PLATE_WIDTH = 150, PLATE_HEIGHT = 26;
    private static final Map<ResourceLocation, Optional<ResourceLocation>> CACHE = new HashMap<>();

    private OriginStyles() {
    }

    public static Optional<ResourceLocation> slot(ResourceLocation origin) {
        return find(origin, "_slot");
    }

    public static Optional<ResourceLocation> plate(ResourceLocation origin) {
        return find(origin, "_plate");
    }

    /** Forget cached lookups (called when the screen opens, so resource pack changes are picked up). */
    public static void clearCache() {
        CACHE.clear();
    }

    private static Optional<ResourceLocation> find(ResourceLocation origin, String suffix) {
        if (origin == null) return Optional.empty();
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS,
                "textures/gui/origin_style/" + origin.getNamespace() + "/" + origin.getPath() + suffix + ".png");
        return CACHE.computeIfAbsent(texture, t ->
                Minecraft.getInstance().getResourceManager().getResource(t).isPresent() ? Optional.of(t) : Optional.empty());
    }
}
