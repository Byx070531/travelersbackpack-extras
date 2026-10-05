package com.tbupgrades.extras.item;

import com.tbupgrades.extras.api.CapacityTier;
import com.tbupgrades.extras.upgrade.CapacityUpgrades;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.item.upgrades.UpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * One of the nine storage upgrades. Placed into a backpack's upgrade slot it raises the amount of
 * items a single backpack slot may hold, and the backpack's tank capacity, by
 * {@link CapacityTier#multiplier()}.
 */
public class CapacityUpgradeItem extends UpgradeItem {
    private final CapacityTier tier;

    public CapacityUpgradeItem(Item.Properties properties, CapacityTier tier) {
        super(properties, null);
        this.tier = tier;
    }

    public CapacityTier tier() {
        return this.tier;
    }

    @Override
    public boolean isTickingUpgrade() {
        return false;
    }

    @Override
    public boolean requiresEquippedBackpack() {
        return false;
    }

    @Override
    public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return true;
    }

    @Override
    public Class<? extends UpgradeBase<?>> getUpgradeClass() {
        return CapacityUpgrades.classFor(this.tier);
    }

    @Override
    public TriFunction<UpgradeManager, Integer, ItemStack, Optional<? extends UpgradeBase<?>>> getUpgrade() {
        return (manager, dataHolderSlot, provider) -> Optional.of(CapacityUpgrades.create(this.tier, manager, dataHolderSlot));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.desc." + this.tier.id())
                .withStyle(ChatFormatting.GRAY));
        // A separate tooltip line from the netherite upgrade up: a "\n" inside a
        // translation value is drawn as a missing-glyph box, not as a line break.
        if (this.tier.ordinal() >= com.tbupgrades.extras.api.CapacityTier.NETHERITE.ordinal()) {
            consumer.accept(Component.translatable("tooltip.travelersbackpackextras.infinite_line")
                    .withStyle(ChatFormatting.GRAY));
        }
        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.summary",
                        Component.literal(this.tier.multiplier() + "x"),
                        Component.literal(Long.toString(this.tier.multiplier() * 64L)))
                .withStyle(ChatFormatting.BLUE));
        // Only the omega tier flies, and the switch that turns it off is easy to miss on a 24 pixel
        // icon, so the two lines that explain it live here.
        if (this.tier.isOmega()) {
            consumer.accept(Component.translatable("tooltip.travelersbackpackextras.omega_flight")
                    .withStyle(ChatFormatting.GOLD));
            consumer.accept(Component.translatable("tooltip.travelersbackpackextras.omega_flight_toggle")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
