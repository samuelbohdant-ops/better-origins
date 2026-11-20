package com.betterorigins;

import com.iafenvoy.origins.attachment.OriginDataHolder;
import com.iafenvoy.origins.data.power.component.PowerComponent;
import com.iafenvoy.origins.data.power.component.builtin.CooldownComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;

/**
 * Quando un giocatore rinasce, tutte le ricariche dei poteri tornano pronte
 * (perla dell'Enderian, palla di fuoco, lancio dell'Elytrian, balzo, ragnatela, ...).
 *
 * Origins copia i dati dei poteri alla morte (copyOnDeath), quindi le ricariche continuerebbero.
 * Le barre a tempo (Velo, Fluttuare, Adrenalina) sono gia' resettate dai file dati (action_on_callback).
 * Non vale quando si esce dall'End (isEndConquered): li' non si e' morti.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class RespawnCooldownReset {
    private RespawnCooldownReset() {
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        OriginDataHolder holder = OriginDataHolder.get(player);
        boolean changed = false;
        for (Map<Class<? extends PowerComponent>, PowerComponent> components : holder.getData().getComponents().values())
            for (PowerComponent component : components.values())
                if (component instanceof CooldownComponent cooldown && !cooldown.canUse()) {
                    cooldown.setValue(0);
                    changed = true;
                }
        if (changed) holder.sync();
    }
}
