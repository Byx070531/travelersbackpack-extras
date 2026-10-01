package com.tbupgrades.extras.upgrade;

import com.tbupgrades.extras.api.CapacityTier;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * One concrete {@link UpgradeBase} subclass per tier.
 *
 * <p>Traveler's Backpack refuses to install two upgrades that share an upgrade class, so each tier
 * needs its own class for the "multipliers stack" rule to work. The omega upgrade deliberately
 * reuses a single class: installing a second copy is accepted by the slot but has no effect, which
 * is exactly the requested behaviour.
 */
public final class CapacityUpgrades {
    private static final Map<CapacityTier, Class<? extends UpgradeBase<?>>> CLASSES = new EnumMap<>(CapacityTier.class);
    private static final Map<CapacityTier, BiFunction<UpgradeManager, Integer, ? extends UpgradeBase<?>>> FACTORIES = new EnumMap<>(CapacityTier.class);

    static {
        register(CapacityTier.WOODEN, Wooden.class, Wooden::new);
        register(CapacityTier.COPPER, Copper.class, Copper::new);
        register(CapacityTier.IRON, Iron.class, Iron::new);
        register(CapacityTier.GOLDEN, Golden.class, Golden::new);
        register(CapacityTier.EMERALD, Emerald.class, Emerald::new);
        register(CapacityTier.DIAMOND, Diamond.class, Diamond::new);
        register(CapacityTier.NETHERITE, Netherite.class, Netherite::new);
        register(CapacityTier.ULTIMATE, Ultimate.class, Ultimate::new);
        register(CapacityTier.OMEGA, Omega.class, Omega::new);
    }

    private CapacityUpgrades() {
    }

    private static void register(CapacityTier tier, Class<? extends UpgradeBase<?>> type,
                                 BiFunction<UpgradeManager, Integer, ? extends UpgradeBase<?>> factory) {
        CLASSES.put(tier, type);
        FACTORIES.put(tier, factory);
    }

    public static Class<? extends UpgradeBase<?>> classFor(CapacityTier tier) {
        return CLASSES.get(tier);
    }

    public static UpgradeBase<?> create(CapacityTier tier, UpgradeManager manager, int slot) {
        return FACTORIES.get(tier).apply(manager, slot);
    }

    public static class Wooden extends CapacityUpgrade {
        public Wooden(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.WOODEN);
        }
    }

    public static class Copper extends CapacityUpgrade {
        public Copper(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.COPPER);
        }
    }

    public static class Iron extends CapacityUpgrade {
        public Iron(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.IRON);
        }
    }

    public static class Golden extends CapacityUpgrade {
        public Golden(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.GOLDEN);
        }
    }

    public static class Emerald extends CapacityUpgrade {
        public Emerald(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.EMERALD);
        }
    }

    public static class Diamond extends CapacityUpgrade {
        public Diamond(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.DIAMOND);
        }
    }

    public static class Netherite extends CapacityUpgrade {
        public Netherite(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.NETHERITE);
        }
    }

    public static class Ultimate extends CapacityUpgrade {
        public Ultimate(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.ULTIMATE);
        }
    }

    public static class Omega extends CapacityUpgrade {
        public Omega(UpgradeManager manager, int slot) {
            super(manager, slot, CapacityTier.OMEGA);
        }
    }
}
