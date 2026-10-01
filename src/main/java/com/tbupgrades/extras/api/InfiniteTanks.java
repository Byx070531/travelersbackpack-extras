package com.tbupgrades.extras.api;

import com.tiviacz.travelersbackpack.inventory.FluidTank;

/** Null-safe helpers for the endless tank behaviour. */
public final class InfiniteTanks {
    private InfiniteTanks() {
    }

    public static InfiniteTank of(FluidTank tank) {
        return tank instanceof InfiniteTank infinite ? infinite : null;
    }

    public static boolean isInfinite(FluidTank tank) {
        InfiniteTank infinite = of(tank);
        return infinite != null && infinite.tbx$isInfinite();
    }

    public static boolean clear(FluidTank tank) {
        InfiniteTank infinite = of(tank);
        return infinite != null && infinite.tbx$clear();
    }
}
