package com.iafenvoy.origins.data.power.builtin.regular;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.Power;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Lets a player fly like in creative mode while the power is active.
 * Better Origins fixes:
 * - flying is really stopped when the power turns off (before, only "may fly" was removed and the
 *   player could keep flying), and the change is sent to the client;
 * - creative and spectator players are never touched;
 * - if the player was flying in the air when the power turned off, they glide down: 7 seconds of Slow Falling II
 *   (level II is the marker FlightGlideClient uses to make this glide a little faster than normal Slow Falling);
 * - "may fly" is restored every tick while active (for example after leaving creative mode).
 */
@SuppressWarnings("deprecation")
public class CreativeFlightPower extends Power {
    public static final MapCodec<CreativeFlightPower> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BaseSettings.CODEC.forGetter(Power::getSettings)
    ).apply(i, CreativeFlightPower::new));

    public CreativeFlightPower(BaseSettings settings) {
        super(settings);
    }

    @Override
    public @NotNull MapCodec<? extends Power> codec() {
        return CODEC;
    }

    private static boolean isManagedByGameMode(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    @Override
    public void active(@NotNull OriginDataHolder holder) {
        if (holder.getEntity() instanceof Player player && !isManagedByGameMode(player)) {
            player.getAbilities().mayfly = true;
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.onUpdateAbilities();
        }
    }

    @Override
    public void activeTick(OriginDataHolder holder) {
        if (holder.getEntity() instanceof ServerPlayer player && !isManagedByGameMode(player) && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void inactive(@NotNull OriginDataHolder holder) {
        if (holder.getEntity() instanceof Player player && !isManagedByGameMode(player)) {
            boolean wasFlyingInAir = player.getAbilities().flying && !player.onGround();
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
                if (wasFlyingInAir)
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 1, false, true, true));
            }
        }
    }
}
