package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.CompactNumbers;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import com.tiviacz.travelersbackpack.inventory.menu.slot.UpgradeLockableSlotItemHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Governs which storage upgrades may be dropped into, and taken out of, a backpack's upgrade slots.
 *
 * <p>Three rules are enforced here:
 * <ul>
 *     <li>at most {@link CapacityHelper#MAX_INSTALLED} storage upgrades in one backpack;</li>
 *     <li>duplicates are allowed - a second omega upgrade can be installed, it simply has no extra
 *     effect (Traveler's Backpack's own "one upgrade per kind" rule is bypassed for our items);</li>
 *     <li>a storage upgrade cannot be taken out while any storage slot still holds an oversized
 *     stack, because those amounts are only legal while a storage upgrade is installed. Removing the
 *     last one first would leave a backpack with more than 64 items in a slot and nothing to allow
 *     it, and the next merge, transfer or sort would then clamp that slot and lose the difference.</li>
 * </ul>
 */
@Mixin(UpgradeLockableSlotItemHandler.class)
public abstract class UpgradeLockableSlotItemHandlerMixin {
    @Shadow
    public BackpackBaseMenu menu;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void tbx$limitCapacityUpgrades(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CapacityHelper.tierOf(stack) == null) {
            return;
        }
        if (this.menu == null || this.menu.getWrapper() == null) {
            return;
        }
        if (CapacityHelper.installedCount(this.menu.getWrapper()) >= CapacityHelper.MAX_INSTALLED) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tbx$keepEnoughCapacity(Player player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack upgrade = ((Slot) (Object) this).getItem();
        if (CapacityHelper.tierOf(upgrade) == null) {
            return;
        }
        if (this.menu == null || this.menu.getWrapper() == null) {
            return;
        }
        BackpackWrapper wrapper = this.menu.getWrapper();
        int upgradeSlot = ((Slot) (Object) this).getContainerSlot();
        if (CapacityHelper.canRemoveUpgrade(wrapper, upgradeSlot)) {
            return;
        }
        cir.setReturnValue(false);
        // mayPickup is asked on both sides; only the server has someone to tell.
        if (player != null && !player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("message.travelersbackpackextras.upgrade_locked",
                    CompactNumbers.format(CapacityHelper.requiredMultiplier(wrapper)),
                    CompactNumbers.format(CapacityHelper.multiplierWithout(wrapper, upgradeSlot))), true);
        }
    }
}
