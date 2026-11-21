package com.betterorigins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Angel, server side.
 * - Selfish mode: the Angel gets Swiftness (+15% speed), Zeal (+20% attack speed), Might (+15% damage),
 *   Resistance I and Regeneration I.
 * - Altruistic mode: the 3 blessings chosen in the menu go to every player (Angel included) and every pet within
 *   10 blocks. The zone (gold and white ring with sparkles) is drawn on each player's game by ZoneParticlesClient.
 * - In either mode the blocks around the Angel light up (3x3, light level 10 = about 70% of a torch) with invisible
 *   light blocks that are removed when it ends.
 * - The bar, the 2x drain with both modes on and the 30 s cooldown are in the power data (angel_tick).
 * - Feather (the Angel's slow fall given to others): no fall damage.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class AngelHandler {
    public enum Blessing {
        SWIFTNESS(() -> BetterOriginsEffects.holder(BetterOriginsEffects.ANGEL_SWIFTNESS), 0),
        ZEAL(() -> BetterOriginsEffects.holder(BetterOriginsEffects.ANGEL_ZEAL), 0),
        MIGHT(() -> BetterOriginsEffects.holder(BetterOriginsEffects.ANGEL_MIGHT), 0),
        RESISTANCE(() -> MobEffects.DAMAGE_RESISTANCE, 0),
        REGENERATION(() -> MobEffects.REGENERATION, 0),
        FEATHER(() -> BetterOriginsEffects.holder(BetterOriginsEffects.ANGEL_FEATHER), 0);

        public final Supplier<Holder<MobEffect>> effect;
        public final int amplifier;

        Blessing(Supplier<Holder<MobEffect>> effect, int amplifier) {
            this.effect = effect;
            this.amplifier = amplifier;
        }

        public int bit() {
            return 1 << this.ordinal();
        }
    }

    public static final int MAX_CHOSEN = 3;
    /** Default choice: Swiftness, Resistance, Regeneration. */
    public static final int DEFAULT_BLESSINGS = Blessing.SWIFTNESS.bit() | Blessing.RESISTANCE.bit() | Blessing.REGENERATION.bit();
    private static final Blessing[] SELFISH = {Blessing.SWIFTNESS, Blessing.ZEAL, Blessing.MIGHT, Blessing.RESISTANCE, Blessing.REGENERATION};
    public static final double RADIUS = 10.0; // the zone ring is drawn by ZoneParticlesClient
    private static final int LIGHT_LEVEL = 10;
    private static final Map<UUID, Set<BlockPos>> LIGHTS = new HashMap<>();
    private static final Map<UUID, ServerLevel> LIGHT_LEVELS = new HashMap<>();

    private AngelHandler() {
    }

    public static int sanitize(int mask) {
        mask &= (1 << Blessing.values().length) - 1;
        return Integer.bitCount(mask) <= MAX_CHOSEN ? mask : DEFAULT_BLESSINGS;
    }

    public static void setBlessings(ServerPlayer player, int mask) {
        if (!BOPowers.has(player, BOPowers.ANGEL_ALTRUIST)) return;
        player.setData(BetterOriginsAttachments.ANGEL_BLESSINGS, sanitize(mask));
        player.syncData(BetterOriginsAttachments.ANGEL_BLESSINGS);
    }

    private static void bless(LivingEntity target, Blessing blessing) {
        boolean slowTimer = blessing == Blessing.REGENERATION;
        BetterOriginsEffects.refresh(target, blessing.effect.get(), blessing.amplifier, slowTimer ? 110 : 40, slowTimer ? 55 : 20, true); // Regeneration heals every 50 ticks: keep its timer intact
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer angel) || !(angel.level() instanceof ServerLevel level)) return;
        boolean altruist = angel.isAlive() && BOPowers.active(angel, BOPowers.ANGEL_ALTRUIST);
        boolean selfish = angel.isAlive() && BOPowers.active(angel, BOPowers.ANGEL_SELFISH);
        if (altruist || selfish) updateLight(angel, level); // the light shines in both modes, the zone only for others
        else if (LIGHTS.containsKey(angel.getUUID())) clearLight(angel.getUUID());
        if (level.getGameTime() % 10 != 0 || !angel.isAlive()) return;

        if (selfish)
            for (Blessing blessing : SELFISH) bless(angel, blessing);

        if (altruist) {
            int mask = sanitize(angel.getData(BetterOriginsAttachments.ANGEL_BLESSINGS));
            AABB box = angel.getBoundingBox().inflate(RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!target.isAlive() || target.distanceToSqr(angel) > RADIUS * RADIUS) continue;
                boolean pet = target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
                if (!(target instanceof Player) && !pet) continue;
                if (target instanceof Player p && p.isSpectator()) continue;
                for (Blessing blessing : Blessing.values())
                    if ((mask & blessing.bit()) != 0) bless(target, blessing);
            }
        }
    }

    // ---------- light around the Angel (only invisible light blocks placed by us, only in air) ----------
    private static void updateLight(ServerPlayer angel, ServerLevel level) {
        UUID id = angel.getUUID();
        ServerLevel previous = LIGHT_LEVELS.get(id);
        if (previous != null && previous != level) clearLight(id);
        Set<BlockPos> placed = LIGHTS.computeIfAbsent(id, k -> new HashSet<>());
        LIGHT_LEVELS.put(id, level);
        BlockPos centre = angel.blockPosition();
        Set<BlockPos> wanted = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = 0; dy <= 1; dy++) wanted.add(centre.offset(dx, dy, dz));
        placed.removeIf(pos -> {
            if (wanted.contains(pos)) return false;
            removeOurLight(level, pos);
            return true;
        });
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL);
        for (BlockPos pos : wanted) {
            if (!level.isLoaded(pos)) continue;
            // also re-place a light that something else removed (for example another Angel walking away)
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, light, 3);
                placed.add(pos.immutable());
            }
        }
    }

    private static void removeOurLight(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == LIGHT_LEVEL)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void clearLight(UUID id) {
        Set<BlockPos> placed = LIGHTS.remove(id);
        ServerLevel level = LIGHT_LEVELS.remove(id);
        if (placed != null && level != null) placed.forEach(pos -> removeOurLight(level, pos));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearLight(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearLight(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (UUID id : Set.copyOf(LIGHTS.keySet())) clearLight(id);
    }

    // ---------- Feather: no fall damage ----------
    @SubscribeEvent
    public static void onFall(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && BetterOriginsEffects.ANGEL_FEATHER != null
                && event.getEntity().hasEffect(BetterOriginsEffects.holder(BetterOriginsEffects.ANGEL_FEATHER)))
            event.setCanceled(true);
    }
}
