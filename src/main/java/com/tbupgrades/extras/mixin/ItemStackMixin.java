package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.ModDataComponents;
import com.tbupgrades.extras.api.VirtualStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches the two most widely used {@code ItemStack} operations about oversized stacks.
 *
 * <h2>{@code isSameItemSameComponents}</h2>
 * An oversized stack is a perfectly ordinary stack plus one extra component holding the real amount.
 * That component is bookkeeping, not identity, but every mod that searches a backpack - Traveler's
 * Backpack's feeding, refill, auto-pickup, void and magnet upgrades included - matches stacks with
 * this method. Without the mixin below, stacked food could not be found by the feeding upgrade and
 * stacked items could not be merged by auto-pickup.
 *
 * <h2>{@code shrink}</h2>
 * The physical count of an oversized stack is always 1, so the vanilla implementation
 * ({@code setCount(count - amount)}) drives it to zero and the stack starts reporting itself as empty
 * while the count component still holds hundreds of items - which is exactly how the feeding upgrade
 * managed to make a stack of carrots appear to vanish. Decrementing the logical amount instead keeps
 * the representation valid for every caller, including the ones that shrink a stack without writing
 * it back through the backpack handler.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void tbx$ignoreVirtualCount(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (!VirtualStack.isVirtual(first) && !VirtualStack.isVirtual(second)) {
            return;
        }
        cir.setReturnValue(VirtualStack.sameItemIgnoringCount(first, second));
    }

    /**
     * Restores correct change detection for oversized stacks, which the mixin above would otherwise
     * break.
     *
     * <p>Vanilla's {@code matches} is the game's "is this the same as what I last saw" test, used by
     * the container menu to decide which slots changed and therefore what to send to the client, and
     * by container components to decide whether two contents are equal. It compares the physical
     * counts and then falls back to {@code isSameItemSameComponents} - and because an oversized stack
     * always has a physical count of 1, that fallback is the only thing that could ever notice the
     * real amount changing. Making that comparison count-blind silently told the menu "nothing
     * changed", so the server decremented the stack while the client kept displaying the old number.
     */
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void tbx$matchVirtualCount(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (!VirtualStack.isVirtual(first) && !VirtualStack.isVirtual(second)) {
            return;
        }
        cir.setReturnValue(first.getCount() == second.getCount()
                && VirtualStack.count(first) == VirtualStack.count(second)
                && VirtualStack.sameItemIgnoringCount(first, second));
    }

    @Inject(method = "shrink", at = @At("HEAD"), cancellable = true)
    private void tbx$shrinkLogical(int amount, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (!VirtualStack.isVirtual(self)) {
            return;
        }
        VirtualStack.grow(self, -amount);
        ci.cancel();
    }

    /**
     * {@code split} is built on {@code copyWithCount}, which would hand the caller a one-item stack
     * that still carries the oversized bookkeeping and therefore claims to hold the whole slot.
     */
    @Inject(method = "split", at = @At("RETURN"))
    private void tbx$cleanSplit(int amount, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result != null && result.has(ModDataComponents.VIRTUAL_COUNT)) {
            result.remove(ModDataComponents.VIRTUAL_COUNT);
        }
    }
}
