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
    public static final ResourceLocation RR_SPRINT_WHOOSH_ID = id("rr_sprint_whoosh");
    public static final ResourceLocation RR_SPRINT_WIND_ID = id("rr_sprint_wind");
    public static final ResourceLocation CW_PARASITES_ON_ID = id("cw_parasites_on");
    public static final ResourceLocation CW_PARASITES_LOOP_ID = id("cw_parasites_loop");
    public static final ResourceLocation CW_PARASITES_OFF_ID = id("cw_parasites_off");
    public static final ResourceLocation ANGEL_SELF_ON_ID = id("angel_self_on");
    public static final ResourceLocation ANGEL_SELF_LOOP_ID = id("angel_self_loop");
    public static final ResourceLocation ANGEL_SELF_OFF_ID = id("angel_self_off");
    public static final ResourceLocation ANGEL_OTHERS_ON_ID = id("angel_others_on");
    public static final ResourceLocation ANGEL_OTHERS_LOOP_ID = id("angel_others_loop");
    public static final ResourceLocation ANGEL_OTHERS_OFF_ID = id("angel_others_off");
    public static final ResourceLocation ANGEL_BOTH_LOOP_ID = id("angel_both_loop");
    public static final ResourceLocation BAT_TAKEOFF_ID = id("bat_takeoff");
    public static final ResourceLocation BAT_DASH_ID = id("bat_dash");
    public static final ResourceLocation BAT_ECHO_ON_ID = id("bat_echo_on");
    public static final ResourceLocation BAT_ECHO_LOOP_ID = id("bat_echo_loop");
    public static final ResourceLocation BAT_ECHO_OFF_ID = id("bat_echo_off");
    public static final ResourceLocation ELYTRIAN_LAUNCH_ID = id("elytrian_launch");
    public static final ResourceLocation WEB_SHOOT_ID = id("web_shoot");
    public static final ResourceLocation SHULK_FORCE_ID = id("shulk_force");
    public static final ResourceLocation POUNCE_DASH_ID = id("pounce_dash");
    public static final ResourceLocation FIREBALL_SPIT_ID = id("fireball_spit");

    public static final SoundEvent ADRENALINE_ACTIVATE = SoundEvent.createVariableRangeEvent(ADRENALINE_ACTIVATE_ID);
    public static final SoundEvent ADRENALINE_RUSH = SoundEvent.createVariableRangeEvent(ADRENALINE_RUSH_ID);
    public static final SoundEvent ADRENALINE_CRASH = SoundEvent.createVariableRangeEvent(ADRENALINE_CRASH_ID);
    public static final SoundEvent RR_SPRINT_WHOOSH = SoundEvent.createVariableRangeEvent(RR_SPRINT_WHOOSH_ID);
    public static final SoundEvent RR_SPRINT_WIND = SoundEvent.createVariableRangeEvent(RR_SPRINT_WIND_ID);
    public static final SoundEvent CW_PARASITES_ON = SoundEvent.createVariableRangeEvent(CW_PARASITES_ON_ID);
    public static final SoundEvent CW_PARASITES_LOOP = SoundEvent.createVariableRangeEvent(CW_PARASITES_LOOP_ID);
    public static final SoundEvent CW_PARASITES_OFF = SoundEvent.createVariableRangeEvent(CW_PARASITES_OFF_ID);
    public static final SoundEvent ANGEL_SELF_ON = SoundEvent.createVariableRangeEvent(ANGEL_SELF_ON_ID);
    public static final SoundEvent ANGEL_SELF_LOOP = SoundEvent.createVariableRangeEvent(ANGEL_SELF_LOOP_ID);
    public static final SoundEvent ANGEL_SELF_OFF = SoundEvent.createVariableRangeEvent(ANGEL_SELF_OFF_ID);
    public static final SoundEvent ANGEL_OTHERS_ON = SoundEvent.createVariableRangeEvent(ANGEL_OTHERS_ON_ID);
    public static final SoundEvent ANGEL_OTHERS_LOOP = SoundEvent.createVariableRangeEvent(ANGEL_OTHERS_LOOP_ID);
    public static final SoundEvent ANGEL_OTHERS_OFF = SoundEvent.createVariableRangeEvent(ANGEL_OTHERS_OFF_ID);
    public static final SoundEvent ANGEL_BOTH_LOOP = SoundEvent.createVariableRangeEvent(ANGEL_BOTH_LOOP_ID);
    public static final SoundEvent BAT_TAKEOFF = SoundEvent.createVariableRangeEvent(BAT_TAKEOFF_ID);
    public static final SoundEvent BAT_DASH = SoundEvent.createVariableRangeEvent(BAT_DASH_ID);
    public static final SoundEvent BAT_ECHO_ON = SoundEvent.createVariableRangeEvent(BAT_ECHO_ON_ID);
    public static final SoundEvent BAT_ECHO_LOOP = SoundEvent.createVariableRangeEvent(BAT_ECHO_LOOP_ID);
    public static final SoundEvent BAT_ECHO_OFF = SoundEvent.createVariableRangeEvent(BAT_ECHO_OFF_ID);
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
            helper.register(RR_SPRINT_WHOOSH_ID, RR_SPRINT_WHOOSH);
            helper.register(RR_SPRINT_WIND_ID, RR_SPRINT_WIND);
            helper.register(CW_PARASITES_ON_ID, CW_PARASITES_ON);
            helper.register(CW_PARASITES_LOOP_ID, CW_PARASITES_LOOP);
            helper.register(CW_PARASITES_OFF_ID, CW_PARASITES_OFF);
            helper.register(ANGEL_SELF_ON_ID, ANGEL_SELF_ON);
            helper.register(ANGEL_SELF_LOOP_ID, ANGEL_SELF_LOOP);
            helper.register(ANGEL_SELF_OFF_ID, ANGEL_SELF_OFF);
            helper.register(ANGEL_OTHERS_ON_ID, ANGEL_OTHERS_ON);
            helper.register(ANGEL_OTHERS_LOOP_ID, ANGEL_OTHERS_LOOP);
            helper.register(ANGEL_OTHERS_OFF_ID, ANGEL_OTHERS_OFF);
            helper.register(ANGEL_BOTH_LOOP_ID, ANGEL_BOTH_LOOP);
            helper.register(BAT_TAKEOFF_ID, BAT_TAKEOFF);
            helper.register(BAT_DASH_ID, BAT_DASH);
            helper.register(BAT_ECHO_ON_ID, BAT_ECHO_ON);
            helper.register(BAT_ECHO_LOOP_ID, BAT_ECHO_LOOP);
            helper.register(BAT_ECHO_OFF_ID, BAT_ECHO_OFF);
            helper.register(ELYTRIAN_LAUNCH_ID, ELYTRIAN_LAUNCH);
            helper.register(WEB_SHOOT_ID, WEB_SHOOT);
            helper.register(SHULK_FORCE_ID, SHULK_FORCE);
            helper.register(POUNCE_DASH_ID, POUNCE_DASH);
            helper.register(FIREBALL_SPIT_ID, FIREBALL_SPIT);
        });
    }
}
