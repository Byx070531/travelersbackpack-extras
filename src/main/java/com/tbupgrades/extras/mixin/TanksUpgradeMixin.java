package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.InfiniteTank;
import com.tbupgrades.extras.api.InfiniteTanks;
import com.tiviacz.travelersbackpack.components.RenderInfo;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.FluidTank;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TanksUpgrade;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the tanks informed about whether their backpack has reached the tier that turns fluids
 * endless.
 *
 * <p>{@code setTanksCapacity} is the single point every tank goes through - it runs when the tank
 * upgrade is built, both on the server and on the client from the synchronised components - so it is
 * the right place to push the flag down.
 */
@Mixin(TanksUpgrade.class)
public abstract class TanksUpgradeMixin {
    @Shadow
    public abstract FluidTank getLeftTank();

    @Shadow
    public abstract FluidTank getRightTank();

    @Inject(method = "setTanksCapacity", at = @At("TAIL"))
    private void tbx$updateInfiniteFlag(CallbackInfo ci) {
        // getUpgradeManager() lives on UpgradeBase, which @Shadow cannot reach.
        UpgradeManager manager = ((UpgradeBase<?>) (Object) this).getUpgradeManager();
        BackpackWrapper wrapper = manager == null ? null : manager.getWrapper();
        boolean enabled = CapacityHelper.hasInfiniteFluidTier(wrapper);
        if (this.getLeftTank() instanceof InfiniteTank left) {
            left.tbx$setEnabled(enabled);
        }
        if (this.getRightTank() instanceof InfiniteTank right) {
            right.tbx$setEnabled(enabled);
        }
    }

    /**
     * The render copy of the tank data (used by the backpack HUD, the radial menu and the item model)
     * is built from {@code RenderInfo}, not from the live tanks, and those consumers construct their
     * own throw-away {@code FluidTank}s that never learn about the endless flag. Publishing the full
     * capacity as the amount keeps every one of them showing a full tank.
     */
    @Inject(method = "writeToRenderData", at = @At("TAIL"))
    private void tbx$endlessRenderData(CompoundTag tag, CallbackInfo ci) {
        tbx$writeEndless(tag, RenderInfo.LEFT_TANK, this.getLeftTank());
        tbx$writeEndless(tag, RenderInfo.RIGHT_TANK, this.getRightTank());
    }

    @Unique
    private static void tbx$writeEndless(CompoundTag tag, String key, FluidTank tank) {
        if (tank == null || !InfiniteTanks.isInfinite(tank)) {
            return;
        }
        long capacity = tank.getCapacity();
        if (capacity <= 0L) {
            return;
        }
        tag.put(key, tank.getFluid().copyWithAmount(capacity).saveOptional());
    }
}
