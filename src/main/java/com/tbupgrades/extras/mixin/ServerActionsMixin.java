package com.tbupgrades.extras.mixin;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.CompactNumbers;
import com.tiviacz.travelersbackpack.common.ServerActions;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses to take a storage upgrade out of a backpack that still needs it.
 *
 * <p>Traveler's Backpack does not let upgrades be dragged out of their slot - {@code BackpackBaseMenu}
 * marks every installed upgrade as locked, so {@code mayPickup} is already false before anyone gets to
 * vote on it. Upgrades are removed through the remove button on the upgrade's own tab, which sends an
 * action packet that lands in {@link ServerActions#removeBackpackUpgrade}. That is the one place that
 * has to know the rule.
 *
 * <p>The rule: removing an upgrade is fine as long as the upgrades that stay behind still cover every
 * stack in the backpack. A stack of 1024 needs a 16x multiplier, so a backpack with three upgrades on
 * it can lose one, while a single wooden upgrade (2x, leaving 1x) cannot be taken out from under those
 * same 1024 items.
 */
@Mixin(ServerActions.class)
public abstract class ServerActionsMixin {
    @Inject(method = "removeBackpackUpgrade", at = @At("HEAD"), cancellable = true)
    private static void tbx$keepEnoughCapacity(ServerPlayer player, int dataHolderSlot, CallbackInfo ci) {
        if (player == null || !(player.containerMenu instanceof BackpackBaseMenu menu)) {
            return;
        }
        BackpackWrapper wrapper = menu.getWrapper();
        if (wrapper == null || CapacityHelper.canRemoveUpgrade(wrapper, dataHolderSlot)) {
            return;
        }
        ci.cancel();
        player.displayClientMessage(Component.translatable("message.travelersbackpackextras.upgrade_locked",
                CompactNumbers.format(CapacityHelper.requiredMultiplier(wrapper)),
                CompactNumbers.format(CapacityHelper.multiplierWithout(wrapper, dataHolderSlot))), true);
    }
}
