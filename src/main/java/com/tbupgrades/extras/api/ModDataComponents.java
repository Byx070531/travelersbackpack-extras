package com.tbupgrades.extras.api;

import com.tbupgrades.extras.TravelersBackpackExtras;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * Data components owned by this addon.
 *
 * <p>{@link #VIRTUAL_COUNT} is the heart of the "huge stack" feature. Vanilla caps an
 * {@code ItemStack}'s persisted count at 99 ({@code ExtraCodecs.intRange(1, 99)} inside
 * {@code ItemStack.MAP_CODEC}), so a backpack slot that should hold e.g. 365 items cannot be
 * represented by the vanilla count field at all. Instead an oversized slot keeps the real amount
 * in this component and pins the physical stack count to 1.
 */
public final class ModDataComponents {
    /**
     * Present only on "oversized" stacks, i.e. stacks whose logical amount is larger than the
     * item's normal maximum stack size. Invariant: when this component is present the physical
     * {@link net.minecraft.world.item.ItemStack#getCount()} is exactly 1.
     */
    public static final DataComponentType<Long> VIRTUAL_COUNT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "virtual_count"),
            DataComponentType.<Long>builder()
                    .persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build()
    );

    private ModDataComponents() {
    }

    /** Forces class loading / registration; called from the mod initializer. */
    public static void init() {
    }
}
