package com.betterorigins;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Extra data stored on entities:
 * - parasites: Cave Worm parasite build-up, 0..240 (240 = level 10); sent only to the infected player (for the HUD bar);
 * - angel_blessings: the 3 blessings an Angel gives in altruistic mode (bit mask of AngelHandler.Blessing), kept on
 *   death, sent only to that player.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BetterOriginsAttachments {
    public static final AttachmentType<Float> PARASITES = AttachmentType.builder(() -> 0F)
            .serialize(Codec.FLOAT, value -> value > 0)
            .sync((holder, player) -> holder == player, ByteBufCodecs.FLOAT)
            .build();
    public static final AttachmentType<Integer> ANGEL_BLESSINGS = AttachmentType.builder(() -> AngelHandler.DEFAULT_BLESSINGS)
            .serialize(Codec.INT)
            .copyOnDeath()
            .sync((holder, player) -> holder == player, ByteBufCodecs.VAR_INT)
            .build();

    private BetterOriginsAttachments() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> {
            helper.register(BOPowers.id("parasites"), PARASITES);
            helper.register(BOPowers.id("angel_blessings"), ANGEL_BLESSINGS);
        });
    }
}
