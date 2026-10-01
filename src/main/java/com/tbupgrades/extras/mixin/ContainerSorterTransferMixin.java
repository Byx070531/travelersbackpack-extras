package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.VirtualStack;
import com.tbupgrades.extras.common.BackpackInteractions;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.inventory.sorter.ContainerSorter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Stops the "transfer to inventory" button from destroying oversized stacks.
 *
 * <p>Traveler's Backpack moves items to the player by reading a slot and emptying it, which is correct
 * for ordinary stacks - a slot holds what it says it holds. An oversized stack breaks that assumption
 * twice over: its physical count is 1 while it claims hundreds, so the button moved a single item and
 * then cleared the whole slot, deleting the rest.
 *
 * <p>When the backpack holds such a stack the transfer is taken over here: ordinary stacks still move
 * exactly as before, and oversized stacks are left untouched. Nothing is lost, at the cost of having
 * to pull those stacks out by hand - one group at a time, with the mouse rules the backpack already
 * has for them.
 */
@Mixin(ContainerSorter.class)
public abstract class ContainerSorterTransferMixin {
    @Inject(method = "transferToPlayer", at = @At("HEAD"), cancellable = true)
    private static void tbx$keepOversizedStacks(BackpackWrapper wrapper, Player player, CallbackInfo ci) {
        ItemStackHandler storage = wrapper == null ? null : wrapper.getStorage();
        if (storage == null || !CapacityHelper.hasOversizedStack(storage)) {
            // Nothing oversized in the way: Traveler's Backpack's own transfer is the right behaviour.
            return;
        }
        ci.cancel();
        List<Integer> keepInPlace = wrapper.getUnsortableSlots();
        for (int i = 0; i < storage.getSlots(); i++) {
            if (keepInPlace != null && keepInPlace.contains(i)) {
                continue;
            }
            ItemStack stack = storage.getStackInSlot(i);
            if (stack.isEmpty() || VirtualStack.isVirtual(stack)) {
                continue;
            }
            BackpackInteractions.moveToPlayer(player, storage, i, VirtualStack.count(stack));
        }
    }
}