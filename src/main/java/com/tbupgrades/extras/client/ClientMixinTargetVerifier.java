package com.tbupgrades.extras.client;

import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.common.MixinTargetVerifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

/**
 * Client half of {@link MixinTargetVerifier}.
 *
 * <p>Also announces the build number in chat once per game launch. The jar file name never changes
 * between deliveries, so without this there is no way to tell which build a player is actually
 * running - which matters a great deal while chasing a report.
 */
public final class ClientMixinTargetVerifier {
    private static final String[] CLIENT_TARGETS = {
            "net.minecraft.client.gui.GuiGraphics",
            "net.minecraft.client.gui.screens.Screen",
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
            "net.minecraft.client.MouseHandler",
            "net.minecraft.client.KeyboardHandler",
            "com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TankWidget"
    };
    private ClientMixinTargetVerifier() {
    }

    public static void run() {
        if (!Boolean.getBoolean("travelersbackpackextras.verifyMixins")) {
            return;
        }
        int loaded = MixinTargetVerifier.load(CLIENT_TARGETS);
        TravelersBackpackExtras.LOGGER.info("[Extra Upgrades] verified {} client mixin targets", loaded);
    }
}
