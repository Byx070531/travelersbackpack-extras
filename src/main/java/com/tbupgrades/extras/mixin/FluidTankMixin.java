package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.InfiniteTank;
import com.tiviacz.travelersbackpack.inventory.FluidTank;
import com.tiviacz.travelersbackpack.inventory.FluidVariantWrapper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Implements the "infinite fluid" behaviour of the netherite upgrade and above.
 *
 * <p>A tank that holds more than two buckets of water, or more than ten thousand buckets of lava,
 * becomes an endless source of that fluid. Potions, milk and anything else are never affected.
 *
 * <p>The state is derived purely from the fluid type and the stored amount, so it needs no extra
 * persistence: it survives saving, loading and client synchronisation for free, and emptying the
 * tank with the clear action removes it.
 *
 * <p>An endless tank never loses fluid to a normal extraction, but a request that asks for the
 * whole capacity at once is treated as a deliberate "empty this tank" action.
 */
@Mixin(FluidTank.class)
public abstract class FluidTankMixin implements InfiniteTank {
    /** More than two buckets of water turns the tank into an endless water source. */
    @Unique
    private static final long TBX_WATER_THRESHOLD = 2000L;
    /** More than ten thousand buckets of lava turns the tank into an endless lava source. */
    @Unique
    private static final long TBX_LAVA_THRESHOLD = 10_000L * 1000L;

    @Shadow
    protected FluidVariantWrapper fluidVariant;

    @Shadow
    public abstract long getCapacity();

    @Shadow
    protected abstract void onContentsChanged();

    @Shadow
    public abstract void setFluid(FluidVariantWrapper stack);

    /** Set from {@code TanksUpgradeMixin}: only netherite and above change fluid handling. */
    @Unique
    private boolean tbx$infiniteEnabled;

    @Override
    public void tbx$setEnabled(boolean enabled) {
        this.tbx$infiniteEnabled = enabled;
    }

    /** True when this tank currently behaves as an endless source. */
    @Override
    public boolean tbx$isInfinite() {
        if (!this.tbx$infiniteEnabled) {
            return false;
        }
        if (this.fluidVariant == null || this.fluidVariant.isEmpty()) {
            return false;
        }
        FluidVariant variant = this.fluidVariant.fluidVariant();
        long amount = this.fluidVariant.getAmount();
        if (isWater(variant)) {
            return amount > TBX_WATER_THRESHOLD;
        }
        if (isLava(variant)) {
            return amount > TBX_LAVA_THRESHOLD;
        }
        return false;
    }

    /** Both the still and the flowing fluid count as water; pipes may carry either. */
    @Unique
    private static boolean isWater(FluidVariant variant) {
        return variant.isOf(Fluids.WATER) || variant.isOf(Fluids.FLOWING_WATER);
    }

    @Unique
    private static boolean isLava(FluidVariant variant) {
        return variant.isOf(Fluids.LAVA) || variant.isOf(Fluids.FLOWING_LAVA);
    }

    /** The fluid an endless tank is full of, or {@code null}. */
    @Override
    public FluidVariant tbx$infiniteFluid() {
        return tbx$isInfinite() ? this.fluidVariant.fluidVariant() : null;
    }

    /** Empties the tank outright; used by the control + shift + drop clearing action. */
    @Override
    public boolean tbx$clear() {
        if (this.fluidVariant == null || this.fluidVariant.isEmpty()) {
            return false;
        }
        this.setFluid(FluidVariantWrapper.blank());
        this.onContentsChanged();
        return true;
    }

    @Inject(method = "fill(Lcom/tiviacz/travelersbackpack/inventory/FluidVariantWrapper;ZLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$blockFill(FluidVariantWrapper resource, boolean simulate, TransactionContext transaction,
                               CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite() && !resource.isEmpty()
                && resource.fluidVariant().isOf(this.fluidVariant.fluidVariant().getFluid())) {
            cir.setReturnValue(0L);
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$blockInsert(FluidVariant insertedVariant, long maxAmount, TransactionContext transaction,
                                 CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite() && insertedVariant.isOf(this.fluidVariant.fluidVariant().getFluid())) {
            cir.setReturnValue(0L);
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$endlessExtract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction,
                                    CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite() && extractedVariant.isOf(this.fluidVariant.fluidVariant().getFluid())) {
            cir.setReturnValue(Math.min(maxAmount, Math.max(this.getCapacity(), 1L)));
        }
    }

    @Inject(method = "drain(JZLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Lcom/tiviacz/travelersbackpack/inventory/FluidVariantWrapper;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$endlessDrain(long maxDrain, boolean simulate, TransactionContext transaction,
                                  CallbackInfoReturnable<FluidVariantWrapper> cir) {
        if (!tbx$isInfinite() || maxDrain <= 0L) {
            return;
        }
        long capacity = Math.max(this.getCapacity(), 1L);
        boolean clear = maxDrain >= capacity;
        FluidVariantWrapper drained = this.fluidVariant.copyWithAmount(Math.min(maxDrain, capacity));
        if (!simulate && clear) {
            this.setFluid(FluidVariantWrapper.blank());
            this.onContentsChanged();
        }
        cir.setReturnValue(drained);
    }

    /**
     * An endless tank reports itself as full so the tank renders full, the tooltip is sane and
     * {@code StorageUtil.move} offers its whole capacity instead of the frozen stored amount.
     *
     * <p>Falls back to the stored amount when the capacity is zero: {@code RenderHelper} divides by
     * the capacity, so reporting a non-zero amount for a zero-capacity tank would be a crash.
     */
    @Inject(method = "getFluidAmount", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$fullAmount(CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite()) {
            long capacity = this.getCapacity();
            cir.setReturnValue(capacity > 0L ? capacity : this.fluidVariant.getAmount());
        }
    }

    /**
     * {@code SingleVariantStorage.getAmount()} backs the transfer API's view of the tank, and
     * {@code StorageUtil.move} caps what it asks for by it. Reporting the frozen stored amount would
     * mean a bucket could only ever be filled up to that leftover amount, so an endless tank reports
     * its full capacity instead.
     */
    @Inject(method = "getAmount", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$fullTransferAmount(CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite()) {
            long capacity = this.getCapacity();
            cir.setReturnValue(capacity > 0L ? capacity : this.fluidVariant.getAmount());
        }
    }

    @Inject(method = "getSpace", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$noSpace(CallbackInfoReturnable<Long> cir) {
        if (tbx$isInfinite()) {
            cir.setReturnValue(0L);
        }
    }

    /**
     * An endless tank must stay endless.
     *
     * <p>Several code paths - most importantly the fluid slot handler in {@code InventoryActions},
     * which works on a throw-away copy of the tank and writes the result back with {@code setFluid} -
     * bypass {@code fill}/{@code drain} entirely. Without this guard, filling a single bucket would
     * drop the stored amount to the threshold and silently end the endless state. Writing empty (the
     * clear action) or a different fluid is still allowed.
     */
    @Inject(method = "setFluid", at = @At("HEAD"), cancellable = true, remap = false)
    private void tbx$keepEndless(FluidVariantWrapper stack, CallbackInfo ci) {
        if (stack == null || stack.isEmpty() || this.fluidVariant == null || this.fluidVariant.isEmpty()) {
            return;
        }
        if (tbx$isInfinite() && stack.fluidVariant().isOf(this.fluidVariant.fluidVariant().getFluid())) {
            ci.cancel();
        }
    }
}
