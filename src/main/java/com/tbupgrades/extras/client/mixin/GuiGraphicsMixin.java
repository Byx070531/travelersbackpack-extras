package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.api.CompactNumbers;
import com.tbupgrades.extras.api.VirtualStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the abbreviated amount of an oversized stack ({@code 0.3k}, {@code 46k}, {@code 2.1B})
 * instead of the raw number, which for an oversized stack would be a meaningless {@code 1}.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "renderItemCount", at = @At("HEAD"), cancellable = true)
    private void tbx$renderItemCount(Font font, ItemStack stack, int x, int y, String override, CallbackInfo ci) {
        if (!VirtualStack.isVirtual(stack)) {
            return;
        }
        String text = override != null ? override : CompactNumbers.format(VirtualStack.count(stack));
        GuiGraphics self = (GuiGraphics) (Object) this;
        self.drawString(font, text, x + 19 - 2 - font.width(text), y + 6 + 3, -1, true);
        ci.cancel();
    }
}
