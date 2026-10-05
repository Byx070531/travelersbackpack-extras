package com.tbupgrades.extras.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.common.OmegaFlightStatePayload;
import com.tbupgrades.extras.common.OmegaFlightTogglePayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * What the client knows about the flight charge, and the key that toggles it.
 *
 * <p>Both halves are here because they answer the same question from opposite ends: the keybind is the
 * only thing that can start a toggle, and the charge is the only thing that can show one happened.
 *
 * <p>The key is {@code F} with Shift held. Shift is part of the combination rather than something the
 * player may drop, because plain {@code F} is vanilla's "swap items with offhand" and silently taking
 * that away would be worse than asking for one more key; the key itself is still rebindable, and Shift
 * only suppresses the offhand swap on whatever key is bound.
 */
public final class OmegaFlightClient {
    /**
     * The bound key. Never read through {@code consumeClick}: {@link
     * com.tbupgrades.extras.client.mixin.KeyboardHandlerMixin} consumes the press itself so that the
     * offhand swap never queues, which means a normal click is never recorded here. It is a real
     * {@link KeyMapping} all the same, because that is what puts it in the controls screen.
     */
    private static final KeyMapping TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.travelersbackpackextras.omega_flight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "main"))));

    private static double charge;
    private static double max = 1.0D;
    private static boolean flying;

    private OmegaFlightClient() {
    }

    public static KeyMapping toggleKey() {
        return TOGGLE_KEY;
    }

    /**
     * Asks the server to flip the omega switch.
     *
     * <p>Guarded by {@code canSend}: on a server without this addon the channel is not declared, and
     * vanilla disconnects clients that send an unregistered payload channel - so pressing the key on
     * such a server has to do nothing at all rather than get the player kicked.
     */
    public static void sendToggle() {
        if (ClientPlayNetworking.canSend(OmegaFlightTogglePayload.TYPE)) {
            ClientPlayNetworking.send(OmegaFlightTogglePayload.INSTANCE);
        }
    }

    /** Whether the experience bar slot should be showing the flight charge instead. */
    public static boolean barVisible() {
        return flying && max > 0.0D;
    }

    /** How much of the charge is left, clamped so a stale packet can never draw outside the bar. */
    public static float fraction() {
        if (max <= 0.0D) {
            return 0.0F;
        }
        double ratio = charge / max;
        if (ratio < 0.0D) {
            return 0.0F;
        }
        return ratio > 1.0D ? 1.0F : (float) ratio;
    }

    /**
     * The charge as the whole number shown where the experience level normally sits - 600 down to 0
     * with the defaults.
     *
     * <p>Rounded rather than truncated so that a full bar reads its maximum instead of one less, and
     * clamped to the maximum so that a stale packet cannot print a number bigger than the bar can be
     * full.
     */
    public static int remaining() {
        double clamped = Math.min(Math.max(charge, 0.0D), max);
        if (clamped <= 0.0D || Double.isNaN(clamped)) {
            return 0;
        }
        return (int) Math.min(Math.round(clamped), Integer.MAX_VALUE);
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(OmegaFlightStatePayload.TYPE,
                (payload, context) -> accept(payload));
        // Leaving the server while flying must not leave the bar behind on the title screen.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private static void accept(OmegaFlightStatePayload payload) {
        charge = payload.charge();
        max = payload.max();
        flying = payload.flying();
    }

    private static void clear() {
        charge = 0.0D;
        max = 1.0D;
        flying = false;
    }
}
