package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.TbxHandlerOwner;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into {@code BackpackWrapper} to
 * <ol>
 *     <li>tag the main storage handler so it knows its owning backpack, and</li>
 *     <li>scale the backpack's tank capacity by the installed storage multiplier.</li>
 * </ol>
 *
 * <p>The tank capacity is scaled at the getter because every consumer of it - the tank upgrade, the
 * bucket transfer actions and the screen warning - reads it through {@code getBackpackTankCapacity}.
 */
@Mixin(BackpackWrapper.class)
public abstract class BackpackWrapperMixin {
    @Inject(method = "createHandler", at = @At("RETURN"))
    private void tbx$attachWrapper(NonNullList<ItemStack> stacks, int dataId, CallbackInfoReturnable<ItemStackHandler> cir) {
        ItemStackHandler handler = cir.getReturnValue();
        if (handler instanceof TbxHandlerOwner owner) {
            owner.tbx$setWrapper((BackpackWrapper) (Object) this);
            if (dataId == BackpackWrapper.STORAGE_ID) {
                owner.tbx$markStorage();
            }
        }
    }

    @Inject(method = "getBackpackTankCapacity", at = @At("RETURN"), cancellable = true)
    private void tbx$scaleTankCapacity(CallbackInfoReturnable<Long> cir) {
        long base = cir.getReturnValue();
        long multiplier = CapacityHelper.multiplier((BackpackWrapper) (Object) this);
        if (multiplier > 1L) {
            cir.setReturnValue(CapacityHelper.scaleFluidCapacity(base, multiplier));
        }
    }
}
