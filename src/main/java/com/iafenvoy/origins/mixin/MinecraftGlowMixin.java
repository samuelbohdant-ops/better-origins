package com.iafenvoy.origins.mixin;

import com.betterorigins.BatEchoClient;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Better Origins: Superior Bat's Echolocation outlines (client only). */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean betterOrigins$batEcho(boolean original, Entity entity) {
        return original || BatEchoClient.shouldGlow(entity);
    }
}
