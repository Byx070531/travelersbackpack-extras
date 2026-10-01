package com.tbupgrades.extras.common;

import com.tbupgrades.extras.init.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Grants genuine creative-style flight while the {@code creative_flight} effect is active - the
 * player stays in survival, they simply gain the ability to fly.
 *
 * <p>The flight ability is persisted by vanilla in the player's ability data, so it has to be
 * revoked explicitly: we remember which players we granted it to, and also clear a stale flag when
 * a player joins without the effect.
 */
public final class FlightHandler {
    private static final Set<UUID> GRANTED = new HashSet<>();

    private FlightHandler() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                update(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!player.hasEffect(ModEffects.CREATIVE_FLIGHT) && canFlyOnlyByUs(player)) {
                setFlight(player, false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GRANTED.remove(handler.getPlayer().getUUID()));
    }

    private static void update(ServerPlayer player) {
        boolean active = player.hasEffect(ModEffects.CREATIVE_FLIGHT);
        if (active) {
            if (!player.getAbilities().mayfly) {
                setFlight(player, true);
            }
            GRANTED.add(player.getUUID());
        } else if (GRANTED.contains(player.getUUID())) {
            GRANTED.remove(player.getUUID());
            if (canFlyOnlyByUs(player)) {
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
