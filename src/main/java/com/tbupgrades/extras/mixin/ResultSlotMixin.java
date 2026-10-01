package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.init.ModItems;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the elytra template in the grid when the elytra recipe is crafted.
 *
 * <p>The recipe needs an elytra in its middle slot, so the elytra is a prerequisite - but a craft must
 * not eat it, or the recipe can only ever be used once. 1.21 has no crafting remainder ("this
 * ingredient comes back") and a result slot cannot hold two elytras, since an elytra is damageable and
 * vanilla refuses to make it stackable.
 *
 * <p>The craft consumes its ingredients in {@code ResultSlot.onTake}, which walks the grid and removes
 * one item from every occupied slot. This redirects that removal and declines it for the template, so
 * the elytra is never taken in the first place - no refill, no timing, nothing to race with. Both
 * sides run the same code, so client and server agree.
 *
 * <p>Scoped to this recipe by the dragon scale in the grid, so the enchanted diamond golden apple still
 * consumes its elytra.
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Redirect(method = "onTake",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack tbx$keepElytraTemplate(CraftingContainer grid, int index, int amount) {
        ItemStack inSlot = grid.getItem(index);
        if (inSlot.is(Items.ELYTRA) && tbx$craftedWithDragonScale(grid)) {
            return ItemStack.EMPTY;
        }
        return grid.removeItem(index, amount);
    }

    private static boolean tbx$craftedWithDragonScale(CraftingContainer grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (grid.getItem(i).is(ModItems.DRAGON_SCALE)) {
                return true;
            }
        }
        return false;
    }
}