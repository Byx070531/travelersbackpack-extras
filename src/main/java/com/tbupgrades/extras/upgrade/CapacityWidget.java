package com.tbupgrades.extras.upgrade;

import com.tbupgrades.extras.api.CapacityHelper;

import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Minimal widget for a capacity upgrade. The upgrade has no tab of its own; this exists so the
 * backpack screen always has a non-null widget to register, and it contributes tooltip lines.
 */
public class CapacityWidget extends UpgradeWidgetBase<CapacityUpgrade> {
    public CapacityWidget(BackpackScreen screen, CapacityUpgrade upgrade, Point pos) {
        super(screen, upgrade, pos, new Point(137, 0), CapacityHelper.widgetTitle(upgrade.tier()));
    }

    @Override
    public void getAdditionalTooltips(Consumer<Component> consumer) {
        super.getAdditionalTooltips(consumer);
        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.summary",
                Component.literal(this.upgrade.tier().multiplier() + "x"),
                Component.literal(Long.toString(this.upgrade.tier().multiplier() * 64L))));
    }
}
