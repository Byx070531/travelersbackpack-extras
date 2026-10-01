package com.tbupgrades.extras.api;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Multiplier bookkeeping for a backpack.
 *
 * <p>Rules requested by the design:
 * <ul>
 *     <li>Multipliers of installed upgrades multiply together.</li>
 *     <li>Whatever combination of sub-ultimate upgrades is installed, the result never exceeds
 *     {@link CapacityTier#SUB_ULTIMATE_CAP} (4096x, the ultimate upgrade's own value).</li>
 *     <li>An omega upgrade always yields {@link CapacityTier#OMEGA_MULTIPLIER}, and further copies
 *     change nothing.</li>
 *     <li>At most {@link #MAX_INSTALLED} storage upgrades may sit in one backpack.</li>
 * </ul>
 */
public final class CapacityHelper {
    /** Maximum number of this addon's upgrades that may be installed in a single backpack. */
    public static final int MAX_INSTALLED = 3;

    /** Safety valve so that capacity arithmetic can never overflow a {@code long}. */
    public static final long MAX_FLUID_CAPACITY = Long.MAX_VALUE / 4L;

    private CapacityHelper() {
    }

    /** The tier of this stack, or {@code null} when it is not one of our upgrades. */
    public static CapacityTier tierOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return tierOf(stack.getItem());
    }

    public static CapacityTier tierOf(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            return null;
        }
        String id = key.toString();
        for (CapacityTier tier : CapacityTier.values()) {
            if (tier.itemId().equals(id)) {
                return tier;
            }
        }
        return null;
    }

    /** How many storage upgrades are currently sitting in the backpack's upgrade slots. */
    public static int installedCount(BackpackWrapper wrapper) {
        ItemStackHandler upgrades = upgradeHandler(wrapper);
        if (upgrades == null) {
            return 0;
        }
        int found = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (tierOf(upgrades.getStackInSlot(i)) != null) {
                found++;
            }
        }
        return found;
    }

    /**
     * The product of every installed tier, ignoring the omega upgrade, clamped to the sub-ultimate
     * cap.
     */
    public static long subUltimateProduct(BackpackWrapper wrapper) {
        ItemStackHandler upgrades = upgradeHandler(wrapper);
        if (upgrades == null) {
            return 1L;
        }
        long product = 1L;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            CapacityTier tier = tierOf(upgrades.getStackInSlot(i));
            if (tier == null || tier.isOmega()) {
                continue;
            }
            if (product > CapacityTier.SUB_ULTIMATE_CAP / tier.multiplier()) {
                return CapacityTier.SUB_ULTIMATE_CAP;
            }
            product *= tier.multiplier();
        }
        return Math.min(product, CapacityTier.SUB_ULTIMATE_CAP);
    }

    public static boolean hasOmega(BackpackWrapper wrapper) {
        ItemStackHandler upgrades = upgradeHandler(wrapper);
        if (upgrades == null) {
            return false;
        }
        for (int i = 0; i < upgrades.getSlots(); i++) {
            CapacityTier tier = tierOf(upgrades.getStackInSlot(i));
            if (tier != null && tier.isOmega()) {
                return true;
            }
        }
        return false;
    }

    /** The effective storage multiplier of this backpack; 1 when no upgrade is installed. */
    public static long multiplier(BackpackWrapper wrapper) {
        if (wrapper == null) {
            return 1L;
        }
        if (hasOmega(wrapper)) {
            return CapacityTier.OMEGA_MULTIPLIER;
        }
        return subUltimateProduct(wrapper);
    }

    /** True when at least one installed tier is the netherite upgrade or higher. */
    public static boolean hasInfiniteFluidTier(BackpackWrapper wrapper) {
        if (!com.tbupgrades.extras.common.TbxConfig.infiniteFluids()) {
            return false;
        }
        ItemStackHandler upgrades = upgradeHandler(wrapper);
        if (upgrades == null) {
            return false;
        }
        for (int i = 0; i < upgrades.getSlots(); i++) {
            CapacityTier tier = tierOf(upgrades.getStackInSlot(i));
            if (tier != null && tier.ordinal() >= CapacityTier.NETHERITE.ordinal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maximum number of items a single slot of this backpack can hold.
     *
     * <p>The multiplier applies to the item's own stack size, so a normal 64-stack item reaches
     * {@code 64 * multiplier}. Items that do not stack at all (tools, potions, backpacks, ...) stay
     * unstackable - the upgrade expands capacity, it does not make every item stackable.
     */
    public static long slotLimit(BackpackWrapper wrapper, ItemStack stack) {
        int base = VirtualStack.baseMax(stack);
        if (base <= 1) {
            return base;
        }
        if (wrapper == null) {
            return base;
        }
        long limit = base * multiplier(wrapper);
        return Math.min(Math.max(limit, base), CapacityTier.MAX_SLOT_COUNT);
    }

    /**
     * The multiplier this backpack would still have if the upgrade sitting in
     * {@code excludedUpgradeSlot} were taken out. Mirrors {@link #multiplier(BackpackWrapper)} exactly,
     * so "what is left" is computed with the same omega and sub-ultimate rules as "what is there".
     */
    public static long multiplierWithout(BackpackWrapper wrapper, int excludedUpgradeSlot) {
        ItemStackHandler upgrades = upgradeHandler(wrapper);
        if (upgrades == null) {
            return 1L;
        }
        boolean omega = false;
        long product = 1L;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (i == excludedUpgradeSlot) {
                continue;
            }
            CapacityTier tier = tierOf(upgrades.getStackInSlot(i));
            if (tier == null) {
                continue;
            }
            if (tier.isOmega()) {
                omega = true;
                continue;
            }
            if (product > CapacityTier.SUB_ULTIMATE_CAP / tier.multiplier()) {
                product = CapacityTier.SUB_ULTIMATE_CAP;
            } else {
                product *= tier.multiplier();
            }
        }
        return omega ? CapacityTier.OMEGA_MULTIPLIER : Math.min(product, CapacityTier.SUB_ULTIMATE_CAP);
    }

    /**
     * The smallest multiplier that still leaves every stack in this backpack legal: a stack of 1024
     * needs {@code 1024 / 64 = 16}, a stack of 64 needs nothing at all.
     *
     * <p>Taking an upgrade out is only a problem when the upgrades that remain can no longer hold what
     * is already stored, so this is the number the removal rule is compared against.
     */
    public static long requiredMultiplier(BackpackWrapper wrapper) {
        ItemStackHandler storage = wrapper == null ? null : wrapper.getStorage();
        if (storage == null) {
            return 1L;
        }
        long required = 1L;
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack stack = storage.getStackInSlot(i);
            long count = VirtualStack.count(stack);
            int base = VirtualStack.baseMax(stack);
            // Items that do not stack cannot be oversized, and anything within one vanilla stack
            // needs no upgrade at all.
            if (base <= 1 || count <= base) {
                continue;
            }
            required = Math.max(required, (count + base - 1L) / base);
        }
        return required;
    }

    /**
     * Whether the storage upgrade in {@code upgradeSlot} may be taken out.
     *
     * <p>Removing an upgrade is fine as long as the ones left behind still cover everything in the
     * backpack - three upgrades holding 1024 items can lose one, because what remains still multiplies
     * to at least the 16x that 1024 items need. It is only refused when the remaining upgrades would
     * leave a stack bigger than its slot is allowed to be, which would strand those items in a
     * backpack that is no longer allowed to hold them.
     */
    public static boolean canRemoveUpgrade(BackpackWrapper wrapper, int upgradeSlot) {
        if (wrapper == null) {
            return true;
        }
        return multiplierWithout(wrapper, upgradeSlot) >= requiredMultiplier(wrapper);
    }

    /**
     * True when any slot of this storage holds more items than a slot could hold without a storage
     * upgrade.
     */
    public static boolean hasOversizedStack(ItemStackHandler storage) {
        if (storage == null) {
            return false;
        }
        for (int i = 0; i < storage.getSlots(); i++) {
            if (VirtualStack.isVirtual(storage.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The label shown on the upgrade's tab inside the backpack: {@code 16x (1024)}, plus an endless
     * fluid note from the netherite upgrade up.
     *
     * <p>Traveler's Backpack takes this as a translation key and renders it literally when the key is
     * unknown, so the finished string is composed here with translated parts rather than passed as a
     * key - the numbers differ per tier, which a fixed key cannot express.
     */
    public static String widgetTitle(CapacityTier tier) {
        long count = Math.min(tier.multiplier() * 64L, CapacityTier.MAX_SLOT_COUNT);
        // Single line on purpose: Traveler's Backpack draws this label with a plain string draw, which
        // does not break on "\n" - a two line title showed up as a missing glyph box instead. The
        // numbers live in the widget's tooltip, which is rendered properly.
        return net.minecraft.network.chat.Component
                .translatable("item.travelersbackpackextras." + tier.id() + "_upgrade").getString();
    }

    /** Backpack tank capacity after applying the multiplier, saturating instead of overflowing. */
    public static long scaleFluidCapacity(long base, long multiplier) {
        if (base <= 0L || multiplier <= 1L) {
            return base;
        }
        if (base > MAX_FLUID_CAPACITY / multiplier) {
            return MAX_FLUID_CAPACITY;
        }
        return base * multiplier;
    }

    private static ItemStackHandler upgradeHandler(BackpackWrapper wrapper) {
        try {
            return wrapper.getUpgrades();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
