package com.tbupgrades.extras.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for the "virtual count" representation of oversized backpack slots.
 *
 * <p>Invariants:
 * <ul>
 *     <li>A stack without {@link ModDataComponents#VIRTUAL_COUNT} behaves exactly like a vanilla
 *     stack; its amount is {@link ItemStack#getCount()}.</li>
 *     <li>A stack with the component always has a physical count of 1 and carries the real amount
 *     in the component. The amount is never larger than {@link CapacityTier#MAX_SLOT_COUNT}.</li>
 *     <li>Whenever the logical amount drops back to the item's normal maximum stack size (or below)
 *     the component is stripped again, so normal gameplay is untouched.</li>
 * </ul>
 */
public final class VirtualStack {
    private VirtualStack() {
    }

    /** The normal (vanilla) stack size of this item; what the multiplier multiplies. */
    public static int baseMax(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        // The live maximum, not only the stored component: Carpet's "stackable shulker boxes" rule (and
        // the shulkerfix patch that keeps it working on 1.20.5+) raises the limit through the getter
        // while the component keeps saying 1. Reading only the component therefore saw an unstackable
        // item and clamped every backpack slot to 1. For ordinary items the two readings are identical,
        // so nothing changes for them.
        int component = Math.max(1, stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
        return Math.max(component, stack.getMaxStackSize());
    }

    /** True when the stack carries an oversized amount. */
    public static boolean isVirtual(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModDataComponents.VIRTUAL_COUNT);
    }

    /** The logical amount held by this stack, oversized or not. */
    public static long count(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        Long virtual = stack.get(ModDataComponents.VIRTUAL_COUNT);
        if (virtual != null) {
            return Math.max(0L, virtual);
        }
        return stack.getCount();
    }

    /**
     * Writes a logical amount into the stack, switching between the vanilla representation and the
     * oversized one as needed. Never returns a stack that violates the invariants above.
     *
     * @param stack the stack to modify (mutated in place)
     * @param count the new logical amount, clamped to {@code [0, 2^31]}
     */
    public static void setCount(ItemStack stack, long count) {
        if (stack.isEmpty()) {
            return;
        }
        long clamped = Math.min(Math.max(0L, count), CapacityTier.MAX_SLOT_COUNT);
        int base = baseMax(stack);
        if (clamped <= base) {
            stack.remove(ModDataComponents.VIRTUAL_COUNT);
            stack.setCount((int) clamped);
        } else {
            stack.set(ModDataComponents.VIRTUAL_COUNT, clamped);
            stack.setCount(1);
        }
    }

    /** Adds {@code amount} to the logical count (or removes when negative) and normalizes. */
    public static void grow(ItemStack stack, long amount) {
        if (stack.isEmpty()) {
            return;
        }
        setCount(stack, count(stack) + amount);
    }

    /**
     * Item-and-components comparison that treats the oversized bookkeeping as invisible.
     *
     * <p>Everything in Traveler's Backpack - and in most other mods - decides whether two stacks are
     * "the same" with {@link ItemStack#isSameItemSameComponents}. Because an oversized stack carries
     * an extra component it would never match a plain stack of the same item, which is what stopped
     * the feeding upgrade from recognising stacked food. This helper is the single place that knows
     * the count component is bookkeeping rather than identity.
     */
    public static boolean sameItemIgnoringCount(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        if (!isVirtual(first) && !isVirtual(second)) {
            return ItemStack.isSameItemSameComponents(first, second);
        }
        ItemStack a = first.copy();
        ItemStack b = second.copy();
        a.remove(ModDataComponents.VIRTUAL_COUNT);
        b.remove(ModDataComponents.VIRTUAL_COUNT);
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /** Whether {@code candidate} may be merged into {@code existing}: same item and same data
     * components apart from the virtual count bookkeeping. */
    public static boolean mergeable(ItemStack existing, ItemStack candidate) {
        return sameItemIgnoringCount(existing, candidate);
    }

    /**
     * A plain vanilla copy of this stack: the oversized bookkeeping is removed and the count is the
     * item's own maximum stack size, exactly what vanilla's creative pick-block produces.
     *
     * <p>Without this, {@code ItemStack.copyWithCount(getMaxStackSize())} would carry the count
     * component along and hand the player a cursor stack that still claims to hold the whole slot.
     */
    public static ItemStack vanillaStackCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.remove(ModDataComponents.VIRTUAL_COUNT);
        copy.setCount(baseMax(stack));
        return copy;
    }

    /**
     * Moves up to {@code amount} items from {@code source} into {@code target}.
     *
     * <p>Both stacks keep a valid representation - the oversized bookkeeping is added and removed as
     * the amounts cross the item's own stack size - and the total never changes, so no item can be
     * created or destroyed. This is the only safe way to merge two backpack stacks; growing the
     * physical count of an oversized stack (which is what vanilla's merge helpers do) would leave it
     * holding a single item that claims to be a whole pile.
     *
     * @return how many items actually moved
     */
    public static long transfer(ItemStack target, ItemStack source, long amount) {
        if (target.isEmpty() || source.isEmpty() || amount <= 0L) {
            return 0L;
        }
        long available = count(source);
        long moved = Math.min(Math.min(amount, available), CapacityTier.MAX_SLOT_COUNT);
        if (moved <= 0L) {
            return 0L;
        }
        setCount(source, available - moved);
        setCount(target, count(target) + moved);
        return moved;
    }

    /**
     * Produces a physical, vanilla-legal copy of at most {@code amount} items. The result is also
     * clamped to the stack's logical amount, so a request for a full group can never invent items
     * that the slot does not hold.
     */
    public static ItemStack copyWithCount(ItemStack stack, long amount) {
        long logical = count(stack);
        int physical = (int) Math.min(Math.min(Math.max(amount, 0L), logical), baseMax(stack));
        ItemStack copy = stack.copy();
        copy.remove(ModDataComponents.VIRTUAL_COUNT);
        copy.setCount(physical);
        return copy;
    }
}
