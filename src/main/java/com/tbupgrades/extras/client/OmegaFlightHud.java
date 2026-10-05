package com.tbupgrades.extras.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws the flight charge into the bar slot at the bottom of the screen.
 *
 * <p>Minecraft 1.21.9 replaced the dedicated experience bar with one shared "contextual bar" slot that
 * the experience bar, the locator bar and the vehicle jump bar take turns using, so there is no
 * experience bar renderer to borrow any more. What there is: whichever of them is showing, its
 * background is drawn by its own {@code renderBackground}, and its contents by its own {@code render}
 * - the locator bar's dots and arrows live in the second half. That split is what makes this work
 * without ever touching which bar is selected:
 *
 * <ul>
 *     <li>take over {@code renderBackground} of both bars, and the flight charge appears whichever of
 *     the two would have been on screen;</li>
 *     <li>leave {@code render} alone, and the locator dots are still drawn on top - so the player
 *     position display is never covered, it simply sits on the charge bar.</li>
 * </ul>
 *
 * <p>The experience level number is drawn by {@code Gui} itself, between those two calls, so taking
 * the bar over means taking that number over as well - otherwise the level would sit on top of the
 * charge. While the bar is up the number becomes the charge counting down instead.
 */
public final class OmegaFlightHud {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");

    /** Charge to spare. */
    private static final int HEALTHY_COLOUR = 0xFF35BEEB;
    /** Running out; the last fifth turns red so the fall is not a surprise. */
    private static final int LOW_COLOUR = 0xFFE8533F;
    private static final float LOW_FRACTION = 0.2F;

    private OmegaFlightHud() {
    }

    /**
     * Draws the charge bar in place of {@code bar}'s own background.
     *
     * @return whether the caller should cancel its own drawing
     */
    public static boolean render(GuiGraphics graphics, ContextualBarRenderer bar) {
        if (!OmegaFlightClient.barVisible()) {
            return false;
        }
        Window window = Minecraft.getInstance().getWindow();
        int left = bar.left(window);
        int top = bar.top(window);
        int height = ContextualBarRenderer.HEIGHT;
        // The vanilla bar texture, so the slot keeps looking like the slot it is; only the fill inside
        // it is ours.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, ContextualBarRenderer.WIDTH, height);

        float fraction = OmegaFlightClient.fraction();
        int filled = Math.round(fraction * (ContextualBarRenderer.WIDTH - 2));
        if (filled > 0) {
            // Inside the texture's one pixel border, and three pixels tall against its five.
            graphics.fill(left + 1, top + 1, left + 1 + filled, top + height - 1,
                    fraction <= LOW_FRACTION ? LOW_COLOUR : HEALTHY_COLOUR);
        }
        // The number above the bar is the charge counting down, not the experience level. Vanilla
        // draws that number itself, between the background and the contents of the bar, so
        // {@link com.tbupgrades.extras.client.mixin.GuiMixin} has to hold it back while this is up.
        // Borrowing vanilla's own routine keeps the position, the outline and the font identical.
        ContextualBarRenderer.renderExperienceLevel(graphics, Minecraft.getInstance().font,
                OmegaFlightClient.remaining());
        return true;
    }
}
