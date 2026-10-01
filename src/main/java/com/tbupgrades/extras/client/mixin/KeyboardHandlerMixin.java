package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.api.TbxTankHover;
import com.tbupgrades.extras.common.BackpackActionPayload;
import com.tbupgrades.extras.common.BackpackInteractions;
import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Catches control + shift + drop as raw keyboard input, for the same reason
 * {@link MouseHandlerMixin} catches alt + left click there: on a large modpack the screen-level key
 * handlers installed by other mods can consume the press first, and then the backpack screen never
 * sees it.
 *
 * <p>Only this one combination is consumed - a press of the player's own drop key with both
 * modifiers held while a Traveler's Backpack container is open - so ordinary key handling, including
 * the plain drop key and control + drop, is untouched and still goes through the normal click
 * pipeline.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    /** {@code GLFW_PRESS}. */
    private static final int TBX_PRESS = 1;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void tbx$dropKeyRaw(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (action != TBX_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (!(screen.getMenu() instanceof AbstractBackpackMenu menu)) {
            return;
        }
        if (!keyEvent.hasControlDown() || !keyEvent.hasShiftDown()) {
            return;
        }
        if (!minecraft.options.keyDrop.matches(keyEvent)) {
            return;
        }

        // Pointer over an endless tank: clear it instead.
        for (GuiEventListener child : ((Screen) screen).children()) {
            if (child instanceof TbxTankHover hover) {
                int tank = hover.tbx$hoveredTank();
                if (tank == 0 || tank == 1) {
                    ClientPlayNetworking.send(new BackpackActionPayload(tank == 0
                            ? BackpackActionPayload.Action.CLEAR_LEFT_TANK
                            : BackpackActionPayload.Action.CLEAR_RIGHT_TANK, 0));
                    ci.cancel();
                    return;
                }
            }
        }

        Slot hovered = ((AbstractContainerScreenAccessor) screen).tbx$getHoveredSlot();
        if (hovered == null || !BackpackInteractions.isManagedSlot(menu, hovered.index)) {
            return;
        }
        ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.DROP_BIG, hovered.index));
        ci.cancel();
    }
}
