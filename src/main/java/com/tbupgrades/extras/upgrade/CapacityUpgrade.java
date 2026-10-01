package com.tbupgrades.extras.upgrade;

import com.mojang.datafixers.util.Pair;
import com.tbupgrades.extras.api.CapacityTier;
import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TanksUpgrade;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Passive upgrade that only carries a {@link CapacityTier}. Installing or removing one forces the
 * backpack to recompute its tank capacity so the fluid multiplier follows the item multiplier.
 */
public class CapacityUpgrade extends UpgradeBase<CapacityUpgrade> {
    protected final CapacityTier tier;

    public CapacityUpgrade(UpgradeManager manager, int dataHolderSlot, CapacityTier tier) {
        super(manager, dataHolderSlot, new Point(24, 24));
        this.tier = tier;
        refresh(manager);
    }

    public CapacityTier tier() {
        return this.tier;
    }

    /**
     * Recomputes the backpack's tank capacity from the currently installed upgrades.
     *
     * <p>Uses the manager it is given rather than {@code wrapper.getUpgradeManager()}: this runs from
     * the {@link UpgradeManager} constructor, at which point the wrapper's own field is not assigned
     * yet.
     */
    public static void refresh(UpgradeManager manager) {
        if (manager == null) {
            return;
        }
        BackpackWrapper wrapper = manager.getWrapper();
        if (wrapper == null) {
            return;
        }
        wrapper.setBackpackTankCapacity();
        manager.getUpgrade(TanksUpgrade.class).ifPresent(TanksUpgrade::setTanksCapacity);
    }

    @Override
    public void remove() {
        refresh(getUpgradeManager());
    }

    @Override
    public boolean hasTab() {
        return false;
    }

    @Override
    public List<Pair<Integer, Integer>> getUpgradeSlotsPosition(int x, int y) {
        return List.of();
    }

    @Override
    public List<? extends Slot> getUpgradeSlots(BackpackBaseMenu menu, BackpackWrapper wrapper, int x, int y) {
        return List.of();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public WidgetBase<?> createWidget(BackpackScreen screen, int x, int y) {
        return new CapacityWidget(screen, this, new Point(screen.getGuiLeft() + x, screen.getGuiTop() + y));
    }
}
