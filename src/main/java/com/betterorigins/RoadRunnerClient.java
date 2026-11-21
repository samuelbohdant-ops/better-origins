package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Road Runner, client side.
 * - Kinetic crash: player movement is calculated here, so the crash is detected here (real speed before the hit,
 *   the side that hit, a wall at least 2 blocks tall touching the body) and sent to the server, which applies the damage.
 * - Stunned: movement keys, jump and camera are frozen.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class RoadRunnerClient {
    /** Horizontal movement of the previous tick (real displacement, not the friction-reduced velocity). */
    private static double lastDx = 0, lastDz = 0;
    private static boolean lastSprinting = false;
    private static final double MIN_IMPACT = 0.4; // blocks per tick; normal sprinting is about 0.28
    private static final double MAX_SPEED = 3.0;  // more than this in one tick is a teleport

    private RoadRunnerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        if (Math.abs(dx) > MAX_SPEED || Math.abs(dz) > MAX_SPEED) dx = dz = 0; // teleport / respawn, not running
        boolean running = !player.isPassenger() && !player.isInWater() && !player.isSwimming() && !player.isFallFlying()
                && !player.getAbilities().flying;
        if (running && player.horizontalCollision && lastSprinting && BOPowers.active(player, BOPowers.RR_DASH)) {
            double impact = 0;
            // X side: we were running fast along X and now the body touches a wall at least 2 blocks tall on that side
            if (Math.abs(lastDx) >= MIN_IMPACT && isTallWall(player, Direction.Axis.X, Math.signum(lastDx)))
                impact = Math.max(impact, Math.abs(lastDx));
            if (Math.abs(lastDz) >= MIN_IMPACT && isTallWall(player, Direction.Axis.Z, Math.signum(lastDz)))
                impact = Math.max(impact, Math.abs(lastDz));
            if (impact > 0) PacketDistributor.sendToServer(new BetterOriginsNetwork.KineticCrash((float) impact));
        }
        lastDx = dx;
        lastDz = dz;
        lastSprinting = player.isSprinting();
    }

    /**
     * Looks at every block column touching the side of the player's body that hit the wall. It is a crash if any
     * column has 2 solid blocks one above the other that touch the body: feet + head, or head + above the head.
     * (A single block at the feet is not a wall: with Speed Burst you just run up it.)
     */
    private static boolean isTallWall(LocalPlayer player, Direction.Axis axis, double sign) {
        Level level = player.level();
        AABB box = player.getBoundingBox();
        double face = axis == Direction.Axis.X ? (sign > 0 ? box.maxX + 0.05 : box.minX - 0.05) : (sign > 0 ? box.maxZ + 0.05 : box.minZ - 0.05);
        int faceBlock = (int) Math.floor(face);
        int feetY = (int) Math.floor(box.minY + 0.01);
        int from = (int) Math.floor((axis == Direction.Axis.X ? box.minZ : box.minX) + 0.01);
        int to = (int) Math.floor((axis == Direction.Axis.X ? box.maxZ : box.maxX) - 0.01);
        for (int across = from; across <= to; across++) {
            boolean[] solid = new boolean[3];
            for (int up = 0; up < 3; up++) {
                BlockPos pos = axis == Direction.Axis.X ? new BlockPos(faceBlock, feetY + up, across) : new BlockPos(across, feetY + up, faceBlock);
                solid[up] = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            }
            if ((solid[0] && solid[1]) || (solid[1] && solid[2])) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!RoadRunnerHandler.isStunned(event.getEntity())) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && RoadRunnerHandler.isStunned(player)) {
            // sensitivity -1/3 makes Minecraft's mouse formula give exactly zero rotation
            event.setMouseSensitivity(-1.0 / 3.0);
            event.setCinematicCameraEnabled(false);
        }
    }
}
