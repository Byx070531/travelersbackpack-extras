package com.tbupgrades.extras.common;

import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.api.InfiniteTanks;
import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import com.tiviacz.travelersbackpack.inventory.upgrades.tanks.TanksUpgrade;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client to server packet for the interactions that vanilla's click types cannot express:
 * the alt-click inventory fill, the oversized drop and clearing an endless tank.
 *
 * <p>Every action is re-validated on the server against the menu the player actually has open, so a
 * malicious client cannot reach past its own backpack.
 */
public record BackpackActionPayload(Action action, int slot) implements CustomPacketPayload {
    public enum Action {
        /** Alt + left click: one group of items into every empty inventory slot. */
        FILL_INVENTORY,
        /** Control + drop key. */
        DROP_GROUP,
        /** Control + shift + drop key. */
        DROP_BIG,
        /** Clearing an endless fluid out of the left tank. */
        CLEAR_LEFT_TANK,
        /** Clearing an endless fluid out of the right tank. */
        CLEAR_RIGHT_TANK,
        /** Shift + right click: a single item straight into the inventory. */
        MOVE_ONE,
    TRANSFER_OVERSIZED,
    CTRL_SWAP
    }

    public static final CustomPacketPayload.Type<BackpackActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "backpack_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.action.ordinal(),
            ByteBufCodecs.VAR_INT, BackpackActionPayload::slot,
            (action, slot) -> new BackpackActionPayload(Action.values()[action], slot)
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> handle(payload, context.player()));
    }

    private static void handle(BackpackActionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof AbstractBackpackMenu menu)) {
            return;
        }
        switch (payload.action()) {
            case FILL_INVENTORY -> BackpackInteractions.fillInventory(menu, payload.slot(), player);
            case DROP_GROUP -> BackpackInteractions.dropGroup(menu, payload.slot(), player);
            case DROP_BIG -> BackpackInteractions.dropBig(menu, payload.slot(), player);
            case CLEAR_LEFT_TANK -> clearTank(menu, true);
            case CLEAR_RIGHT_TANK -> clearTank(menu, false);
            case MOVE_ONE -> BackpackInteractions.moveOneToInventory(menu, payload.slot(), player);
            case TRANSFER_OVERSIZED -> BackpackInteractions.transferFirstOversized(menu, player);
            case CTRL_SWAP -> BackpackInteractions.ctrlClickSwap(menu, payload.slot(), player);
        }
        player.containerMenu.broadcastChanges();
    }

    private static void clearTank(AbstractBackpackMenu menu, boolean left) {
        menu.getWrapper().getUpgradeManager().getUpgrade(TanksUpgrade.class).ifPresent(tanks -> {
            if (left) {
                InfiniteTanks.clear(tanks.getLeftTank());
            } else {
                InfiniteTanks.clear(tanks.getRightTank());
            }
        });
    }
}
