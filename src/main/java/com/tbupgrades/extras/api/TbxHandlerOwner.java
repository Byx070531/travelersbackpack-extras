package com.tbupgrades.extras.api;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;

/**
 * Implemented (via mixin) by Traveler's Backpack's {@code ItemStackHandler} so that a handler knows
 * which backpack it belongs to, and therefore which storage multiplier applies to it.
 *
 * <p>Lives outside the mixin package on purpose: Mixin refuses to load ordinary classes that sit in
 * a package owned by a mixin configuration.
 */
public interface TbxHandlerOwner {
    BackpackWrapper tbx$getWrapper();

    void tbx$setWrapper(BackpackWrapper wrapper);

    /** True when this handler is the backpack's main item storage. */
    boolean tbx$isStorage();

    /** Marks this handler as the backpack's main item storage. */
    void tbx$markStorage();
}
