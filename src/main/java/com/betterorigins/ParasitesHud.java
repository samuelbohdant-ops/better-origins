package com.betterorigins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** The 10-step parasite bar shown to a player infected by a Cave Worm (above the hotbar, centred). */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class ParasitesHud {
    private static final int SEGMENTS = 10, SEG_W = 9, SEG_H = 5, GAP = 1;

    private ParasitesHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, BOPowers.id("parasites"), ParasitesHud::render);
    }

    private static int colour(int segment) {
        // green -> yellow -> sickly orange as the level rises
        if (segment < 3) return 0xFF7BD12E;
        if (segment < 6) return 0xFFB8D624;
        if (segment < 9) return 0xFFE0C81E;
        return 0xFFE8861A;
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !player.hasData(BetterOriginsAttachments.PARASITES)) return;
        int level = CaveWormHandler.level(player.getData(BetterOriginsAttachments.PARASITES));
        if (level <= 0) return;
        int width = SEGMENTS * SEG_W + (SEGMENTS - 1) * GAP;
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = graphics.guiHeight() - 68;
        graphics.fill(x - 2, y - 2, x + width + 2, y + SEG_H + 2, 0xAA0F1A08);
        for (int i = 0; i < SEGMENTS; i++) {
            int sx = x + i * (SEG_W + GAP);
            graphics.fill(sx, y, sx + SEG_W, y + SEG_H, i < level ? colour(i) : 0xFF2A3320);
            if (i < level) graphics.fill(sx, y, sx + SEG_W, y + 1, 0x55FFFFFF);
        }
        Component label = Component.translatable("gui.better_origins.parasites", level);
        graphics.drawString(minecraft.font, label, graphics.guiWidth() / 2 - minecraft.font.width(label) / 2, y - 11, 0xC8E86A, true);
    }
}
