package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.VirtualStack;
import com.tiviacz.travelersbackpack.inventory.sorter.SortSelector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The "sort by amount" mode totals up how many items of each type the backpack holds, and the name
 * mode orders same-named items by how many are on the stack. Both add {@code ItemStack.getCount()},
 * which for an oversized stack is the physical 1 rather than the real amount, so a slot holding 189
 * items counted as a single one and the order came out wrong.
 */
@Mixin(SortSelector.class)
public abstract class SortSelectorMixin {
    @Redirect(method = "calculateCount",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"))
    private static int tbx$logicalCount(ItemStack stack) {
        return (int) Math.min(Integer.MAX_VALUE, VirtualStack.count(stack));
    }

    /**
     * The name mode builds a key out of {@code 9999 - count} so that fuller stacks come first. The
     * count is clamped into that range, which keeps ordinary stacks exactly as they were, puts any
     * oversized stack in the "biggest pile first" position and avoids the overflow a raw amount in
     * the billions would cause.
     */
    @Redirect(method = "stackSize",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"))
    private static int tbx$logicalCountForName(ItemStack stack) {
        return (int) Math.min(9999L, VirtualStack.count(stack));
    }
}
