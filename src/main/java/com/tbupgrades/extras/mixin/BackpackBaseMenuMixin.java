package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.common.BackpackInteractions;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Traveler's Backpack replaces {@code AbstractContainerMenu.moveItemStackTo} with its own
 * implementation (to honour memory and unsortable slots), so injecting into the vanilla method would
 * never run for a backpack. This is the copy that actually executes.
 *
 * <p>Vanilla (and the mod's rewrite) decide whether two stacks may merge with
 * {@code ItemStack.isSameItemSameComponents}, which is never true for an oversized stack because of
 * its extra count component. Merging into a boosted slot therefore has to happen here, through the
 * backpack handler, which is the only place that knows the boosted slot limit.
 */
@Mixin(BackpackBaseMenu.class)
public abstract class BackpackBaseMenuMixin {
    @Unique
    private boolean tbx$mergedIntoBackpack;

    @Inject(method = "moveItemStackTo", at = @At("HEAD"))
    private void tbx$preMerge(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection,
                              CallbackInfoReturnable<Boolean> cir) {
        this.tbx$mergedIntoBackpack = BackpackInteractions.mergeIntoBackpack(
                (BackpackBaseMenu) (Object) this, stack, startIndex, endIndex);
    }

    /**
     * The mod's rewrite reports {@code false} when its own loops moved nothing, which is exactly the
     * case once the pre-merge above emptied the stack. Reporting {@code true} keeps callers such as
     * {@code quickMoveStack} from returning early without writing the (now reduced) stack back into
     * the source slot, which would duplicate items.
     */
    @Inject(method = "moveItemStackTo", at = @At("RETURN"), cancellable = true)
    private void tbx$postMerge(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection,
                               CallbackInfoReturnable<Boolean> cir) {
        if (this.tbx$mergedIntoBackpack && !cir.getReturnValue()) {
            cir.setReturnValue(true);
        }
        this.tbx$mergedIntoBackpack = false;
    }
}
