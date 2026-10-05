package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.client.OmegaFlightHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the flight charge where the experience bar's fill would have been, and only while the charge is
 * actually being spent - see {@link OmegaFlightHud} for why the background is the half to take over.
 */
@Mixin(ExperienceBarRenderer.class)
public abstract class ExperienceBarRendererMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void tbx$omegaFlightBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (OmegaFlightHud.render(graphics, (ContextualBarRenderer) (Object) this)) {
            ci.cancel();
        }
    }
}
