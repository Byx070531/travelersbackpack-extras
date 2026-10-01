package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.common.BackpackActionPayload;
import com.tbupgrades.extras.common.BackpackInteractions;
import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Catches alt + left click as raw mouse input, before any screen or any other mod can see it.
 *
 * <p>Intercepting at {@code AbstractContainerScreen.mouseClicked} was not enough on a large modpack:
 * a container-screen handler registered by another mod (the malilib family, Item Scroller and
 * Inventory Profiles Next all install one) can consume the click first, and then the backpack screen
 * never runs its own click code at all. Mouse input arrives here before any of that, so consuming the
 * event at this point is the only place that cannot be pre-empted.
 *
 * <p>The event is deliberately only consumed for this one combination - left press, alt held, inside
 * a Traveler's Backpack container, over one of the backpack's storage slots. Every other click,
 * including every click while no backpack is open, falls straight through untouched.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /** {@code GLFW_PRESS}. */
    private static final int TBX_PRESS = 1;
    /** {@code GLFW_MOUSE_BUTTON_LEFT}. */
    private static final int TBX_LEFT = 0;
    /** {@code GLFW_MOUSE_BUTTON_RIGHT}. */
    private static final int TBX_RIGHT = 1;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void tbx$altClickRaw(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
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

        if (buttonInfo.button() == TBX_LEFT
                && (buttonInfo.hasAltDown() || minecraft.hasAltDown())) {
            Slot hovered = ((AbstractContainerScreenAccessor) screen).tbx$getHoveredSlot();
            if (hovered == null || !BackpackInteractions.isStorageSlot(menu, hovered.index)) {
                return;
            }
            ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.FILL_INVENTORY, hovered.index));
            ci.cancel();
            return;
        }

        // Shift + right click: one item straight into the inventory. Only taken over while the
        // cursor is empty, which is exactly the condition under which vanilla would turn this click
        // into a QUICK_MOVE; with something on the cursor the vanilla drag behaviour is kept.
        if (buttonInfo.button() == TBX_LEFT && minecraft.hasControlDown()) {
            Slot hovered = ((AbstractContainerScreenAccessor) screen).tbx$getHoveredSlot();
            if (hovered != null && BackpackInteractions.isStorageSlot(menu, hovered.index)) {
                ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.CTRL_SWAP, hovered.index));
                ci.cancel();
                return;
            }
        }        if (buttonInfo.button() == TBX_RIGHT && buttonInfo.hasShiftDown() && menu.getCarried().isEmpty()) {
            Slot hovered = ((AbstractContainerScreenAccessor) screen).tbx$getHoveredSlot();
            if (hovered == null || !BackpackInteractions.isManagedSlot(menu, hovered.index)) {
                return;
            }
            ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.MOVE_ONE, hovered.index));
            ci.cancel();
        }
    }
}
