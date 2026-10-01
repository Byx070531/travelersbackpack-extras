package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.TbxHandlerOwner;
import com.tbupgrades.extras.api.VirtualStack;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches the backpack's item handler about oversized stacks.
 *
 * <p>Vanilla clamps a slot at {@code min(getSlotLimit(), stack.getMaxStackSize())}, which caps every
 * backpack slot at 64 no matter what. When a storage upgrade is installed the slot limit becomes
 * {@code itemMax * multiplier}, and amounts above the vanilla maximum are carried in
 * {@link com.tbupgrades.extras.api.ModDataComponents#VIRTUAL_COUNT}.
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin implements TbxHandlerOwner {
    @Shadow
    public abstract ItemStack getStackInSlot(int slot);

    @Shadow
    public abstract void setStackInSlot(int slot, ItemStack stack);

    @Unique
    private BackpackWrapper tbx$wrapper;

    @Unique
    private boolean tbx$storage;

    @Override
    public BackpackWrapper tbx$getWrapper() {
        return this.tbx$wrapper;
    }

    @Override
    public void tbx$setWrapper(BackpackWrapper wrapper) {
        this.tbx$wrapper = wrapper;
    }

    @Override
    public boolean tbx$isStorage() {
        return this.tbx$storage;
    }

    /** Called from {@code BackpackWrapperMixin} once the handler has been built. */
    @Override
    public void tbx$markStorage() {
        this.tbx$storage = true;
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void tbx$insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.isEmpty() || this.tbx$wrapper == null || !this.tbx$storage) {
            return;
        }
        ItemStack existing = this.getStackInSlot(slot);
        if (existing.isEmpty() || !VirtualStack.mergeable(existing, stack)) {
            return;
        }
        long limit = CapacityHelper.slotLimit(this.tbx$wrapper, existing);
        long logical = VirtualStack.count(existing);
        long space = limit - logical;
        if (space <= 0L) {
            cir.setReturnValue(stack);
            return;
        }
        long accepted = Math.min(space, stack.getCount());
        if (!simulate) {
            VirtualStack.setCount(existing, logical + accepted);
            this.setStackInSlot(slot, existing);
        }
        if (accepted >= stack.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
        } else {
            cir.setReturnValue(stack.copyWithCount((int) (stack.getCount() - accepted)));
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void tbx$extractItem(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (amount <= 0) {
            return;
        }
        ItemStack existing = this.getStackInSlot(slot);
        if (!VirtualStack.isVirtual(existing)) {
            return;
        }
        long logical = VirtualStack.count(existing);
        long taken = Math.min(amount, logical);
        if (taken <= 0L) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack result = VirtualStack.copyWithCount(existing, taken);
        if (!simulate) {
            long left = logical - taken;
            if (left <= 0L) {
                this.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                VirtualStack.setCount(existing, left);
                this.setStackInSlot(slot, existing);
            }
        }
        cir.setReturnValue(result);
    }

    /**
     * The vanilla {@code getStackLimit} caps a slot at the item's maximum stack size; report the
     * boosted limit instead so that shift-clicking and hopper-style transfers keep filling a slot.
     * Callers subtract the amount already present themselves.
     */
    @Inject(method = "getStackLimit", at = @At("HEAD"), cancellable = true)
    private void tbx$getStackLimit(int slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (this.tbx$wrapper == null || !this.tbx$storage || stack.isEmpty()) {
            return;
        }
        long limit = CapacityHelper.slotLimit(this.tbx$wrapper, stack);
        cir.setReturnValue((int) Math.min(Integer.MAX_VALUE, limit));
    }
}
