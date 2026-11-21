package com.betterorigins;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Better Origins status effects:
 * - stunned (Road Runner): frozen in place; the rest of the freeze is in RoadRunnerHandler / RoadRunnerClient;
 * - Angel blessings: swiftness (+15% speed), zeal (+20% attack speed), might (+15% damage), feather (falls 20% slower,
 *   no fall damage - see AngelHandler).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BetterOriginsEffects {
    public static MobEffect STUNNED, ANGEL_SWIFTNESS, ANGEL_ZEAL, ANGEL_MIGHT, ANGEL_FEATHER;

    private BetterOriginsEffects() {
    }

    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.MOB_EFFECT, helper -> {
            // no attribute changes: a 0 speed would zoom the player's view in; the freeze is done in RoadRunnerHandler/Client
            STUNNED = new SimpleEffect(MobEffectCategory.HARMFUL, 0xF2C94C);
            ANGEL_SWIFTNESS = new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFF4C2)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, BOPowers.id("angel_swiftness"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            ANGEL_ZEAL = new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFD85A)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, BOPowers.id("angel_zeal"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            ANGEL_MIGHT = new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xE8B43A)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, BOPowers.id("angel_might"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            ANGEL_FEATHER = new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF)
                    .addAttributeModifier(Attributes.GRAVITY, BOPowers.id("angel_feather"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            helper.register(BOPowers.id("stunned"), STUNNED);
            helper.register(BOPowers.id("angel_swiftness"), ANGEL_SWIFTNESS);
            helper.register(BOPowers.id("angel_zeal"), ANGEL_ZEAL);
            helper.register(BOPowers.id("angel_might"), ANGEL_MIGHT);
            helper.register(BOPowers.id("angel_feather"), ANGEL_FEATHER);
        });
    }

    public static Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    /**
     * Gives an effect, or refreshes it only when it is about to run out (re-adding every tick would reset the
     * damage/heal timers of effects like Poison and Regeneration).
     */
    public static void refresh(LivingEntity entity, Holder<MobEffect> effect, int amplifier, int duration, int refreshBelow, boolean visible) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier) return;
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < refreshBelow)
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, visible, true));
    }
}
