package com.tbupgrades.extras.init;

import com.tbupgrades.extras.TravelersBackpackExtras;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The "creative flight" status effect granted by the enchanted diamond golden apple. The effect
 * itself is passive; {@link com.tbupgrades.extras.common.FlightHandler} keeps the player's flight
 * ability in sync with it.
 */
public final class ModEffects {
    public static final Holder<MobEffect> CREATIVE_FLIGHT = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, "creative_flight"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0x9BE7FF) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                    return false;
                }
            }
    );

    private ModEffects() {
    }

    public static void init() {
    }
}
