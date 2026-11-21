package com.betterorigins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Better Origins sounds (assets/better_origins/sounds.json + sounds/*.ogg).
 * - adrenaline_activate: played by the server when Adrenaline triggers (heard by nearby players)
 * - adrenaline_rush:     30 s heartbeat, played only for the player during Adrenaline
 * - adrenaline_crash:    played only for the player when the crash starts
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BetterOriginsSounds {
    public static final ResourceLocation ADRENALINE_ACTIVATE_ID = id("adrenaline_activate");
    public static final ResourceLocation ADRENALINE_RUSH_ID = id("adrenaline_rush");
    public static final ResourceLocation ADRENALINE_CRASH_ID = id("adrenaline_crash");
    public static final ResourceLocation ELYTRIAN_LAUNCH_ID = id("elytrian_launch");
    public static final ResourceLocation WEB_SHOOT_ID = id("web_shoot");
    public static final ResourceLocation SHULK_FORCE_ID = id("shulk_force");
    public static final ResourceLocation POUNCE_DASH_ID = id("pounce_dash");
    public static final ResourceLocation FIREBALL_SPIT_ID = id("fireball_spit");

    public static final SoundEvent ADRENALINE_ACTIVATE = SoundEvent.createVariableRangeEvent(ADRENALINE_ACTIVATE_ID);
    public static final SoundEvent ADRENALINE_RUSH = SoundEvent.createVariableRangeEvent(ADRENALINE_RUSH_ID);
    public static final SoundEvent ADRENALINE_CRASH = SoundEvent.createVariableRangeEvent(ADRENALINE_CRASH_ID);
    public static final SoundEvent ELYTRIAN_LAUNCH = SoundEvent.createVariableRangeEvent(ELYTRIAN_LAUNCH_ID);
    public static final SoundEvent WEB_SHOOT = SoundEvent.createVariableRangeEvent(WEB_SHOOT_ID);
    public static final SoundEvent SHULK_FORCE = SoundEvent.createVariableRangeEvent(SHULK_FORCE_ID);
    public static final SoundEvent POUNCE_DASH = SoundEvent.createVariableRangeEvent(POUNCE_DASH_ID);
    public static final SoundEvent FIREBALL_SPIT = SoundEvent.createVariableRangeEvent(FIREBALL_SPIT_ID);

    private BetterOriginsSounds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, path);
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            helper.register(ADRENALINE_ACTIVATE_ID, ADRENALINE_ACTIVATE);
            helper.register(ADRENALINE_RUSH_ID, ADRENALINE_RUSH);
            helper.register(ADRENALINE_CRASH_ID, ADRENALINE_CRASH);
            helper.register(ELYTRIAN_LAUNCH_ID, ELYTRIAN_LAUNCH);
            helper.register(WEB_SHOOT_ID, WEB_SHOOT);
            helper.register(SHULK_FORCE_ID, SHULK_FORCE);
            helper.register(POUNCE_DASH_ID, POUNCE_DASH);
            helper.register(FIREBALL_SPIT_ID, FIREBALL_SPIT);
        });
    }
}
