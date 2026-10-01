package com.tbupgrades.extras;

import com.tbupgrades.extras.client.ClientMixinTargetVerifier;
import net.fabricmc.api.ClientModInitializer;

public class TravelersBackpackExtrasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientMixinTargetVerifier.run();
    }
}
