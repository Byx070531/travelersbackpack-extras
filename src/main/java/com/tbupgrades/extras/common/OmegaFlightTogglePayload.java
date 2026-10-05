package com.tbupgrades.extras.common;

import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.api.CapacityHelper;
import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Shift + the omega flight key: flips the omega upgrade's switch without opening the backpack.
 *
 * <p>Deliberately a separate payload from {@link BackpackActionPayload}: that one only makes sense
 * while a backpack menu is open and refuses anything else, whereas this has to work while the player
 * is flying around in the world with no menu at all.
 *
 * <p>Carries nothing - the server decides which backpack and which slot to touch from the player it
 * arrives from, so a client cannot name a backpack or slot of its own choosing.
 */
public record OmegaFlightTogglePayload() implements CustomPacketPayload {
    public static final OmegaFlightTogglePayload INSTANCE = new OmegaFlightTogglePayload();

    public static final CustomPacketPayload.Type<OmegaFlightTogglePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "omega_flight_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OmegaFlightTogglePayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> handle(context.player()));
    }

    private static void handle(ServerPlayer player) {
        BackpackWrapper wrapper = wornBackpack(player);
        if (wrapper == null) {
            return;
        }
        int slot = CapacityHelper.omegaSlot(wrapper);
        if (slot < 0) {
            return;
        }
        ItemStackHandler upgrades = wrapper.getUpgrades();
        ItemStack current = upgrades.getStackInSlot(slot);
        if (current.isEmpty()) {
            return;
        }
        // The component has to be written through the slot handler rather than onto the live upgrade
        // object: that is what Traveler's Backpack syncs, and what its own switch does too.
        ItemStack updated = current.copy();
        boolean nowOn = CapacityHelper.flipSwitch(updated);
        upgrades.setStackInSlot(slot, updated);

        player.displayClientMessage(Component.translatable(nowOn
                        ? "screen.travelersbackpack.upgrade_enabled"
                        : "screen.travelersbackpack.upgrade_disabled",
                current.getHoverName()), true);
    }

    private static BackpackWrapper wornBackpack(ServerPlayer player) {
        try {
            return AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.UPGRADES_ONLY.get());
        } catch (RuntimeException e) {
            TravelersBackpackExtras.LOGGER.debug("[Extra Upgrades] no worn backpack to toggle flight on", e);
            return null;
        }
    }
}
