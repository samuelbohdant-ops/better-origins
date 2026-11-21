package com.iafenvoy.origins.screen;

import com.betterorigins.OriginStyles;
import com.iafenvoy.origins.Origins;
import com.iafenvoy.origins.data.layer.Layer;
import com.iafenvoy.origins.data.origin.Impact;
import com.iafenvoy.origins.data.origin.Origin;
import com.iafenvoy.origins.network.payload.ChooseOriginC2SPayload;
import com.iafenvoy.origins.registry.OriginsItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Better Origins: the list of origins is split in 4 sections
 * (Standard = no impact + Random, Low, Medium, High impact), and origins with a style texture
 * get their own coloured background (see {@link OriginStyles}).
 */
@OnlyIn(Dist.CLIENT)
public class ChooseOriginScreen extends OriginDisplayScreen {
    private static final ResourceLocation ORIGINS_CHOICES = ResourceLocation.fromNamespaceAndPath(Origins.MOD_ID, "textures/gui/origin_choices.png");
    private static final int CHOICES_WIDTH = 219, CHOICES_HEIGHT = 182, ORIGIN_ICON_SIZE = 26;
    private static final int COLUMNS = 7, CELL_WIDTH = 28, CELL_HEIGHT = 28, HEADER_HEIGHT = 12, LEFT_PADDING = 12, TOP_PADDING = 8, BOTTOM_LIMIT = CHOICES_HEIGHT - 6;
    private static final String[] SECTION_KEYS = {"standard", "low", "medium", "high"};
    private static final ChatFormatting[] SECTION_COLORS = {ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.RED};
    private final List<Holder<Layer>> layers;
    private final List<Holder<Origin>> origins;
    private final int currentLayerIndex;
    private final List<List<Entry>> pageEntries = new ArrayList<>();
    private Holder<Origin> randomOrigin;
    private int currentOriginIndex = 0, maxSelection = 0;
    private int calculatedTop, calculatedLeft;
    private int currentPage = 0, pages = 1;

    /** One thing drawn in the list: a section title (index -1) or an origin slot (index into origins, origins.size() = random). */
    private record Entry(int index, int section, int x, int y) {
        boolean isHeader() {
            return this.index < 0;
        }
    }

    public ChooseOriginScreen(List<Holder<Layer>> layers, int currentLayerIndex, boolean showDirtBackground) {
        super(Component.empty(), showDirtBackground);
        this.layers = layers;
        this.currentLayerIndex = currentLayerIndex;
        this.origins = new ArrayList<>(layers.size());
        OriginStyles.clearCache();
        this.initRandomOrigin();
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Layer currentLayer = this.getCurrentLayer().value();
            currentLayer.collectOrigins(player).forEach(holder -> {
                if (!holder.value().unchoosable()) {
                    ItemStack iconStack = holder.value().icon().orElse(ItemStack.EMPTY);
                    if (iconStack.is(Items.PLAYER_HEAD) && !iconStack.has(DataComponents.PROFILE))
                        iconStack.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
                    this.origins.add(holder);
                }
            });
            this.origins.sort(Comparator.<Holder<Origin>>comparingInt(o -> o.value().impact().getImpactValue()).thenComparingInt(x -> x.value().order()));
            this.maxSelection = currentLayer.getOriginOptionCount(player);
            if (this.maxSelection == 0) {
                this.openNextLayerScreen();
            }

            Holder<Origin> newOrigin = this.getCurrentOrigin();
            this.showOrigin(newOrigin, this.getCurrentLayer(), Objects.equals(newOrigin.getKey(), this.randomOrigin.getKey()));
        }
    }

    private void openNextLayerScreen() {
        Minecraft.getInstance().setScreen(new WaitForNextLayerScreen(this.layers, this.currentLayerIndex, this.showDirtBackground));
    }

    private void initRandomOrigin() {
        this.randomOrigin = Holder.direct(Origin.special(ResourceLocation.fromNamespaceAndPath(Origins.MOD_ID, "random"), OriginsItems.ORB_OF_ORIGIN.toStack(), Impact.NONE, -1));
        MutableComponent randomOriginText = Component.empty();
        Player player = Minecraft.getInstance().player;
        assert player != null;
        this.layers.get(this.currentLayerIndex).value().collectRandomizableOrigins(player).sorted((ia, ib) -> {
            Origin a = ia.value();
            Origin b = ib.value();
            int impactDelta = Integer.compare(a.impact().getImpactValue(), b.impact().getImpactValue());
            return impactDelta != 0 ? impactDelta : Integer.compare(a.order(), b.order());
        }).forEach(origin -> {
            randomOriginText.append(Origin.getName(origin));
            randomOriginText.append(Component.literal("\n"));
        });
        this.setRandomOriginText(randomOriginText);
    }

    @Override
    public @NotNull Component getTitle() {
        return this.getCurrentLayer().value().getChooseOriginTitle(Component.translatable("origins.gui.choose_origin.title", Layer.getName(this.getCurrentLayer())));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private boolean hasRandom() {
        return this.maxSelection > this.origins.size();
    }

    private static int sectionOf(Origin origin) {
        return Mth.clamp(origin.impact().getImpactValue(), 0, 3);
    }

    /** Splits the origins in 4 sections and places them on pages (a page = what fits in the box). */
    private void buildLayout() {
        List<List<Integer>> sections = new ArrayList<>();
        for (int s = 0; s < 4; s++) sections.add(new ArrayList<>());
        for (int i = 0; i < this.origins.size(); i++) sections.get(sectionOf(this.origins.get(i).value())).add(i);
        if (this.hasRandom()) sections.get(0).add(this.origins.size());

        this.pageEntries.clear();
        List<Entry> page = new ArrayList<>();
        this.pageEntries.add(page);
        int y = TOP_PADDING;
        for (int s = 0; s < 4; s++) {
            List<Integer> list = sections.get(s);
            for (int row = 0; row * COLUMNS < list.size(); row++) {
                boolean needsHeader = row == 0;
                if (y + (needsHeader ? HEADER_HEIGHT : 0) + CELL_HEIGHT > BOTTOM_LIMIT && !page.isEmpty()) {
                    page = new ArrayList<>();
                    this.pageEntries.add(page);
                    y = TOP_PADDING;
                    needsHeader = true; // repeat the title at the top of the new page
                }
                if (needsHeader) {
                    page.add(new Entry(-1, s, LEFT_PADDING, y));
                    y += HEADER_HEIGHT;
                }
                for (int c = 0; c < COLUMNS && row * COLUMNS + c < list.size(); c++)
                    page.add(new Entry(list.get(row * COLUMNS + c), s, LEFT_PADDING + c * CELL_WIDTH, y));
                y += CELL_HEIGHT;
            }
        }
        this.pages = this.pageEntries.size();
        if (this.currentPage >= this.pages) this.currentPage = 0;
    }

    @Override
    protected void init() {
        super.init();
        this.calculatedTop = (this.height - CHOICES_HEIGHT) / 2;
        this.calculatedLeft = (this.width - 405) / 2;
        this.guiTop = (this.height - CHOICES_HEIGHT) / 2;
        this.guiLeft = this.calculatedLeft + CHOICES_WIDTH + 10;
        this.buildLayout();
        if (this.pages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), (b) -> {
                --this.currentPage;
                if (this.currentPage < 0) this.currentPage = this.pages - 1;
            }).pos(this.calculatedLeft, this.guiTop + CHOICES_HEIGHT + 5).size(20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), (b) -> this.currentPage = (this.currentPage + 1) % this.pages).pos(this.calculatedLeft + CHOICES_WIDTH - 20, this.guiTop + CHOICES_HEIGHT + 5).size(20, 20).build());
        }
        if (this.maxSelection > 0) {
            this.addRenderableWidget(Button.builder(Component.translatable("origins.gui.select"), (button) -> {
                PacketDistributor.sendToServer(new ChooseOriginC2SPayload(this.getCurrentLayer(), this.currentOriginIndex == this.origins.size() ? Optional.empty() : Optional.of(super.getCurrentOrigin())));
                this.openNextLayerScreen();
            }).bounds(this.guiLeft + 88 - 50, this.guiTop + CHOICES_HEIGHT + 5, 100, 20).build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.pageEntries.isEmpty()) {
            for (Entry entry : this.pageEntries.get(this.currentPage)) {
                if (entry.isHeader()) continue;
                int x = this.calculatedLeft + entry.x(), y = this.calculatedTop + entry.y();
                if (mouseX >= x && mouseY >= y && mouseX < x + ORIGIN_ICON_SIZE && mouseY < y + ORIGIN_ICON_SIZE) {
                    this.currentOriginIndex = entry.index();
                    Holder<Origin> newOrigin = this.getCurrentOrigin();
                    this.showOrigin(newOrigin, this.layers.get(this.currentLayerIndex), newOrigin == this.randomOrigin);
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public Holder<Layer> getCurrentLayer() {
        return this.layers.get(this.currentLayerIndex);
    }

    @Override
    public Holder<Origin> getCurrentOrigin() {
        if (this.currentOriginIndex == this.origins.size()) return this.randomOrigin;
        else return this.origins.get(this.currentOriginIndex);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.maxSelection == 0) this.openNextLayerScreen();
        else super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderOriginWindow(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderOriginChoicesBox(graphics, mouseX, mouseY);
        super.renderOriginWindow(graphics, mouseX, mouseY, delta);
    }

    public void renderOriginChoicesBox(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(ORIGINS_CHOICES, this.calculatedLeft, this.calculatedTop, 0, 0, CHOICES_WIDTH, CHOICES_HEIGHT);
        if (this.pageEntries.isEmpty()) return;
        for (Entry entry : this.pageEntries.get(this.currentPage)) {
            int x = this.calculatedLeft + entry.x(), y = this.calculatedTop + entry.y();
            if (entry.isHeader()) {
                Component title = Component.translatable("better_origins.gui.section." + SECTION_KEYS[entry.section()]).withStyle(SECTION_COLORS[entry.section()]);
                graphics.drawString(this.font, title, x + 1, y + 2, 0xFFFFFF, true);
                int lineStart = x + this.font.width(title) + 5;
                graphics.fill(lineStart, y + 5, this.calculatedLeft + CHOICES_WIDTH - LEFT_PADDING, y + 6, 0x66FFFFFF);
            } else if (entry.index() >= this.origins.size()) {
                boolean selected = this.getCurrentOrigin().equals(this.randomOrigin);
                this.renderRandomOrigin(graphics, mouseX, mouseY, x, y, selected);
            } else {
                Holder<Origin> origin = this.origins.get(entry.index());
                boolean selected = Objects.equals(origin.getKey(), this.getCurrentOrigin().getKey());
                this.renderOriginWidget(graphics, mouseX, mouseY, x, y, selected, origin);
                graphics.renderItem(origin.value().icon().orElse(ItemStack.EMPTY), x + 5, y + 5);
            }
        }
        if (this.pages > 1)
            graphics.drawCenteredString(this.font, Component.literal((this.currentPage + 1) + "/" + this.pages).getVisualOrderText(), this.calculatedLeft + 109, this.guiTop + CHOICES_HEIGHT + 9, 16777215);
    }

    private boolean renderOriginBase(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean selected, Holder<Origin> origin) {
        boolean mouseHovering = mouseX >= x && mouseY >= y && mouseX < x + ORIGIN_ICON_SIZE && mouseY < y + ORIGIN_ICON_SIZE;
        int v = (selected ? ORIGIN_ICON_SIZE : 0) + (mouseHovering ? 52 : 0);
        Optional<ResourceLocation> style = origin == this.randomOrigin ? Optional.empty() : OriginStyles.slot(origin.getKey() == null ? null : origin.getKey().location());
        if (style.isPresent()) {
            graphics.blit(style.get(), x, y, 0, v, ORIGIN_ICON_SIZE, ORIGIN_ICON_SIZE, OriginStyles.SLOT_SIZE, OriginStyles.SLOT_TEXTURE_HEIGHT);
        } else {
            graphics.blit(ORIGINS_CHOICES, x, y, 230, v, ORIGIN_ICON_SIZE, ORIGIN_ICON_SIZE);
        }
        if (mouseHovering) {
            Component text = Layer.getName(this.getCurrentLayer()).copy().append(": ").append(Origin.getName(origin));
            graphics.renderTooltip(this.font, text, mouseX, mouseY);
        }
        return mouseHovering;
    }

    public void renderOriginWidget(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean selected, Holder<Origin> origin) {
        this.renderOriginBase(graphics, mouseX, mouseY, x, y, selected, origin);
    }

    public void renderRandomOrigin(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean selected) {
        this.renderOriginBase(graphics, mouseX, mouseY, x, y, selected, this.randomOrigin);
        graphics.blit(ORIGINS_CHOICES, x + 6, y + 5, 243, 120, 13, 16);
    }

    private static final class Mth {
        static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
