package com.betterorigins;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.reference.PowerHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
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
 * Superior Bat's Echolocation, client side.
 * - While it is on, mobs within 64 blocks and players within 32 blocks get a glowing outline seen through
 *   walls, only on the Bat's own screen (MinecraftGlowMixin -> shouldGlow).
 * - A Bat with Echolocation on is itself outlined in white for everyone who sees it (vanilla outline;
 *   plus a soft white end-rod aura drawn around them on every client, which also works with shader packs).
 * - Sounds: switch-on and switch-off are heard by everyone nearby (attached to the Bat); the steady
 *   "static light" hum while it is on is only heard by the Bat.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class BatEchoClient {
    private static final ResourceLocation ECHO_POWER = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "bat_echo");
    private static final double MOB_RANGE = 64, PLAYER_RANGE = 32;
    private static final Map<UUID, Boolean> LAST_STATE = new HashMap<>();
    private static final Set<Integer> ECHOING_PLAYERS = new HashSet<>();   // entity ids of Bats with Echolocation on (this tick)
    private static boolean selfActive = false;
    private static HumSound hum;

    private BatEchoClient() {
    }

    private static boolean echoActive(Player player) {
        return OriginDataHolder.optional(player).map(holder -> {
            for (PowerHolder power : holder.getAllPowers())
                if (ECHO_POWER.equals(power.id())) return power.power().isActive(holder);
            return false;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        ECHOING_PLAYERS.clear();
        if (level == null || self == null) {
            LAST_STATE.clear();
            selfActive = false;
            stopHum();
            return;
        }
        Map<UUID, Boolean> seen = new HashMap<>();
        for (AbstractClientPlayer player : level.players()) {
            boolean active = echoActive(player);
            Boolean before = LAST_STATE.get(player.getUUID());
            seen.put(player.getUUID(), active);
            if (active) {
                ECHOING_PLAYERS.add(player.getId());
                aura(level, player);
            }
            if (before == null || before == active) continue;
            play(minecraft, active ? BetterOriginsSounds.BAT_ECHO_ON : BetterOriginsSounds.BAT_ECHO_OFF, player, active ? 1.0F : 0.9F);
        }
        LAST_STATE.clear();
        LAST_STATE.putAll(seen);
        selfActive = ECHOING_PLAYERS.contains(self.getId());
        if (selfActive && (hum == null || hum.isStopped()) && self.isAlive()) {
            hum = new HumSound(self);
            minecraft.getSoundManager().play(hum);
        } else if (!selfActive) stopHum();
    }

    /** Soft white glowing aura: end-rod sparks appearing around the body and slowly fading (works with shader packs). */
    private static void aura(ClientLevel level, Player player) {
        if (player.isInvisible()) return;
        RandomSource random = level.getRandom();
        Minecraft minecraft = Minecraft.getInstance();
        // in first person keep the sparks below the eyes so they don't cover the view
        boolean ownFirstPerson = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
        double height = ownFirstPerson ? player.getBbHeight() * 0.55 : player.getBbHeight();
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.35 + random.nextDouble() * 0.35;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + random.nextDouble() * height;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.008, 0);
        }
    }

    private static void play(Minecraft minecraft, SoundEvent sound, Player player, float volume) {
        minecraft.getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS, volume, 1.0F, player, player.getRandom().nextLong()));
    }

    private static void stopHum() {
        if (hum != null) hum.finish();
        hum = null;
    }

    /** Used by MinecraftGlowMixin. */
    public static boolean shouldGlow(Entity entity) {
        if (entity instanceof Player && ECHOING_PLAYERS.contains(entity.getId())) return true; // the Bat itself glows white
        LocalPlayer self = Minecraft.getInstance().player;
        if (!selfActive || self == null || entity == self || !(entity instanceof LivingEntity) || entity instanceof ArmorStand || !entity.isAlive())
            return false;
        double range = entity instanceof Player ? PLAYER_RANGE : MOB_RANGE;
        return entity.distanceToSqr(self) <= range * range;
    }

    /** Quiet steady hum while Echolocation is on (only for the Bat). */
    private static final class HumSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        private HumSound(LocalPlayer player) {
            super(BetterOriginsSounds.BAT_ECHO_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.35F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        void finish() {
            this.stop();
        }

        @Override
        public void tick() {
            if (!this.player.isAlive() || this.player.isRemoved()) this.stop();
        }
    }
}
