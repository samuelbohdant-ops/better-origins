package com.betterorigins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/** Angel blessing menu: choose up to 3 of the 6 blessings given in altruistic mode. White and gold. */
@OnlyIn(Dist.CLIENT)
public class AngelBlessingScreen extends Screen {
    private static final int PANEL_W = 220, PANEL_H = 168, CELL_W = 64, CELL_H = 44;
    private static final int WHITE = 0xFFF8F4E8, GOLD = 0xFFE8B84A, GOLD_DARK = 0xFFB08528, TEXT = 0xFF5A4520;
    private int mask;
    private int left, top;

    public AngelBlessingScreen(int mask) {
        super(Component.translatable("gui.better_origins.blessings.title"));
        this.mask = mask;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 40, this.top + PANEL_H - 26, 80, 20).build());
    }

    private int cellX(int i) {
        return this.left + 12 + (i % 3) * (CELL_W + 4);
    }

    private int cellY(int i) {
        return this.top + 30 + (i / 3) * (CELL_H + 6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            AngelHandler.Blessing[] all = AngelHandler.Blessing.values();
            for (int i = 0; i < all.length; i++) {
                int x = this.cellX(i), y = this.cellY(i);
                if (mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H) {
                    int bit = all[i].bit();
                    if ((this.mask & bit) != 0) this.mask &= ~bit;
                    else if (Integer.bitCount(this.mask) < AngelHandler.MAX_CHOSEN) this.mask |= bit;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int l = this.left, t = this.top;
        // panel: gold frame, warm white inside (drawn behind the Done button)
        graphics.fill(l - 2, t - 2, l + PANEL_W + 2, t + PANEL_H + 2, GOLD_DARK);
        graphics.fill(l - 1, t - 1, l + PANEL_W + 1, t + PANEL_H + 1, GOLD);
        graphics.fill(l, t, l + PANEL_W, t + PANEL_H, WHITE);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int t = this.top;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, t + 8, 0xFFC8962E);
        Component count = Component.translatable("gui.better_origins.blessings.count", Integer.bitCount(this.mask), AngelHandler.MAX_CHOSEN);
        graphics.drawString(this.font, count, this.width / 2 - this.font.width(count) / 2, t + 18, TEXT, false);

        AngelHandler.Blessing[] all = AngelHandler.Blessing.values();
        for (int i = 0; i < all.length; i++) {
            int x = this.cellX(i), y = this.cellY(i);
            boolean selected = (this.mask & all[i].bit()) != 0;
            boolean hover = mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H;
            graphics.fill(x, y, x + CELL_W, y + CELL_H, selected ? GOLD : hover ? 0xFFEDE3C8 : 0xFFE6DCC4);
            graphics.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, selected ? 0xFFFFF1C9 : 0xFFF5EEDD);
            RenderSystem.enableBlend();
            AngelClient.drawIcon(graphics, all[i], x + CELL_W / 2 - 8, y + 5, 16);
            Component name = Component.translatable("gui.better_origins.blessing." + all[i].name().toLowerCase());
            graphics.drawString(this.font, name, x + CELL_W / 2 - this.font.width(name) / 2, y + 27, selected ? 0xFF8A5A00 : TEXT, false);
        }
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new BetterOriginsNetwork.SetBlessings(this.mask));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
