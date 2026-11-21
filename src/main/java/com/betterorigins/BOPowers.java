package com.betterorigins;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.reference.PowerHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Small helpers to ask whether an entity has one of our powers, and whether it is switched on. */
public final class BOPowers {
    public static final ResourceLocation RR_DASH = id("rr_dash");
    public static final ResourceLocation RR_STUN = id("rr_stun");
    public static final ResourceLocation RR_IMPACT = id("rr_impact");
    public static final ResourceLocation CW_PARASITES = id("cw_parasites");
    public static final ResourceLocation CW_PROSPECTOR = id("cw_prospector");
    public static final ResourceLocation ANGEL_SELFISH = id("angel_selfish");
    public static final ResourceLocation ANGEL_ALTRUIST = id("angel_altruist");
    public static final ResourceLocation ANGEL_MENU = id("angel_blessing_menu");

    private BOPowers() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, path);
    }

    /** True if the entity has the power (switched on or not). */
    public static boolean has(Entity entity, ResourceLocation power) {
        return OriginDataHolder.optional(entity).map(holder -> {
            for (PowerHolder p : holder.getAllPowers())
                if (power.equals(p.id())) return true;
            return false;
        }).orElse(false);
    }

    /** True if the entity has the power and it is active (toggles: switched on). */
    public static boolean active(Entity entity, ResourceLocation power) {
        return OriginDataHolder.optional(entity).map(holder -> {
            for (PowerHolder p : holder.getAllPowers())
                if (power.equals(p.id())) return p.power().isActive(holder);
            return false;
        }).orElse(false);
    }
}
