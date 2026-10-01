package com.tbupgrades.extras.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Hands Mod Menu the addon's own config screen. Only referenced from the {@code modmenu} entrypoint in
 * fabric.mod.json, so nothing here runs when Mod Menu is absent - the json file remains usable by hand.
 */
public class TbxModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TbxConfigScreen::new;
    }
}