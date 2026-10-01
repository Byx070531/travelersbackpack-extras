package com.tbupgrades.extras.api;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

/**
 * Implemented (via mixin) by Traveler's Backpack's {@code FluidTank} when this addon is present.
 */
public interface InfiniteTank {
    /**
     * Turns the endless behaviour on or off for this tank. Traveler's Backpack only changes how
     * fluids are handled from the netherite upgrade upwards, so the tank has to be told whether its
     * backpack qualifies.
     */
    void tbx$setEnabled(boolean enabled);

    /** True when the tank currently behaves as an endless source of its fluid. */
    boolean tbx$isInfinite();

    /** The fluid this tank provides endlessly, or {@code null} when it is a normal tank. */
    FluidVariant tbx$infiniteFluid();

    /** Empties the tank outright, which also removes the endless state. */
    boolean tbx$clear();
}
