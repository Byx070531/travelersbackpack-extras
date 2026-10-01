package com.tbupgrades.extras.client.mixin;

import com.tiviacz.travelersbackpack.handlers.KeybindHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets creative mode keep its middle click inside a backpack.
 *
 * <p>Traveler's Backpack binds its sort action to the middle mouse button and consumes it in
 * {@code BackpackScreen.mouseClicked} through {@code SORT_BACKPACK.matchesMouse(event)}, which is why
 * middle-clicking anything while a backpack is open sorted instead of picking the item - the vanilla
 * creative pick-block never saw the click, not even for the player's own inventory slots.
 *
 * <p>The two uses never overlap: creative pick-block only exists in creative mode, and sorting is
 * still available there through the backpack's own sort button. Everywhere else the keybind behaves
 * exactly as before, so survival players keep sorting with the middle button.
 */
@Mixin(KeyMapping.class)
public abstract class SortKeyMappingMixin {
    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void tbx$creativePickBlock(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != KeybindHandler.SORT_BACKPACK) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.getAbilities().instabuild) {
            return;
        }
        if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        cir.setReturnValue(false);
    }
}