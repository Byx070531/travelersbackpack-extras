package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.InfiniteTanks;
import com.tiviacz.travelersbackpack.inventory.FluidTank;
import com.tiviacz.travelersbackpack.item.HoseItem;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the hose suck fluid back out of an endless tank.
 *
 * <p>The hose's suction branch only fires when the picked-up fluid still fits, i.e.
 * {@code bucket + tankAmount <= tank.getCapacity()}. An endless tank reports itself as full, so that
 * test can never pass and fluid poured into the world by mistake could never be recovered.
 *
 * <p>An endless tank can always take its own fluid back - the stored amount does not change - so the
 * capacity it reports for this one comparison is widened by a bucket. {@code getCapacity()} has
 * exactly one live call site in this class (the milk branch next to it is commented out), which makes
 * this redirection unambiguous.
 */
@Mixin(HoseItem.class)
public abstract class HoseItemMixin {
    @Redirect(method = "use",
            at = @At(value = "INVOKE", target = "Lcom/tiviacz/travelersbackpack/inventory/FluidTank;getCapacity()J"))
    private long tbx$endlessRoomForSuckBack(FluidTank tank) {
        long capacity = tank.getCapacity();
        if (!InfiniteTanks.isInfinite(tank)) {
            return capacity;
        }
        return Math.max(capacity, tank.getFluidAmount() + FluidConstants.BUCKET);
    }
}
