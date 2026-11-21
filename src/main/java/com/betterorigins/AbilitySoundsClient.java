package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ability sounds played on each player's own game (no network traffic: the on/off state of the abilities is already
 * sent to everyone nearby with the origin data). Loops follow the player and fade in/out.
 * - Road Runner: a whoosh when starting to sprint with Speed Burst on, then a wind loop louder the faster you run.
 * - Cave Worm: Cave Parasites on / loop / off (heard by everyone nearby).
 * - Angel: Blessing Self on / off and its cold loop (the loop only for the Angel); Blessing Others on / off and its
 *   warm loop; with both on, an energising loop instead (heard by everyone nearby).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class AbilitySoundsClient {
    private static final double RANGE_SQR = 48 * 48;
    private static final float ONE_SHOT = 0.6F;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean fresh = true, running, parasites, self, others;
        FollowLoop runLoop, parasiteLoop, angelLoop;
    }

    private AbilitySoundsClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            STATES.values().forEach(AbilitySoundsClient::stopAll);
            STATES.clear();
            return;
        }
        if (minecraft.isPaused()) return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            if (!player.isAlive() || player.distanceToSqr(camera) > RANGE_SQR) continue;
            seen.add(player.getUUID());
            State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
            update(minecraft, player, state);
            state.fresh = false;
        }
        STATES.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            stopAll(entry.getValue());
            return true;
        });
    }

    private static void update(Minecraft minecraft, AbstractClientPlayer player, State state) {
        // ---- Road Runner ----
        boolean running = player.isSprinting() && !player.isSwimming() && !player.isFallFlying() && !player.isPassenger()
                && BOPowers.active(player, BOPowers.RR_DASH);
        if (running && !state.running && !state.fresh) play(minecraft, player, BetterOriginsSounds.RR_SPRINT_WHOOSH, ONE_SHOT);
        if (running) {
            double speed = Math.sqrt(Mth.square(player.getX() - player.xo) + Mth.square(player.getZ() - player.zo));
            float volume = 0.15F + 0.4F * Mth.clamp((float) ((speed - 0.25) / 0.75), 0F, 1F);
            state.runLoop = loop(minecraft, state.runLoop, player, BetterOriginsSounds.RR_SPRINT_WIND, volume);
        } else state.runLoop = fadeOut(state.runLoop);
        state.running = running;

        // ---- Cave Worm ----
        boolean parasites = BOPowers.active(player, BOPowers.CW_PARASITES);
        if (parasites != state.parasites && !state.fresh)
            play(minecraft, player, parasites ? BetterOriginsSounds.CW_PARASITES_ON : BetterOriginsSounds.CW_PARASITES_OFF, ONE_SHOT);
        state.parasiteLoop = parasites ? loop(minecraft, state.parasiteLoop, player, BetterOriginsSounds.CW_PARASITES_LOOP, 0.4F) : fadeOut(state.parasiteLoop);
        state.parasites = parasites;

        // ---- Angel ----
        boolean self = BOPowers.active(player, BOPowers.ANGEL_SELFISH);
        boolean others = BOPowers.active(player, BOPowers.ANGEL_ALTRUIST);
        if (!state.fresh) {
            if (self != state.self)
                play(minecraft, player, self ? BetterOriginsSounds.ANGEL_SELF_ON : BetterOriginsSounds.ANGEL_SELF_OFF, ONE_SHOT);
            if (others != state.others)
                play(minecraft, player, others ? BetterOriginsSounds.ANGEL_OTHERS_ON : BetterOriginsSounds.ANGEL_OTHERS_OFF, ONE_SHOT);
        }
        SoundEvent wanted = self && others ? BetterOriginsSounds.ANGEL_BOTH_LOOP
                : others ? BetterOriginsSounds.ANGEL_OTHERS_LOOP
                : self && player == minecraft.player ? BetterOriginsSounds.ANGEL_SELF_LOOP // the cold loop is personal
                : null;
        if (state.angelLoop != null && state.angelLoop.sound != wanted) state.angelLoop = fadeOut(state.angelLoop); // crossfade
        if (wanted != null) state.angelLoop = loop(minecraft, state.angelLoop, player, wanted, wanted == BetterOriginsSounds.ANGEL_SELF_LOOP ? 0.35F : 0.45F);
        state.self = self;
        state.others = others;
    }

    private static void play(Minecraft minecraft, Player player, SoundEvent sound, float volume) {
        minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS, volume, 1.0F, player, player.getRandom().nextLong()));
    }

    /** Keeps (or starts) a loop following the player at the given volume. */
    private static FollowLoop loop(Minecraft minecraft, FollowLoop current, Player player, SoundEvent sound, float volume) {
        if (current == null || current.isStopped() || current.fading) {
            current = new FollowLoop(sound, player);
            minecraft.getSoundManager().play(current);
        }
        current.target = volume;
        return current;
    }

    /** Lets a loop fade out by itself; returns null so the caller forgets it. */
    private static FollowLoop fadeOut(FollowLoop loop) {
        if (loop != null) loop.fading = true;
        return null;
    }

    private static void stopAll(State state) {
        fadeOut(state.runLoop);
        fadeOut(state.parasiteLoop);
        fadeOut(state.angelLoop);
    }

    /** A looping sound that follows a player, fading in when it starts and out when it is no longer wanted. */
    private static final class FollowLoop extends AbstractTickableSoundInstance {
        private static final float FADE_STEP = 0.05F; // about half a second for a full fade
        final SoundEvent sound;
        private final Player player;
        float target = 0.4F;
        boolean fading = false;

        FollowLoop(SoundEvent sound, Player player) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.sound = sound;
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.02F; // must start above 0 or Minecraft doesn't play it
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (this.player.isRemoved()) {
                this.stop();
                return;
            }
            this.x = this.player.getX();
            this.y = this.player.getY();
            this.z = this.player.getZ();
            float goal = this.fading ? 0F : this.target;
            this.volume += Mth.clamp(goal - this.volume, -FADE_STEP, FADE_STEP);
            if (this.fading && this.volume <= 0.01F) this.stop();
        }
    }
}
