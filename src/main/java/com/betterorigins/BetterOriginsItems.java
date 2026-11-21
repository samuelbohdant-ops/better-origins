package com.betterorigins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Better Origins items.
 * - perfect_human_emblem: icon of The Perfect Human in the origin screen
 * - feline_emblem: icon of the Feline in the origin screen
 * Both have no recipe and are not in creative tabs (can still be obtained with /give).
 */
@EventBusSubscriber(modid = BetterOriginsIds.MOD_ID)
public final class BetterOriginsItems {
    public static final ResourceLocation PERFECT_HUMAN_EMBLEM_ID =
            ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "perfect_human_emblem");
    public static final ResourceLocation FELINE_EMBLEM_ID =
            ResourceLocation.fromNamespaceAndPath(BetterOriginsIds.CONTENT_NS, "feline_emblem");

    private BetterOriginsItems() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            helper.register(PERFECT_HUMAN_EMBLEM_ID, new Item(new Item.Properties().stacksTo(1)));
            helper.register(FELINE_EMBLEM_ID, new Item(new Item.Properties().stacksTo(1)));
        });
    }
}
