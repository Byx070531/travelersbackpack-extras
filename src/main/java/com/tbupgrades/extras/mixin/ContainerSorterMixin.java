package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.VirtualStack;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.sorter.ContainerSorter;
import com.tiviacz.travelersbackpack.inventory.sorter.SortSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the backpack's sort button work with oversized stacks.
 *
 * <p>The sorter collects every storage stack into a list, merges the entries, sorts the list and
 * writes it back one slot per entry. Both merge helpers only understand vanilla stacks:
 *
 * <ul>
 *     <li>{@code canMergeItems} compares with {@code ItemStack.isSameItemSameComponents}, which is
 *     never true for an oversized stack because of the extra count component, and it treats a stack
 *     as full once its <em>physical</em> count reaches 64 - which an oversized stack (physical count
 *     1, real amount in the component) never does;</li>
 *     <li>{@code combineStacks} adds {@code getCount()} to {@code getCount()}, so merging an
 *     oversized stack would write a physical count of 65 onto a stack that is supposed to hold 1 item
 *     plus a component - and it caps the result at the item's own maximum of 64 instead of the
 *     backpack's boosted slot limit.</li>
 * </ul>
 *
 * <p>Left alone, that ends in either a corrupted stack or a backpack where identical items sit in
 * several slots and are never combined. Both helpers are replaced here with arithmetic on the
 * logical amount that respects the boosted per slot limit, so sorting a backpack merges and packs
 * oversized stacks exactly like ordinary ones.
 */
@Mixin(ContainerSorter.class)
public abstract class ContainerSorterMixin {
    /**
     * The backpack being sorted. {@code combineStacks} and {@code canMergeItems} are static helpers
     * without access to the backpack, but they need its multiplier to know how much fits in a slot,
     * so the wrapper is picked up from the entry point and dropped again on the way out.
     */
    @Unique
    private static BackpackWrapper tbx$activeWrapper;

    @Inject(method = "sortBackpack", at = @At("HEAD"))
    private static void tbx$captureWrapper(BackpackWrapper wrapper, Player player, SortSelector.SortType sortType,
                                           boolean setNextSortType, CallbackInfo ci) {
        tbx$activeWrapper = wrapper;
    }

    @Inject(method = "sortBackpack", at = @At("RETURN"))
    private static void tbx$releaseWrapper(BackpackWrapper wrapper, Player player, SortSelector.SortType sortType,
                                           boolean setNextSortType, CallbackInfo ci) {
        tbx$activeWrapper = null;
    }

    /**
     * How many items this stack may hold in one slot: the item's own stack size times the backpack's
     * multiplier. An amount that is already above that (a backpack whose storage upgrade was removed
     * before this rule existed) is never lowered - sorting must not delete items - it simply stops
     * the stack from growing any further.
     */
    @Unique
    private static long tbx$slotLimit(ItemStack stack) {
        long limit = tbx$activeWrapper == null
                ? VirtualStack.baseMax(stack)
                : CapacityHelper.slotLimit(tbx$activeWrapper, stack);
        return Math.max(limit, VirtualStack.count(stack));
    }

    @Unique
    private static boolean tbx$hasRoom(ItemStack stack) {
        return !stack.isEmpty() && VirtualStack.count(stack) < tbx$slotLimit(stack);
    }

    /**
     * Traveler's Backpack skips the merge entirely when the stack it is about to file away is full,
     * judged with {@code getCount() == getMaxStackSize()}. In an upgraded backpack a plain stack of
     * 64 is not full - the slot may hold hundreds - so that test would leave every ordinary full
     * stack sitting next to the oversized pile of the same item, never combined. Comparing against
     * the boosted limit instead restores the intended behaviour, and for a backpack without an
     * upgrade the limit is still the item's own 64, so nothing changes there.
     */
    @Redirect(method = "addStackWithMerge",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int tbx$boostedStackSize(ItemStack stack) {
        return (int) Math.min(Integer.MAX_VALUE, tbx$slotLimit(stack));
    }

    /** Replaces vanilla's physical count comparison with the logical one. */
    @Inject(method = "canMergeItems", at = @At("HEAD"), cancellable = true)
    private static void tbx$canMergeItems(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (first.isEmpty() || second.isEmpty() || !first.isStackable() || !second.isStackable()) {
            cir.setReturnValue(false);
            return;
        }
        if (!VirtualStack.mergeable(first, second)) {
            cir.setReturnValue(false);
            return;
        }
        // Vanilla also refuses when either stack looks full. For an oversized stack "full" is not
        // 64 but the boosted limit, and a merge into a stack that has no room left is a no-op
        // anyway, so asking whether either side has room is both safe and enough.
        cir.setReturnValue(tbx$hasRoom(first) || tbx$hasRoom(second));
    }

    /**
     * Merges {@code existing} into {@code incoming} (Traveler's Backpack's argument order), moving
     * only as many items as the boosted slot limit allows and never touching a physical count.
     */
    @Inject(method = "combineStacks", at = @At("HEAD"), cancellable = true)
    private static void tbx$combineStacks(ItemStack incoming, ItemStack existing, CallbackInfo ci) {
        ci.cancel();
        if (incoming.isEmpty() || existing.isEmpty()) {
            return;
        }
        long space = tbx$slotLimit(incoming) - VirtualStack.count(incoming);
        if (space <= 0L) {
            return;
        }
        VirtualStack.transfer(incoming, existing, space);
    }
}
