package com.betterorigins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/**
 * Shulk's inventory (the 27-slot power inventory): paints the frame around the Shulk slots purple,
 * in the same palette as the Shulk in the origin menu. The slots and the player's own inventory keep
 * the normal colours. Drawn after the normal background and before the items.
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID, value = Dist.CLIENT)
public final class ShulkInventoryClient {
    private static final String TITLE_KEY = "container.shulker_inventory_power";
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "textures/gui/shulk_inventory.png");
    private static final int WIDTH = 176, HEIGHT = 71; // top part of a 3-row chest screen (title + 3 rows of slots)

    private ShulkInventoryClient() {
    }

    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!(screen instanceof ContainerScreen)) return;
        if (!(screen.getTitle().getContents() instanceof TranslatableContents contents) || !TITLE_KEY.equals(contents.getKey())) return;
        GuiGraphics graphics = event.getGuiGraphics();
        RenderSystem.enableBlend();
        graphics.blit(FRAME, screen.getGuiLeft(), screen.getGuiTop(), 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        RenderSystem.disableBlend();
    }
}
