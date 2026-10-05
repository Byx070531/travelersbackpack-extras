package com.tbupgrades.extras;

import com.tbupgrades.extras.common.BackpackActionPayload;
import com.tbupgrades.extras.common.DragonScaleDrop;
import com.tbupgrades.extras.common.OmegaFlight;
import com.tbupgrades.extras.common.OmegaFlightStatePayload;
import com.tbupgrades.extras.common.OmegaFlightTogglePayload;
import com.tbupgrades.extras.common.TbxConfig;
import com.tbupgrades.extras.common.FlightHandler;
import com.tbupgrades.extras.common.SelfTest;
import com.tbupgrades.extras.api.ModDataComponents;
import com.tbupgrades.extras.init.ModEffects;
import com.tbupgrades.extras.init.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traveler's Backpack: Extra Upgrades.
 *
 * <p>Adds nine storage capacity upgrades that raise how many items a single backpack slot can hold
 * (up to 2,147,483,648) and scale the backpack's tanks with them, plus the smithing template and the
 * enchanted diamond golden apple used to craft the omega tier. The omega upgrade also carries a
 * flight charge that stands in for creative flight until it runs out.
 */
public class TravelersBackpackExtras implements ModInitializer {
    public static final String MOD_ID = "travelersbackpackextras";
    public static final Logger LOGGER = LoggerFactory.getLogger("TravelersBackpackExtras");

    @Override
    public void onInitialize() {
        TbxConfig.load();
        ModDataComponents.init();
        ModEffects.init();
        ModItems.init();
        BackpackActionPayload.register();
        OmegaFlightTogglePayload.register();
        OmegaFlightStatePayload.register();
        FlightHandler.init();
        DragonScaleDrop.init();
        OmegaFlight.init();
        SelfTest.init();
        LOGGER.info("[Extra Upgrades] loaded: storage upgrades registered");
    }
}
