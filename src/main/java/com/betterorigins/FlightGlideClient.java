package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Glide after a flight ends in the air (Superior Bat): the flight power gives Slow Falling II for 7 s.
 * Vanilla Slow Falling ignores the level, so level II is used as a marker: with it you fall
 * a little faster than normal Slow Falling (gravity 0.015 instead of 0.01). Player movement is
 * calculated on the player's own client, so this runs there.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class FlightGlideClient {
    private static final double EXTRA_GRAVITY = 0.005;

    private FlightGlideClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        MobEffectInstance effect = player.getEffect(MobEffects.SLOW_FALLING);
        if (effect == null || effect.getAmplifier() != 1) return;
        if (player.onGround() || player.getAbilities().flying || player.isInWater() || player.isFallFlying()) return;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < 0) player.setDeltaMovement(motion.x, motion.y - EXTRA_GRAVITY, motion.z);
    }
}
