package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Draws the ability zones on each player's own game (no network traffic): the Cave Worm's toxic ring (radius 5,
 * green/yellow, tall) and the Angel's blessing ring (radius 10, gold/white, with sparkles). The on/off state of the
 * abilities is already sent to everyone nearby with the origin data.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class ZoneParticlesClient {
    private static final DustParticleOptions TOXIC_GREEN = new DustParticleOptions(new Vector3f(0.55F, 0.95F, 0.1F), 1.4F);
    private static final DustParticleOptions TOXIC_YELLOW = new DustParticleOptions(new Vector3f(0.9F, 0.95F, 0.15F), 1.4F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.3F), 1.3F);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.95F), 1.3F);
    private static final double VIEW_DISTANCE_SQR = 64 * 64;

    private ZoneParticlesClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused() || level.getGameTime() % 2 != 0) return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        RandomSource random = level.getRandom();
        for (AbstractClientPlayer player : level.players()) {
            if (player.isInvisible() || player.distanceToSqr(camera) > VIEW_DISTANCE_SQR) continue;
            if (BOPowers.active(player, BOPowers.CW_PARASITES)) caveRing(level, player, random);
            if (BOPowers.active(player, BOPowers.ANGEL_ALTRUIST)) angelRing(level, player, random);
        }
    }

    private static void caveRing(ClientLevel level, AbstractClientPlayer worm, RandomSource random) {
        double r = CaveWormHandler.RADIUS;
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double height = random.nextDouble() * 2.3; // taller than the Shulk's ring
            level.addParticle(random.nextBoolean() ? TOXIC_GREEN : TOXIC_YELLOW,
                    worm.getX() + Math.cos(angle) * r, worm.getY() + 0.1 + height, worm.getZ() + Math.sin(angle) * r, 0, 0.01, 0);
        }
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = Math.sqrt(random.nextDouble()) * r;
            level.addParticle(TOXIC_GREEN, worm.getX() + Math.cos(angle) * dist, worm.getY() + 0.2, worm.getZ() + Math.sin(angle) * dist, 0, 0.02, 0);
        }
    }

    private static void angelRing(ClientLevel level, AbstractClientPlayer angel, RandomSource random) {
        double r = AngelHandler.RADIUS;
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double height = random.nextBoolean() ? 0.15 : 0.9;
            level.addParticle(random.nextBoolean() ? GOLD : WHITE,
                    angel.getX() + Math.cos(angle) * r, angel.getY() + height + random.nextDouble() * 0.2, angel.getZ() + Math.sin(angle) * r, 0, 0, 0);
        }
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = Math.sqrt(random.nextDouble()) * r;
            level.addParticle(i == 0 ? ParticleTypes.END_ROD : GOLD, angel.getX() + Math.cos(angle) * dist,
                    angel.getY() + 0.2 + random.nextDouble() * 0.8, angel.getZ() + Math.sin(angle) * dist, 0, 0.005, 0);
        }
    }
}
