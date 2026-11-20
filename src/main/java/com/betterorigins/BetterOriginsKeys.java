package com.betterorigins;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Tasti abilita' 3, 4 e 5.
 *
 * Origins attiva un potere quando viene premuto il tasto il cui NOME e' scritto nel campo "key"
 * del potere: cerca il nome fra tutti i tasti registrati nel gioco (KeyMapping.ALL).
 * Basta quindi registrare questi tre tasti: i poteri con
 * "key": {"key": "key.better_origins.ternary_active"} funzionano da soli.
 *
 * Solo lato client (i tasti non esistono sul server).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class BetterOriginsKeys {
    /** Stessa categoria dei tasti di Origins, cosi' nelle impostazioni stanno tutti insieme. */
    private static final String CATEGORY = "category.origins";

    public static final KeyMapping TERNARY = new KeyMapping(
            "key.better_origins.ternary_active", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping QUATERNARY = new KeyMapping(
            "key.better_origins.quaternary_active", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, CATEGORY);
    public static final KeyMapping QUINARY = new KeyMapping(
            "key.better_origins.quinary_active", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, CATEGORY);

    private BetterOriginsKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TERNARY);
        event.register(QUATERNARY);
        event.register(QUINARY);
    }
}
