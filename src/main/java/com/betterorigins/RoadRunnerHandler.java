package com.betterorigins;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Road Runner, server side.
 * - Stun: a melee hit while sprinting with Speed Burst on stuns the target for 3 s (frozen + Blindness). After a stun
 *   the target can't be stunned again for 1.5 s (mobs) or 5 s (players). The Ender Dragon is immune.
 * - While stunned: players can't attack, use or interact (their movement keys and camera are frozen in
 *   RoadRunnerClient); mobs have their AI switched off (no moving, turning, attacking or shooting, creepers stop
 *   swelling) and get it back when the stun ends.
 * - Kinetic crash: see crash().
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class RoadRunnerHandler {
    public static final int STUN_TICKS = 60;
    private static final int MOB_IMMUNITY_TICKS = 30, PLAYER_IMMUNITY_TICKS = 100;
    private static final String IMMUNE_UNTIL = "better_origins_stun_immune_until";
    /** Entity tag on mobs whose AI we switched off for a stun (saved with the mob, so it is restored even after a reload). */
    private static final String FROZEN_TAG = "better_origins_stun_frozen";
    private static final Set<Mob> FROZEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final String LAST_CRASH = "better_origins_last_crash";

    private RoadRunnerHandler() {
    }

    private static Holder<MobEffect> stunned() {
        return BetterOriginsEffects.holder(BetterOriginsEffects.STUNNED);
    }

    public static boolean isStunned(Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(stunned());
    }

    // ---------- applying the stun ----------
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (attacker.level().isClientSide()) return;
        if (isStunned(attacker)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive() || target instanceof EnderDragon) return;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        if (!attacker.isSprinting() || !BOPowers.active(attacker, BOPowers.RR_DASH)) return;
        long now = target.level().getGameTime();
        CompoundTag data = target.getPersistentData();
        if (isStunned(target) || now < data.getLong(IMMUNE_UNTIL)) return;
        target.addEffect(new MobEffectInstance(stunned(), STUN_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUN_TICKS, 0, false, false, true));
        if (target instanceof Mob mob) freeze(mob);
        data.putLong(IMMUNE_UNTIL, now + STUN_TICKS + (target instanceof Player ? PLAYER_IMMUNITY_TICKS : MOB_IMMUNITY_TICKS));
    }

    // ---------- what a stunned entity can't do ----------
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (isStunned(event.getSource().getEntity()) && event.getSource().getEntity() == event.getSource().getDirectEntity())
            event.setCanceled(true); // a stunned mob or player can't hit anyone in melee
    }

    @SubscribeEvent
    public static void onProjectile(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() && event.getEntity() instanceof Projectile projectile && isStunned(projectile.getOwner()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (isStunned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isStunned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isStunned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isStunned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (isStunned(event.getEntity())) event.setCanceled(true);
    }

    /** Mobs: switch their AI off for the stun (no moving, turning, attacking), restored when it ends. */
    private static void freeze(Mob mob) {
        if (mob.isNoAi()) return; // already without AI (for example set by a map maker): leave it alone
        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.setNoAi(true);
        mob.addTag(FROZEN_TAG);
        FROZEN.add(mob);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        FROZEN.removeIf(mob -> {
            if (mob.isRemoved()) return true; // unloaded or dead: the tag is restored on next load
            if (mob instanceof Creeper creeper) creeper.setSwellDir(-1); // a frozen creeper must not keep swelling
            if (isStunned(mob)) return false;
            mob.setNoAi(false);
            mob.removeTag(FROZEN_TAG);
            return true;
        });
    }

    /** A mob saved while frozen gets its AI back when it is loaded again. */
    @SubscribeEvent
    public static void onMobLoaded(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && mob.getTags().contains(FROZEN_TAG))
            FROZEN.add(mob);
    }

    // ---------- kinetic crash ----------
    /**
     * Called when a Road Runner's game reports hitting a wall (2+ blocks tall) while sprinting with Speed Burst on.
     * Damage = (speed in blocks per tick - 0.3) x 16, like an elytra crash but harder.
     */
    public static void crash(ServerPlayer player, float speed) {
        if (!BOPowers.has(player, BOPowers.RR_IMPACT) || !BOPowers.active(player, BOPowers.RR_DASH)) return;
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLong(LAST_CRASH) < 10) return; // at most one crash every 0.5 s
        player.getPersistentData().putLong(LAST_CRASH, now);
        // speed = blocks per tick when hitting the wall; full Speed Burst sprint is about 1.0 -> about 11 damage
        float damage = (Mth.clamp(speed, 0F, 2F) - 0.3F) * 16F;
        if (damage > 0) player.hurt(player.damageSources().flyIntoWall(), damage);
    }
}
