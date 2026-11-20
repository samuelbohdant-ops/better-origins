package com.iafenvoy.origins;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Origins.MOD_ID, dist = Dist.CLIENT)
public final class OriginsClient {
    public OriginsClient(ModContainer container) {
        Proxies.TICK_COUNT = () -> Minecraft.getInstance().clientTickCount;
        // Better Origins: NeoForge's built-in config screen (Mods -> Better Origins -> Config)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
