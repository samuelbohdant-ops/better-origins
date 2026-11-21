package com.betterorigins;

import com.iafenvoy.origins.attachment.EntityOriginAttachment;
import com.iafenvoy.origins.data.power.component.PowerComponent;
import com.iafenvoy.origins.data.power.component.builtin.CooldownComponent;
import com.iafenvoy.origins.registry.OriginsAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Elytrian launch ("Gift of the Winds") sound.
 * Origins' launch power played its sound on the server at a fixed low pitch and it was never heard,
 * so the client plays it instead: when the launch cooldown of a nearby player restarts (synced data),
 * the sound is played attached to that player, so it follows them while they fly up.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class LaunchSoundClient {
    private static final ResourceLocation LAUNCH_POWER = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "gift_of_the_winds");
    private static final Map<UUID, Integer> LAST_VALUES = new HashMap<>();

    private LaunchSoundClient() {
    }

    /** Remaining launch cooldown of a player, or -1 if the player doesn't have the power. */
    private static int cooldownOf(Player player) {
        EntityOriginAttachment data = player.getExistingData(OriginsAttachments.ENTITY_ORIGIN).orElse(null);
        if (data == null) return -1;
        Map<Class<? extends PowerComponent>, PowerComponent> components = data.getComponents().get(LAUNCH_POWER);
        if (components == null) return -1;
        return components.get(CooldownComponent.class) instanceof CooldownComponent cooldown ? cooldown.getValue() : -1;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            LAST_VALUES.clear();
            return;
        }
        Map<UUID, Integer> seen = new HashMap<>();
        for (AbstractClientPlayer player : level.players()) {
            int now = cooldownOf(player);
            if (now < 0) continue;
            seen.put(player.getUUID(), now);
            Integer before = LAST_VALUES.get(player.getUUID());
            if (before != null && now > before + 100) // cooldown just restarted = launch used
                minecraft.getSoundManager().play(new EntityBoundSoundInstance(BetterOriginsSounds.ELYTRIAN_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.0F, player, player.getRandom().nextLong()));
        }
        LAST_VALUES.clear();
        LAST_VALUES.putAll(seen);
    }
}
