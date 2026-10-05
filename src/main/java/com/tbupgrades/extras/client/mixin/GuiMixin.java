package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.client.OmegaFlightClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
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
 * <p><b>What is redirected here matters more than what it does.</b> The number is drawn behind
 * {@code hasExperience() && experienceLevel > 0}, and the obvious place to intervene is that
 * {@code hasExperience()} call - which is exactly where <b>Better Mount HUD</b> puts its own
 * {@code @Redirect}, to keep the bar visible while riding a mount. Two redirects cannot share one
 * instruction: the first to apply wins and the other fails its injection check, which for a mixin with
 * {@code require = 1} is a hard crash before the game reaches the title screen. It really did crash
 * that way, on a 163 mod pack - see the handover.
 *
 * <p>So this takes the other route instead: the guard is the <em>first</em> read of
 * {@code LocalPlayer.experienceLevel}, and answering zero there skips the number without touching
 * anything Better Mount HUD or anyone else cares about. It keeps working whichever way that mod
 * decides {@code hasExperience()}, and it still shows the charge at experience level zero, which is
 * when a player is most likely to be flying around with nothing to show for it.
 *
 * <p>With the charge down the field is returned untouched, so nothing about the ordinary experience
 * bar changes.
 *
 * <p>{@code require = 0} is deliberate, and is the one place in this addon that uses it for a vanilla
 * target. This injector exists to stop a number being drawn twice; if some other mod ever claims the
 * same field read, the worst acceptable outcome is that the experience level shows on top of the
 * flight bar. The alternative - failing the injection check and taking the game down before the title
 * screen - is what this whole fix was about, and it is not worth a cosmetic glitch being reported
 * instead.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Redirect(method = "renderHotbarAndDecorations", require = 0,
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I",
                    ordinal = 0))
    private int tbx$chargeInsteadOfLevel(LocalPlayer player) {
        return OmegaFlightClient.barVisible() ? 0 : player.experienceLevel;
    }
}
