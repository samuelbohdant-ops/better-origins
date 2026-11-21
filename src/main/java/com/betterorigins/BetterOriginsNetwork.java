package com.betterorigins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Messages from the player's game to the server:
 * - KineticCrash: the Road Runner hit a wall while sprinting (movement is calculated on the player's own game, so the
 *   crash is detected there); the server checks the power and limits the damage. It can only hurt the sender.
 * - SetBlessings: the 3 blessings chosen in the Angel menu.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BetterOriginsNetwork {
    private BetterOriginsNetwork() {
    }

    public record KineticCrash(float speed) implements CustomPacketPayload {
        public static final Type<KineticCrash> TYPE = new Type<>(BOPowers.id("kinetic_crash"));
        public static final StreamCodec<ByteBuf, KineticCrash> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, KineticCrash::speed, KineticCrash::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetBlessings(int mask) implements CustomPacketPayload {
        public static final Type<SetBlessings> TYPE = new Type<>(BOPowers.id("set_blessings"));
        public static final StreamCodec<ByteBuf, SetBlessings> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SetBlessings::mask, SetBlessings::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(KineticCrash.TYPE, KineticCrash.STREAM_CODEC, BetterOriginsNetwork::onKineticCrash);
        registrar.playToServer(SetBlessings.TYPE, SetBlessings.STREAM_CODEC, BetterOriginsNetwork::onSetBlessings);
    }

    private static void onKineticCrash(KineticCrash payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) RoadRunnerHandler.crash(player, payload.speed());
        });
    }

    private static void onSetBlessings(SetBlessings payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) AngelHandler.setBlessings(player, payload.mask());
        });
    }
}
