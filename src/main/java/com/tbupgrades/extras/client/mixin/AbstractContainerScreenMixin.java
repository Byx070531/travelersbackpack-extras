package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.api.TbxTankHover;
import com.tbupgrades.extras.common.BackpackActionPayload;
import com.tbupgrades.extras.common.BackpackInteractions;
import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts the two interactions that vanilla's click types cannot carry: the alt-click inventory
 * fill and the control + shift + drop actions. Everything else is left to the vanilla handling,
 * which the server side then processes through
 * {@link com.tbupgrades.extras.mixin.AbstractContainerMenuMixin}.
 *
 * <p>The alt-click is caught in {@code mouseClicked} rather than in {@code slotClicked} on purpose:
 * {@code slotClicked} is also the entry point other mods use to drive slot interaction
 * programmatically - Mouse Tweaks, for example, calls it directly for every item its wheel and drag
 * features move - and those synthetic clicks must never be reinterpreted as a player request.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tbx$altClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // The modifier state carried by the click itself is the reliable source; polling the global
        // key state is only a fallback, because another handler may already have consumed the key.
        // Control + the transfer button: the button is not a slot, so an empty hovered slot plus
        // Control identifies it. The click is deliberately NOT consumed - Traveler's Backpack still
        // gets to do its own thing, and its transfer already leaves oversized stacks alone.
        // Shift + left click with no slot under the pointer - that is the transfer button: move only the
        // first oversized stack. The click is consumed on purpose so Traveler's Backpack does not run
        // its own transfer as well, which would sweep the ordinary stacks along with it.
        if (event.button() == 0 && Minecraft.getInstance().hasShiftDown()
                && this.tbx$menu() instanceof AbstractBackpackMenu
                && this.tbx$overTransferToPlayerButton(event.x(), event.y())) {
            ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.TRANSFER_OVERSIZED, 0));
            cir.setReturnValue(true);
        }

        if (event.button() != 0 || !(event.hasAltDown() || Minecraft.getInstance().hasAltDown())) {
            return;
        }
        if (!(this.tbx$menu() instanceof AbstractBackpackMenu menu)) {
            return;
        }
        Slot hovered = this.hoveredSlot;
        if (hovered == null || !BackpackInteractions.isStorageSlot(menu, hovered.index)) {
            return;
        }
        ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.FILL_INVENTORY, hovered.index));
        cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tbx$dropKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.tbx$menu() instanceof AbstractBackpackMenu menu)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyDrop.matches(event)) {
            return;
        }
        if (!Minecraft.getInstance().hasControlDown() || !event.hasShiftDown()) {
            return;
        }

        // The pointer is over an endless tank: clear it.
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof TbxTankHover hover) {
                int tank = hover.tbx$hoveredTank();
                if (tank == 0) {
                    ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.CLEAR_LEFT_TANK, 0));
                    cir.setReturnValue(true);
                    return;
                }
                if (tank == 1) {
                    ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.CLEAR_RIGHT_TANK, 0));
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // Otherwise it is the "drop 1024" action on a managed storage slot.
        if (this.hoveredSlot != null
                && BackpackInteractions.isManagedSlot(menu, this.hoveredSlot.index)) {
            ClientPlayNetworking.send(new BackpackActionPayload(BackpackActionPayload.Action.DROP_BIG, this.hoveredSlot.index));
            cir.setReturnValue(true);
        }
    }


    private net.minecraft.world.inventory.AbstractContainerMenu tbx$menu() {
        return ((AbstractContainerScreen<?>) (Object) this).getMenu();
    }
    /**
     * True only over Traveler's Backpack's own "transfer to inventory" button.
     *
     * <p>The widget is looked up among the screen's children rather than through
     * {@code BackpackScreen.sortingButtons}, which is not guaranteed to be assigned. Its position comes
     * from the widget itself, and the hit test is Traveler's Backpack's own, so the button layout stays
     * correct even if its GUI moves.
     */
    private boolean tbx$overTransferToPlayerButton(double mouseX, double mouseY) {
        for (net.minecraft.client.gui.components.events.GuiEventListener child
                : ((net.minecraft.client.gui.screens.Screen) (Object) this).children()) {
            if (child instanceof com.tiviacz.travelersbackpack.client.screens.widgets.SortingButtons buttons
                    && buttons.isButtonHovered(buttons.getPos(), (int) mouseX, (int) mouseY,
                            com.tiviacz.travelersbackpack.client.screens.widgets.SortingButtons.Buttons.TRANSFER_TO_PLAYER)) {
                return true;
            }
        }
        return false;
    }}