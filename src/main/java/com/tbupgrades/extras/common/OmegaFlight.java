package com.tbupgrades.extras.common;

import com.mojang.serialization.Codec;
import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.init.ModEffects;
import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The omega upgrade's flight charge: 600 points by default, one per second while flying, 0.8 back per
 * second while not. At zero the player stops flying and drops into a glide instead.
 *
 * <p>The glide falls back the way the game itself decides: {@code canGlide()} is asked first, which is
 * already true when an elytra is worn <em>or</em> supplied by an accessory mod, so accessory mods keep
 * working without this addon knowing anything about them. Only when that says no is an elytra taken out
 * of the backpack and worn - and only into an empty chest slot, never over equipment the player chose.
 *
 * <p>The charge lives per player, not on the backpack item, so swapping backpacks neither refills nor
 * loses it, and it is written to the player's own saved data so that quitting the game keeps it. It is
 * kept when the upgrade's own switch is turned off.
 *
 * <p>{@code mayfly} is a single vanilla flag with more than one thing able to set it, so this class
 * only ever takes back flight it handed out itself ({@link #GRANTED}) and stands down entirely while
 * the golden apple's effect is doing the same job - otherwise the two would spend every tick undoing
 * each other and the player would drop out of the sky.
 */
public final class OmegaFlight {
    /**
     * The charge itself, kept on the player rather than on the backpack.
     *
     * <p>On the player for two reasons. A backpack is an item: the charge would follow the item around
     * and swapping to another omega backpack would hand out a second full tank. And an attachment is
     * written into the player's own saved data, so quitting the game no longer throws the charge away
     * and coming back does not start from 600 again.
     *
     * <p>Persistent with the double codec, and copied on death so that dying is not a way to refill
     * either.
     */
    private static final AttachmentType<Double> CHARGE = AttachmentRegistry.<Double>builder()
            .persistent(Codec.DOUBLE)
            .copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(
                    TravelersBackpackExtras.MOD_ID, "omega_flight_charge"));

    /**
     * Whether the player was airborne when they last left, whoever was paying for it.
     *
     * <p>Vanilla already saves "was flying" as part of the ability block, but leaning on that alone is
     * what did not work: something in the login path loses it, and the only way to put a player back in
     * the air reliably is to know, ourselves, that they were up there. Both flight sources consult it,
     * so it records the ability rather than the charge.
     */
    private static final AttachmentType<Boolean> WAS_AIRBORNE = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(
                    TravelersBackpackExtras.MOD_ID, "omega_flight_was_flying"));

    private static final Map<UUID, OmegaFlightStatePayload> SYNCED = new HashMap<>();
    /** Players flying on this addon's charge, so that flight is only ever taken back from them. */
    private static final Set<UUID> GRANTED = new HashSet<>();
    /**
     * Players who left in mid-air and still have to be put back there, with the tick by which the
     * attempt gives up. Waiting for the upgrade to show up is what makes this order independent.
     */
    private static final Map<UUID, Integer> PENDING_RESTORE = new HashMap<>();

    /** How far the charge may drift before the client is told again; well under one pixel of bar. */
    private static final double SYNC_EPSILON = 0.25D;

    /** How long after logging in the upgrade may take to appear before the restore is abandoned. */
    private static final int RESTORE_GRACE_TICKS = 100;

    private OmegaFlight() {
    }

    public static double charge(ServerPlayer player) {
        return player.getAttachedOrElse(CHARGE, TbxConfig.omegaFlightMax());
    }

    /** Exposed so the self test can assert the charge is really persisted and survives death. */
    public static AttachmentType<Double> chargeAttachment() {
        return CHARGE;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Queued rather than decided here: whether anything is still granting flight cannot be
            // answered until the backpack has been read back, which is not guaranteed to have happened
            // when this fires. The flag is our own record of having left in mid-air, so a player who
            // left standing still logs back in standing.
            if (Boolean.TRUE.equals(player.getAttached(WAS_AIRBORNE))) {
                PENDING_RESTORE.put(player.getUUID(), player.tickCount + RESTORE_GRACE_TICKS);
            }
        });
        // The charge itself lives on the player and is saved with them; only the bookkeeping that has
        // no meaning offline is dropped here.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            SYNCED.remove(id);
            GRANTED.remove(id);
            PENDING_RESTORE.remove(id);
        });
    }

    /**
     * Puts a player who left in mid-air back in mid-air, on the first tick where the omega upgrade is
     * actually there to pay for it.
     *
     * <p>Waiting for that tick is the whole point. Doing it at login means guessing whether the
     * backpack has been read back yet, and getting that wrong either drops the player out of the sky or
     * leaves them hovering on a tank that nothing is filling.
     */
    private static void handleLoginRestore(ServerPlayer player, boolean ours) {
        UUID id = player.getUUID();
        Integer deadline = PENDING_RESTORE.get(id);
        if (deadline == null) {
            return;
        }
        if (!ours) {
            // No upgrade yet; keep waiting, but not forever, in case one is equipped much later.
            if (player.tickCount > deadline) {
                PENDING_RESTORE.remove(id);
            }
            return;
        }
        PENDING_RESTORE.remove(id);
        GRANTED.add(id);
        player.getAbilities().mayfly = true;
        if (!player.getAbilities().flying) {
            // Forcing the flag also tells the client, so it stops applying gravity on its side.
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    /**
     * Whether the omega charge is the thing holding this player up right now.
     *
     * <p>Public because the golden apple's own bookkeeping has to know. {@code mayfly} is one flag with
     * several things able to set it, so before either feature takes flight away it has to be sure the
     * other one is not the one currently providing it.
     */
    public static boolean grantsFlight(ServerPlayer player) {
        return TbxConfig.omegaFlight()
                && !player.hasEffect(ModEffects.CREATIVE_FLIGHT)
                && CapacityHelper.hasEnabledOmega(wornBackpack(player));
    }

    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        double max = TbxConfig.omegaFlightMax();
        double charge = Math.min(Math.max(charge(player), 0.0D), max);

        if (player.isCreative() || player.isSpectator()) {
            // Creative flight is the game's own; the charge is neither spent nor granted there.
            GRANTED.remove(id);
            PENDING_RESTORE.remove(id);
            player.getAbilities().mayfly = true;
            sync(player, charge, max, false);
            return;
        }

        boolean ours = grantsFlight(player);
        handleLoginRestore(player, ours);

        boolean flying;
        if (!ours) {
            release(player);
            charge = Math.min(max, charge + TbxConfig.omegaFlightRegen() / 20.0);
            flying = false;
        } else {
            flying = player.getAbilities().flying;
            if (!flying) {
                charge = Math.min(max, charge + TbxConfig.omegaFlightRegen() / 20.0);
                grant(player);
            } else {
                charge -= TbxConfig.omegaFlightDrain() / 20.0;
                if (charge > 0.0) {
                    grant(player);
                } else {
                    charge = 0.0;
                    if (release(player)) {
                        // Only a charge that actually let go of the player throws them into the glide.
                        fallIntoGlide(player);
                        flying = false;
                    }
                }
            }
        }
        player.setAttached(CHARGE, charge);
        // What "left in mid-air" means for the next login. The ability, not the charge: the golden
        // apple's flight has to be restorable too, and it is the same flag either way.
        player.setAttached(WAS_AIRBORNE, player.getAbilities().flying);
        sync(player, charge, max, flying);
    }

    /** Whether the player was airborne when they last left, whoever was paying for it. */
    public static boolean wasAirborne(ServerPlayer player) {
        return Boolean.TRUE.equals(player.getAttached(WAS_AIRBORNE));
    }

    /** Hands out flight, and remembers having done so. */
    private static void grant(ServerPlayer player) {
        GRANTED.add(player.getUUID());
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /**
     * Takes back flight this addon handed out, and only that.
     *
     * @return whether the player was actually dropped, which is what decides if they glide or simply
     * stay up on somebody else's flight
     */
    private static boolean release(ServerPlayer player) {
        if (!GRANTED.remove(player.getUUID())) {
            return false;
        }
        if (player.hasEffect(ModEffects.CREATIVE_FLIGHT) || !player.getAbilities().mayfly) {
            return false;
        }
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        return true;
    }

    /**
     * Tells the client what the bar should show, but only when there is something new to say.
     *
     * <p>The charge moves every tick while flying, so sending it every tick would be twenty packets a
     * second per player for a bar whose whole width is 182 pixels: a quarter of a point is already
     * under a pixel, and the switch and the takeoff still arrive immediately because they change
     * {@code flying} rather than the amount.
     */
    private static void sync(ServerPlayer player, double charge, double max, boolean flying) {
        UUID id = player.getUUID();
        OmegaFlightStatePayload last = SYNCED.get(id);
        if (last != null
                && last.flying() == flying
                && last.max() == max
                && Math.abs(last.charge() - charge) < SYNC_EPSILON) {
            return;
        }
        OmegaFlightStatePayload payload = new OmegaFlightStatePayload(charge, max, flying);
        SYNCED.put(id, payload);
        if (ServerPlayNetworking.canSend(player, OmegaFlightStatePayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Out of charge: glide if the game says the player can, otherwise fetch an elytra from the backpack
     * and only then glide. No room for it (the chest slot is taken) means falling, which is the honest
     * outcome - the addon does not unequip gear the player put on.
     */
    private static void fallIntoGlide(ServerPlayer player) {
        if (((com.tbupgrades.extras.mixin.PlayerAccessor) player).tbx$canGlide()) {
            player.startFallFlying();
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            return;
        }
        BackpackWrapper wrapper = wornBackpack(player);
        if (wrapper == null) {
            return;
        }
        for (int i = 0; i < wrapper.getStorage().getSlots(); i++) {
            ItemStack stack = wrapper.getStorage().getStackInSlot(i);
            if (stack.is(Items.ELYTRA)) {
                ItemStack elytra = stack.copyWithCount(1);
                stack.shrink(1);
                wrapper.getStorage().setStackInSlot(i, stack);
                player.setItemSlot(EquipmentSlot.CHEST, elytra);
                player.startFallFlying();
                return;
            }
        }
    }

    private static BackpackWrapper wornBackpack(ServerPlayer player) {
        try {
            // Traveler's Backpack's own entry point, the same one it uses for its upgrade actions.
            return AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.UPGRADES_ONLY.get());
        } catch (RuntimeException e) {
            TravelersBackpackExtras.LOGGER.debug("[Extra Upgrades] no worn backpack for the flight charge", e);
            return null;
        }
    }
}
