package com.tbupgrades.extras;

import com.tbupgrades.extras.client.ClientMixinTargetVerifier;
import com.tbupgrades.extras.client.OmegaFlightClient;
import net.fabricmc.api.ClientModInitializer;

public class TravelersBackpackExtrasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientMixinTargetVerifier.run();
        OmegaFlightClient.init();
    }
}
