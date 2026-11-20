package com.betterorigins;

import com.iafenvoy.origins.attachment.PowerHelper;
import com.iafenvoy.origins.data.power.Power;
import com.iafenvoy.origins.data.power.PowerRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;

import java.util.Optional;

/**
 * Occhi dell'End: gli enderman non reagiscono allo sguardo di chi ha il potere better_origins:end_eyes.
 *
 * NeoForge lancia EnderManAngerEvent quando un enderman controlla se un giocatore lo sta fissando.
 * Annullandolo, l'enderman non si accorge dello sguardo: niente bocca aperta, urlo o suono.
 * Se lo colpisci, la Parentela dell'End (JSON) lo rende comunque ostile per 15 secondi.
 *
 * DA VERIFICARE alla prima compilazione: nome e pacchetto esatti dell'evento in NeoForge 1.21.1.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class EndEyesHandler {
    private static final ResourceKey<Power> END_EYES = ResourceKey.create(
            PowerRegistries.POWER_KEY,
            ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "end_eyes"));

    private EndEyesHandler() {
    }

    @SubscribeEvent
    public static void onEnderManAnger(EnderManAngerEvent event) {
        Player player = event.getPlayer();
        Optional<Power> endEyes = player.level().registryAccess()
                .registryOrThrow(PowerRegistries.POWER_KEY)
                .getOptional(END_EYES);
        if (endEyes.isEmpty()) {
            return; // il potere non e' caricato (es. datapack modificato): nessun effetto
        }
        Power target = endEyes.get();
        if (PowerHelper.get(player).anyActive(Power.class, power -> power == target)) {
            event.setCanceled(true);
        }
    }
}
