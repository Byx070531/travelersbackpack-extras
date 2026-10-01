package com.tbupgrades.extras.api;

/**
 * Implemented (via mixin) by the tank widget so the key handler can tell which tank the pointer is
 * over.
 */
public interface TbxTankHover {
    /** {@code 0} for the left tank, {@code 1} for the right tank, {@code -1} when not hovering. */
    int tbx$hoveredTank();
}
