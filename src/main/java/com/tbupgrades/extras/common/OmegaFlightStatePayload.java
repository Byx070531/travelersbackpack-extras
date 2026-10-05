package com.tbupgrades.extras.common;

import com.tbupgrades.extras.TravelersBackpackExtras;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: how much flight charge is left, and whether the player is spending it right now.
 *
 * <p>Only the charge itself has to travel - the client could read the switch off the backpack it can
 * already see, but the charge lives purely on the server, and the bar has to draw it.
 *
 * <p>{@code flying} is sent rather than worked out on the client because it is the server that
 * decides when the charge is being spent: it is the flag the HUD uses to take over the experience
 * bar, so it has to agree with the drain exactly.
 */
public record OmegaFlightStatePayload(double charge, double max, boolean flying) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OmegaFlightStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "omega_flight_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OmegaFlightStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, OmegaFlightStatePayload::charge,
            ByteBufCodecs.DOUBLE, OmegaFlightStatePayload::max,
            ByteBufCodecs.BOOL, OmegaFlightStatePayload::flying,
            OmegaFlightStatePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side only; the receiving end is registered by the client initializer. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }
}
