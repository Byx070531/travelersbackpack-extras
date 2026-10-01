package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.upgrades.feeding.FeedingUpgrade;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the feeding upgrade actually consume an oversized food stack.
 *
 * <p>The upgrade eats a single item by copying the slot stack, dropping the copy to one item, using
 * it, and then placing whatever the food left behind back into the backpack:
 *
 * <pre>
 *   ItemStack singleItemCopy = stack.copy();     // still carries the oversized count component
 *   singleItemCopy.setCount(1);                  // only the physical count is lowered
 *   ... eat ...
 *   stack.shrink(1);                             // logical amount drops by one
 *   ItemStack resultItem = singleItemCopy.finishUsingItem(...);
 *   if (!resultItem.isEmpty()) { insert it back into the backpack }
 * </pre>
 *
 * <p>Because the copy keeps the count component it still claims to hold the whole stack, so after
 * eating it is not empty, the upgrade treats it as a leftover and inserts it back - adding exactly
 * one item again. The amount therefore oscillates: 189 to 188 on the bite, back to 189 on the
 * leftover. That is what made the number appear frozen.
 *
 * <p>Stripping the component from the copy makes it a genuine single item: eating consumes it, there
 * is no leftover, and the decrement sticks.
 */
@Mixin(FeedingUpgrade.class)
public abstract class FeedingUpgradeMixin {
    @Redirect(method = "tryFeedingStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack tbx$plainMealCopy(ItemStack original) {
        ItemStack copy = original.copy();
        if (copy.has(ModDataComponents.VIRTUAL_COUNT)) {
            copy.remove(ModDataComponents.VIRTUAL_COUNT);
        }
        return copy;
    }
}
