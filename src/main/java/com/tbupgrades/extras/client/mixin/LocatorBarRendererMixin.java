package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.client.OmegaFlightHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The other half of the shared bar slot: when the locator bar is the one showing, its background is
 * replaced by the flight charge while flying.
 *
 * <p>Only {@code renderBackground} is cancelled. {@code render}, which draws the waypoint dots and
 * arrows and no background at all, still runs afterwards and lays them over the charge - so the player
 * position display stays readable instead of being hidden for the duration of a flight.
 */
@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void tbx$omegaFlightBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (OmegaFlightHud.render(graphics, (ContextualBarRenderer) (Object) this)) {
            ci.cancel();
        }
    }
}
