package com.betterorigins;

import com.iafenvoy.origins.attachment.EntityOriginAttachment;
import com.iafenvoy.origins.data.power.component.PowerComponent;
import com.iafenvoy.origins.data.power.component.builtin.ResourceComponent;
import com.iafenvoy.origins.registry.OriginsAttachments;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.FireworkExplosion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client side of The Perfect Human's Adrenaline (sounds + visual effects).
 *
 * It reads the "better_origins:adrenaline_timer" resource that Origins already syncs to every client:
 *   0 = ready, 2400..1801 = Adrenaline (30 s), 1800..1 = crash (90 s).
 * - Timer jumps up to ~2400 on any nearby player -> black & red firework burst + smoke around them (everyone sees it).
 * - The local player: red fog at the screen edges for the length of the activation sound (2.4 s),
 *   the heartbeat loop during the 30 s, the crash sound when the crash starts.
 * The activation sound itself is played by the server (adrenaline power in the data), so nearby players hear it.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class AdrenalineClient {
    private static final ResourceLocation TIMER_POWER = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "adrenaline_timer");
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "textures/gui/adrenaline_vignette.png");
    private static final int ACTIVE_ABOVE = 1800;          // timer > 1800 = Adrenaline
    private static final float VIGNETTE_SECONDS = 2.4F;    // same length as adrenaline_activate.ogg
    private static final int RED = 0xB3312C;               // firework red (red dye)
    private static final int DARK_RED = 0x5A0E0E;
    private static final int BLACK = 0x1D1D21;             // firework black (black dye)

    private static final Map<UUID, Integer> LAST_VALUES = new HashMap<>();
    private static long vignetteStart = -1;
    private static RushSound rushSound;

    private AdrenalineClient() {
    }

    /** Adrenaline timer of a player, or -1 if the player doesn't have The Perfect Human's Adrenaline. */
    private static int timerOf(Player player) {
        EntityOriginAttachment data = player.getExistingData(OriginsAttachments.ENTITY_ORIGIN).orElse(null);
        if (data == null) return -1;
        Map<Class<? extends PowerComponent>, PowerComponent> components = data.getComponents().get(TIMER_POWER);
        if (components == null) return -1;
        return components.get(ResourceComponent.class) instanceof ResourceComponent resource ? resource.getValue() : -1;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        if (level == null || self == null) {
            LAST_VALUES.clear();
            stopRush();
            vignetteStart = -1;
            return;
        }
        Map<UUID, Integer> seen = new HashMap<>();
        for (AbstractClientPlayer player : level.players()) {
            int now = timerOf(player);
            if (now < 0) continue;
            seen.put(player.getUUID(), now);
            Integer before = LAST_VALUES.get(player.getUUID());
            if (before == null) continue; // first time we see this player: just remember the value
            boolean isSelf = player == self;
            if (now > 2300 && now > before + 200) {           // Adrenaline just triggered
                burst(level, player);
                if (isSelf) {
                    vignetteStart = System.nanoTime();
                    startRush(minecraft);
                }
            } else if (isSelf && before > ACTIVE_ABOVE && now <= ACTIVE_ABOVE && now > 0) {   // crash starts
                stopRush();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(BetterOriginsSounds.ADRENALINE_CRASH, 1.0F, 0.9F));
            }
        }
        LAST_VALUES.clear();
        LAST_VALUES.putAll(seen);
        // stop the heartbeat if Adrenaline is over for any reason (crash, death, origin changed, respawn)
        int mine = seen.getOrDefault(self.getUUID(), -1);
        if (rushSound != null && (mine <= ACTIVE_ABOVE || !self.isAlive())) stopRush();
    }

    // ---------- particles ----------
    private static void burst(ClientLevel level, Player player) {
        double x = player.getX(), y = player.getY() + 1.0, z = player.getZ();
        level.createFireworks(x, y, z, 0, 0, 0, List.of(
                new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(RED, BLACK), IntList.of(DARK_RED), false, false),
                new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(BLACK, RED), IntList.of(BLACK), true, false)));
        RandomSource random = level.getRandom();
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.05F), 1.6F);
        for (int i = 0; i < 40; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double up = random.nextDouble() * 0.3 - 0.05;
            double speed = 0.15 + random.nextDouble() * 0.2;
            double vx = Math.cos(angle) * speed, vz = Math.sin(angle) * speed;
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y - 0.5, z, vx, up, vz);
            level.addParticle(red, x + vx * 3, y - 0.5 + random.nextDouble(), z + vz * 3, 0, 0, 0);
        }
    }

    // ---------- heartbeat ----------
    private static void startRush(Minecraft minecraft) {
        stopRush();
        rushSound = new RushSound();
        minecraft.getSoundManager().play(rushSound);
    }

    private static void stopRush() {
        if (rushSound != null) rushSound.finish();
        rushSound = null;
    }

    private static final class RushSound extends AbstractTickableSoundInstance {
        private RushSound() {
            super(BetterOriginsSounds.ADRENALINE_RUSH, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.8F;
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
            // stopped from AdrenalineClient
        }
    }

    // ---------- red fog at the screen edges ----------
    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "adrenaline_vignette"), AdrenalineClient::renderVignette);
    }

    private static void renderVignette(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (vignetteStart < 0) return;
        float t = (System.nanoTime() - vignetteStart) / 1_000_000_000F;
        if (t >= VIGNETTE_SECONDS) {
            vignetteStart = -1;
            return;
        }
        float alpha;
        if (t < 0.15F) alpha = t / 0.15F;                                          // quick fade in
        else if (t > VIGNETTE_SECONDS - 0.8F) alpha = (VIGNETTE_SECONDS - t) / 0.8F; // slow fade out
        else alpha = 1F;
        alpha *= 0.85F + 0.15F * Mth.sin(t * 9F);                                  // light pulse
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1, 1, 1, Mth.clamp(alpha, 0F, 1F));
        graphics.blit(VIGNETTE, 0, 0, -90, 0, 0, width, height, width, height);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
