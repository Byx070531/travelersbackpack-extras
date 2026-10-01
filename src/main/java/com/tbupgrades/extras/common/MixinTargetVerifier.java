package com.tbupgrades.extras.common;

import com.tbupgrades.extras.TravelersBackpackExtras;

/**
 * Dev-time helper: Mixin is applied lazily when a class is first loaded, so a wrong selector would
 * otherwise stay hidden until some player happens to trigger the right code path. When a run sets
 * {@code travelersbackpackextras.verifyMixins} (client) or {@code travelersbackpackextras.selfTest}
 * (server) every target this addon mixes into is loaded during startup, which makes configuration
 * mistakes fail immediately and visibly.
 */
public final class MixinTargetVerifier {
    /** Targets that exist on both the client and the dedicated server. */
    private static final String[] COMMON_TARGETS = {
            "net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.inventory.ResultSlot",
            "net.minecraft.world.item.ItemStack",
            "com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu",
            "com.tiviacz.travelersbackpack.inventory.BackpackWrapper",
            "com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler",
            "com.tiviacz.travelersbackpack.inventory.FluidTank",
            "com.tiviacz.travelersbackpack.item.HoseItem",
            "com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TanksUpgrade",
            "com.tiviacz.travelersbackpack.inventory.upgrades.feeding.FeedingUpgrade",
            "com.tiviacz.travelersbackpack.inventory.sorter.ContainerSorter",
            "com.tiviacz.travelersbackpack.common.ServerActions",
            "com.tiviacz.travelersbackpack.inventory.sorter.SortSelector",
            "com.tiviacz.travelersbackpack.inventory.menu.slot.UpgradeLockableSlotItemHandler",
            "com.tiviacz.travelersbackpack.inventory.menu.slot.ResultSlotExt"
    };

    private MixinTargetVerifier() {
    }

    public static void verifyCommon() {
        int loaded = load(COMMON_TARGETS);
        TravelersBackpackExtras.LOGGER.info("[Extra Upgrades] verified {} common mixin targets", loaded);
    }

    public static int load(String[] targets) {
        ClassLoader loader = MixinTargetVerifier.class.getClassLoader();
        for (String name : targets) {
            try {
                // initialize = false: load and transform the class without running its static
                // initialisers, which is exactly what applies the mixin.
                Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Mixin target not found: " + name, e);
            }
        }
        return targets.length;
    }
}
