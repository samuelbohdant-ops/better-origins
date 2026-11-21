package com.betterorigins;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Cave Worm, server side.
 * - Prospector: every block the Cave Worm breaks drops loot as if the tool had one more level of Fortune
 *   (no Fortune -> Fortune I, Fortune III -> Fortune IV).
 * - Cave Parasites: while on, every mob, player and pet within 5 blocks (not the Cave Worm's own pets, not other
 *   Cave Worms) builds up parasites: 12 s in the zone = level 10. Outside the zone it drops 1 level every 0.5 s.
 *   Effects by level: 1-3 parasite damage (like Poison I) + Slowness I; 4-6 like Poison II + Slowness II +
 *   Darkness; 7-9 like Poison III + Slowness III + Darkness; 10 like Poison IV + Slowness IV + Darkness. The damage
 *   works on every mob (also undead and spiders, which are immune to real Poison); below level 6 it stops at half a
 *   heart, from level 6 it can kill. Infected entities give off small green particles.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class CaveWormHandler {
    public static final float MAX_PARASITES = 240F;           // 12 s x 20 ticks = level 10
    private static final float PER_LEVEL = MAX_PARASITES / 10F;
    private static final float DECAY_PER_TICK = PER_LEVEL / 10F; // 1 level every 0.5 s
    public static final double RADIUS = 5.0; // the zone ring is drawn by ZoneParticlesClient
    private static final DustParticleOptions INFECTED_DUST = new DustParticleOptions(new Vector3f(0.45F, 0.8F, 0.1F), 0.9F);
    private static final Set<LivingEntity> INFECTED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ThreadLocal<Player> MINER = new ThreadLocal<>();

    private CaveWormHandler() {
    }

    public static int level(float parasites) {
        return parasites <= 0 ? 0 : Math.min(10, (int) Math.ceil(parasites / PER_LEVEL));
    }

    // ---------- Prospector (Fortune +1) ----------
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        MINER.set(player != null && !player.level().isClientSide() && BOPowers.has(player, BOPowers.CW_PROSPECTOR) ? player : null);
    }

    /** If another mod (claims, protection) cancels the break, no drops will come: close the Fortune window. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBreakFinal(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) MINER.remove();
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        MINER.remove(); // drops of this block are done
    }

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        MINER.remove();
    }

    @SubscribeEvent
    public static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (MINER.get() == null || !event.isTargetting(Enchantments.FORTUNE)) return;
        var fortune = event.getTargetEnchant();
        if (fortune == null) return;
        event.getEnchantments().set(fortune, event.getEnchantments().getLevel(fortune) + 1);
    }

    // ---------- Cave Parasites ----------
    /** Who a Cave Worm's zone does not affect: itself, its own pets, other Cave Worms, creative/spectator players. */
    private static boolean excluded(LivingEntity target, ServerPlayer worm) {
        if (target == worm || !target.isAlive() || target instanceof ArmorStand) return true;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return true;
        if (target instanceof OwnableEntity pet && worm.getUUID().equals(pet.getOwnerUUID())) return true;
        return BOPowers.has(target, BOPowers.CW_PARASITES);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Set<LivingEntity> inZone = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerPlayer worm : level.players()) {
            if (!worm.isAlive() || !BOPowers.active(worm, BOPowers.CW_PARASITES)) continue;
            AABB box = worm.getBoundingBox().inflate(RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box))
                if (target.distanceToSqr(worm) <= RADIUS * RADIUS && !excluded(target, worm)) inZone.add(target);
        }
        // build-up inside a zone (once per tick, even inside two zones), decay outside
        for (LivingEntity target : inZone) {
            set(target, Math.min(MAX_PARASITES, target.getData(BetterOriginsAttachments.PARASITES) + 1F));
            INFECTED.add(target);
        }
        long time = level.getGameTime();
        INFECTED.removeIf(target -> {
            if (target.isRemoved() || !target.isAlive()) return true;
            if (target.level() != level) return false; // handled by its own level's tick
            float value = target.getData(BetterOriginsAttachments.PARASITES);
            if (!inZone.contains(target)) {
                value = Math.max(0F, value - DECAY_PER_TICK);
                set(target, value);
            }
            if (value <= 0F) return true;
            applyEffects(target, level(value), time);
            return false;
        });
    }

    /** Entities that come back (player logging in, chunk loading) with parasites keep losing them. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living
                && living.hasData(BetterOriginsAttachments.PARASITES) && living.getData(BetterOriginsAttachments.PARASITES) > 0)
            INFECTED.add(living);
    }

    private static void set(LivingEntity target, float value) {
        float old = target.getData(BetterOriginsAttachments.PARASITES);
        if (old == value) return;
        target.setData(BetterOriginsAttachments.PARASITES, value);
        if (target instanceof ServerPlayer && level(old) != level(value))
            target.syncData(BetterOriginsAttachments.PARASITES); // the HUD only shows whole levels
    }

    /**
     * The parasites hurt by themselves (Poison does nothing to undead mobs and spiders), ignoring armour like poison:
     * 1.5 damage every 20 ticks at level 1-3, then 2 / 3.5 / 5 damage every 10 ticks. Below level 6 they stop at half a heart; from level 6 they can kill.
     */
    private static void applyEffects(LivingEntity target, int level, long time) {
        if (level <= 0) return;
        int tier = level <= 3 ? 0 : level <= 6 ? 1 : level <= 9 ? 2 : 3;
        if (time % 10 == 0) {
            BetterOriginsEffects.refresh(target, MobEffects.MOVEMENT_SLOWDOWN, tier, 40, 20, true);
            if (level >= 4) BetterOriginsEffects.refresh(target, MobEffects.DARKNESS, 0, 60, 30, true);
            if (target.level() instanceof ServerLevel level1 && time % 20 == 0)
                level1.sendParticles(INFECTED_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        3 + tier * 2, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0);
        }
        // hits closer than 10 ticks would be blocked by the damage cooldown, so the top tiers hit harder instead
        int interval = tier == 0 ? 20 : 10;
        float damage = tier == 0 ? 1.5F : tier == 1 ? 2.0F : tier == 2 ? 3.5F : 5.0F;
        if (time % interval != 0) return;
        if (level < 6) damage = Math.min(damage, target.getHealth() - 1.0F); // not deadly yet: stop at half a heart
        if (damage > 0) target.hurt(target.damageSources().magic(), damage);
    }
}
