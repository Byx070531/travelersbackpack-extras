package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.VirtualStack;
import com.tbupgrades.extras.common.BackpackInteractions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Takes over mouse clicks on the storage slots of an upgraded backpack.
 *
 * <p>Traveler's Backpack overrides {@code doClick} on its menu class, and that override calls up
 * into this method for everything except its ghost/filter slots, so injecting here covers every
 * backpack menu (item, block entity and wearable) at a single point. Clicks on ordinary stacks and
 * on ordinary backpacks are left completely alone.
 *
 * <p>Shift-click merging is <em>not</em> handled here: the mod replaces {@code moveItemStackTo}
 * wholesale, so that lives in {@link BackpackBaseMenuMixin}.
 *
 * <h2>What is deliberately left to vanilla</h2>
 * <ul>
 *     <li><b>QUICK_CRAFT</b> (drag distribution, used by vanilla and by Mouse Tweaks, which
 *     deliberately stays out of the way of vanilla's left-drag): the collect step refuses a slot
 *     whose contents are not component-identical to the carried stack, and an oversized stack always
 *     differs by its count component. Such a slot is therefore never dragged onto and never
 *     rewritten, so the gesture is safe as-is. Swallowing it - as an earlier version did - broke
 *     dragging over every backpack slot.</li>
 *     <li><b>PICKUP_ALL</b> (double-click gathering): the gather only ever pulls from slots that pass
 *     the same component test, so oversized stacks are skipped and normal slots work normally. For
 *     the clicked slot itself the branch does nothing at all unless the slot is empty or cannot be
 *     picked up.</li>
 *     <li><b>THROW with a non-zero target</b>: dropping the cursor stack over nothing is unrelated
 *     to the backpack.</li>
 * </ul>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void tbx$doClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (BackpackInteractions.handleClick((AbstractContainerMenu) (Object) this, slotId, button, clickType, player)) {
            ci.cancel();
        }
    }

    /**
     * Keeps drag distribution and double-click gathering away from oversized stacks.
     *
     * <p>{@link ItemStackMixin} makes the standard stack comparison blind to the count component, so
     * these two gestures would now happily pick an oversized slot as a target - and both end with
     * {@code slot.setByPlayer(stack.copyWithCount(amount))}, which replaces the stack outright and
     * would throw away everything past the amount placed. Refusing the slot keeps it untouched, and
     * the gesture simply skips it as it did before.
     */
    @Inject(method = "canItemQuickReplace", at = @At("HEAD"), cancellable = true)
    private static void tbx$protectOversized(@Nullable Slot slot, ItemStack stack, boolean bl,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (slot != null && VirtualStack.isVirtual(slot.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
