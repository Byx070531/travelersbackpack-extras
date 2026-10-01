package com.tbupgrades.extras.client.mixin;

import com.tbupgrades.extras.api.InfiniteTank;
import com.tbupgrades.extras.api.TbxTankHover;
import com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TankWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records where the pointer is over the two tanks (so the clear keybind can target the right one)
 * and shows an endless tank as infinite in its tooltip.
 */
@Mixin(TankWidget.class)
public abstract class TankWidgetMixin implements TbxTankHover {
    @Unique
    private double tbx$mouseX = Double.NaN;

    @Unique
    private double tbx$mouseY = Double.NaN;

    @Inject(method = "renderAboveBg", at = @At("HEAD"))
    private void tbx$captureMouse(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        this.tbx$mouseX = mouseX;
        this.tbx$mouseY = mouseY;
    }

    @Override
    public int tbx$hoveredTank() {
        if (Double.isNaN(this.tbx$mouseX)) {
            return -1;
        }
        TankWidget self = (TankWidget) (Object) this;
        if (self.inTank(self.leftTankElement, this.tbx$mouseX, this.tbx$mouseY)) {
            return 0;
        }
        if (self.inTank(self.rightTankElement, this.tbx$mouseX, this.tbx$mouseY)) {
            return 1;
        }
        return -1;
    }

    /**
     * Replaces the "amount/capacity" line with an endless marker when the tank has turned into an
     * infinite source. The tooltip is rebuilt from the vanilla list so nothing else is disturbed.
     */
    @Inject(method = "getTankTooltip", at = @At("RETURN"), cancellable = true)
    private static void tbx$infiniteTooltip(
            com.tiviacz.travelersbackpack.inventory.FluidTank tank,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.List<Component>> cir) {
        if (!(tank instanceof InfiniteTank infinite) || !infinite.tbx$isInfinite()) {
            return;
        }
        java.util.List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        java.util.List<Component> updated = new java.util.ArrayList<>(original);
        updated.set(updated.size() - 1, Component.translatable("tooltip.travelersbackpackextras.infinite"));
        updated.add(Component.translatable("tooltip.travelersbackpackextras.infinite_clear"));
        cir.setReturnValue(updated);
    }
}
