package com.betterorigins;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.reference.PowerHolder;
import com.iafenvoy.origins.registry.OriginsAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

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
    public static final ResourceLocation BAT_FLIGHT = id("bat_flight");
    public static final ResourceLocation BAT_ECHO = id("bat_echo");

    private BOPowers() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, path);
    }

    /** Mobs, items, arrows... without origin data can't have powers: answer without building anything. */
    private static boolean mayHavePowers(Entity entity) {
        return entity != null && (entity instanceof Player || entity.hasData(OriginsAttachments.ENTITY_ORIGIN));
    }

    /** True if the entity has the power (switched on or not). */
    public static boolean has(Entity entity, ResourceLocation power) {
        if (!mayHavePowers(entity)) return false;
        return OriginDataHolder.optional(entity).map(holder -> {
            for (PowerHolder p : holder.getAllPowers())
                if (power.equals(p.id())) return true;
            return false;
        }).orElse(false);
    }

    /** True if the entity has the power and it is active (toggles: switched on). */
    public static boolean active(Entity entity, ResourceLocation power) {
        if (!mayHavePowers(entity)) return false;
        return OriginDataHolder.optional(entity).map(holder -> {
            for (PowerHolder p : holder.getAllPowers())
                if (power.equals(p.id())) return p.power().isActive(holder);
            return false;
        }).orElse(false);
    }
}
