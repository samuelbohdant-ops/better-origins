package com.betterorigins;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.reference.PowerHolder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Superior Bat flight effects (server side, so every player nearby hears and sees them):
 * - taking off: soft gust of air with light wing flaps;
 * - starting to sprint while flying: quick whoosh plus the same short white trail as the Feline's leap
 *   (first 7 ticks only).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BatFlightEffects {
    private static final ResourceLocation FLIGHT_POWER = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "bat_flight");
    private static final int TRAIL_TICKS = 7;
    private static final float VOLUME = 0.45F;
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.2F);
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean flying, dashing;
        int trail;
    }

    private BatFlightEffects() {
    }

    private static boolean isBat(Player player) {
        return OriginDataHolder.optional(player).map(holder -> {
            for (PowerHolder power : holder.getAllPowers())
                if (FLIGHT_POWER.equals(power.id())) return true;
            return false;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator() || !isBat(player)) {
            STATES.remove(player.getUUID());
            return;
        }
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        boolean flying = player.getAbilities().flying;
        boolean dashing = flying && player.isSprinting();
        if (flying && !state.flying) play(level, player, BetterOriginsSounds.BAT_TAKEOFF);
        if (dashing && !state.dashing) {
            play(level, player, BetterOriginsSounds.BAT_DASH);
            state.trail = TRAIL_TICKS;
        }
        if (state.trail > 0) {
            state.trail--;
            double y = player.getY() + player.getBbHeight() * 0.5;
            level.sendParticles(WHITE_DUST, player.getX(), y, player.getZ(), 10, 0.3, 0.6, 0.3, 0.0);
            level.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), y, player.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        }
        state.flying = flying;
        state.dashing = dashing;
    }

    private static void play(ServerLevel level, Player player, SoundEvent sound) {
        level.playSound(null, player, sound, SoundSource.PLAYERS, VOLUME, 0.95F + level.random.nextFloat() * 0.1F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
