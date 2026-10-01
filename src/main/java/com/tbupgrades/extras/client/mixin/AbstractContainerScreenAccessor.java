package com.tbupgrades.extras.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads {@code AbstractContainerScreen.hoveredSlot} (which is protected) so the raw mouse handler can
 * tell which slot the pointer is over without going through the screen's click dispatch.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("hoveredSlot")
    Slot tbx$getHoveredSlot();
}
