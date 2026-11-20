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

    public static final SoundEvent ADRENALINE_ACTIVATE = SoundEvent.createVariableRangeEvent(ADRENALINE_ACTIVATE_ID);
    public static final SoundEvent ADRENALINE_RUSH = SoundEvent.createVariableRangeEvent(ADRENALINE_RUSH_ID);
    public static final SoundEvent ADRENALINE_CRASH = SoundEvent.createVariableRangeEvent(ADRENALINE_CRASH_ID);

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
        });
    }
}
