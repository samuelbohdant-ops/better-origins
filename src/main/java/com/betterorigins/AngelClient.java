package com.betterorigins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Angel, client side: the 4th ability key opens the blessing menu; next to the Angel's bar the chosen blessings are
 * shown (bright while altruistic mode is on, faded while off).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class AngelClient {
    public static final ResourceLocation ICONS = BOPowers.id("textures/gui/angel_blessings.png");
    public static final ResourceLocation BAR_SHEET = BOPowers.id("textures/gui/power_bars.png");
    public static final int ANGEL_BAR_INDEX = 7;
    private static boolean wasDown = false;

    private AngelClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean down = BetterOriginsKeys.QUATERNARY.isDown();
        if (down && !wasDown && player != null && minecraft.screen == null && BOPowers.has(player, BOPowers.ANGEL_MENU))
            minecraft.setScreen(new AngelBlessingScreen(chosen(player)));
        wasDown = down;
    }

    public static int chosen(LocalPlayer player) {
        return AngelHandler.sanitize(player.getData(BetterOriginsAttachments.ANGEL_BLESSINGS));
    }

    /** Draws one 16x16 blessing icon from the sheet at any size. */
    public static void drawIcon(GuiGraphics graphics, AngelHandler.Blessing blessing, int x, int y, int size) {
        graphics.blit(ICONS, x, y, size, size, blessing.ordinal() * 16, 0, 16, 16, 96, 16);
    }

    /** Called by Origins' HUD bar renderer after each bar; adds the blessing icons after the Angel's bar. */
    public static void afterBar(GuiGraphics graphics, ResourceLocation sprite, int barIndex, int x, int y, int barWidth) {
        if (barIndex != ANGEL_BAR_INDEX || !BAR_SHEET.equals(sprite)) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean on = BOPowers.active(player, BOPowers.ANGEL_ALTRUIST);
        int mask = chosen(player);
        int ix = x + barWidth + 3;
        graphics.setColor(1F, 1F, 1F, on ? 1F : 0.45F);
        for (AngelHandler.Blessing blessing : AngelHandler.Blessing.values()) {
            if ((mask & blessing.bit()) == 0) continue;
            drawIcon(graphics, blessing, ix, y - 2, 8);
            ix += 9;
        }
        graphics.setColor(1F, 1F, 1F, 1F);
    }
}
