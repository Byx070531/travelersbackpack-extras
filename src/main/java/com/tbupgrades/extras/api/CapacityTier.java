package com.tbupgrades.extras.api;

import net.minecraft.network.chat.Component;

/**
 * The nine storage upgrades, in progression order.
 *
 * <p>Every tier is installed into one of the backpack's upgrade slots. The effective multiplier of
 * a backpack is the product of all installed (non-omega) tiers, clamped to
 * {@link #SUB_ULTIMATE_CAP}; installing an omega upgrade jumps straight to
 * {@link #OMEGA_MULTIPLIER}.
 */
public enum CapacityTier {
    WOODEN("wooden", 2L),
    COPPER("copper", 8L),
    IRON("iron", 16L),
    GOLDEN("golden", 64L),
    EMERALD("emerald", 128L),
    DIAMOND("diamond", 256L),
    NETHERITE("netherite", 1024L),
    ULTIMATE("ultimate", 4096L),
    OMEGA("omega", 33554432L);

    /** Nothing below the ultimate upgrade may push the multiplier past this value. */
    public static final long SUB_ULTIMATE_CAP = 4096L;
    /** {@link #OMEGA} multiplier; 2^25, so 64 * 2^25 == 2^31 exactly. */
    public static final long OMEGA_MULTIPLIER = 33554432L;
    /** Hard ceiling for a single slot: 2^31, i.e. 2147483648 items. */
    public static final long MAX_SLOT_COUNT = 2147483648L;
    /** The vanilla stack size the multipliers are relative to. */
    public static final long BASE_STACK = 64L;

    private final String id;
    private final long multiplier;

    CapacityTier(String id, long multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }

    public String id() {
        return this.id;
    }

    public long multiplier() {
        return this.multiplier;
    }

    public boolean isOmega() {
        return this == OMEGA;
    }

    /** Item id, e.g. {@code travelersbackpackextras:wooden_upgrade}. */
    public String itemId() {
        return "travelersbackpackextras:" + this.id + "_upgrade";
    }

    public String translationKey() {
        return "item.travelersbackpackextras." + this.id + "_upgrade";
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    /** The tier that is one step below this one, or {@code null} for {@link #WOODEN}. */
    public CapacityTier previous() {
        return this.ordinal() == 0 ? null : values()[this.ordinal() - 1];
    }

    public static CapacityTier byId(String id) {
        for (CapacityTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return null;
    }
}
