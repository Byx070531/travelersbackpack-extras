package com.tbupgrades.extras.common;

import com.tbupgrades.extras.init.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Grants genuine creative-style flight while the {@code creative_flight} effect is active - the
 * player stays in survival, they simply gain the ability to fly.
 *
 * <p>The flight ability is persisted by vanilla in the player's ability data - along with whether the
 * player was actually flying - so it has to be revoked explicitly: we remember which players we granted
 * it to, and also clear a stale flag when a player comes back without the effect and without anything
 * else in this addon granting flight.
 */
public final class FlightHandler {
    private static final Set<UUID> GRANTED = new HashSet<>();
    /**
     * Players who have just logged in, with the tick by which their saved flight has to be justified.
     * A deadline rather than a one-shot flag because the effect can take a moment to appear.
     */
    private static final Map<UUID, Integer> PENDING_CHECK = new HashMap<>();

    /** How long after logging in the effect may take to appear before the saved flight is abandoned. */
    private static final int CHECK_GRACE_TICKS = 20;

    private FlightHandler() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                update(player);
            }
        });

        // Only queued here, never decided here. At the moment a player joins their own data has not
        // been read back yet - logging proves it: the backpack reads as absent and the apple's effect
        // as inactive for a player who owns both, and clearing the flag on that answer drops them out
        // of the sky on every single login.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PENDING_CHECK.put(player.getUUID(), player.tickCount + CHECK_GRACE_TICKS);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            GRANTED.remove(id);
            PENDING_CHECK.remove(id);
        });
    }

    private static void update(ServerPlayer player) {
        UUID id = player.getUUID();
        Integer deadline = PENDING_CHECK.get(id);

        if (player.hasEffect(ModEffects.CREATIVE_FLIGHT)) {
            PENDING_CHECK.remove(id);
            if (deadline != null && OmegaFlight.wasAirborne(player) && !player.getAbilities().flying) {
                // Logged out in mid-air under the apple and the game did not put it back. Forcing the
                // flag is what makes this work at all, and onUpdateAbilities is what tells the client -
                // without it the server believes the player is flying while the client walks them
                // straight down out of the sky.
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            } else if (!player.getAbilities().mayfly) {
                setFlight(player, true);
            } else if (deadline != null) {
                // Already flying: just make sure the client agrees.
                player.onUpdateAbilities();
            }
            GRANTED.add(id);
            return;
        }

        if (deadline != null) {
            // No effect yet. Keep waiting until the deadline in case it is still being read back.
            if (player.tickCount > deadline) {
                PENDING_CHECK.remove(id);
                if (canFlyOnlyByUs(player) && !OmegaFlight.grantsFlight(player)) {
                    setFlight(player, false);
                }
            }
            return;
        }

        if (GRANTED.contains(id)) {
            GRANTED.remove(id);
            // The omega charge may have taken over the moment the effect ran out. Taking flight away
            // then would drop the player mid-air with a full tank, which is the opposite of the point.
            if (canFlyOnlyByUs(player) && !OmegaFlight.grantsFlight(player)) {
                setFlight(player, false);
            }
        }
    }

    private static boolean canFlyOnlyByUs(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    private static void setFlight(ServerPlayer player, boolean mayFly) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = mayFly;
        if (!mayFly) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
    }
}
