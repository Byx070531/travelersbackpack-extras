package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.client.IpnCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops Inventory Profiles Next from adding its sort buttons to Traveler's Backpack's screen, which
 * already has its own - and only that one knows about oversized stacks.
 *
 * <p>IPN draws those buttons from its own render hook rather than from the screen's widget list, so
 * they cannot be removed from the outside. What can be stopped is the widget collection's
 * {@code init}: IPN's render path checks the {@code initialized} flag it sets, so a collection that
 * never initialises draws nothing and handles nothing.
 *
 * <p>Only the screen's class decides - IPN keeps working on every other screen. The target belongs to
 * another mod, so the injector is {@code require = 0} inside a mixin config that is not required:
 * if IPN is absent, or renames the method in a future release, this quietly does nothing instead of
 * failing the game.
 */
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.SortingButtonCollectionWidget", remap = false)
public abstract class IpnSortButtonMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0)
    private void tbx$skipBackpackScreen(CallbackInfo ci) {
        if (IpnCompat.isBackpackScreen(this)) {
            ci.cancel();
        }
    }
}