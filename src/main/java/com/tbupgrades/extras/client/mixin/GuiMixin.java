package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.client.OmegaFlightClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps vanilla's experience level number off the screen while the flight charge owns the bar.
 *
 * <p>{@code Gui.renderHotbarAndDecorations} draws the bar's background, then the experience level
 * number, then the bar's contents. The flight charge takes over the background, so the level number
 * would land on top of it. {@link com.tbupgrades.extras.client.OmegaFlightHud} draws the charge in
 * that same number's place, and this is what stops the two from being drawn twice.
 *
 * <p>The gate is {@code hasExperience()}, which in that method is asked exactly once, immediately
 * before the number. Declining it while the charge is up skips the number along with the
 * {@code experienceLevel > 0} test behind it - so the charge still shows at experience level zero,
 * which is when a player is most likely to be flying around with nothing to show for it.
 *
 * <p>With the charge down, {@code hasExperience()} answers for itself and nothing about the ordinary
 * experience bar changes.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Redirect(method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean tbx$chargeInsteadOfLevel(MultiPlayerGameMode gameMode) {
        return !OmegaFlightClient.barVisible() && gameMode.hasExperience();
    }
}
