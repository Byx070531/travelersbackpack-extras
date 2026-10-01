package com.tbupgrades.extras.client;

import com.tbupgrades.extras.common.TbxConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The in-game switches, shown by Mod Menu when it is installed.
 *
 * <p>Deliberately built from vanilla widgets only: Mod Menu provides the button and this provides the
 * screen, so no configuration library is needed as a dependency. Both switches write straight back to
 * config/travelersbackpackextras.json, so the file stays the single source of truth.
 */
public class TbxConfigScreen extends Screen {
    private final Screen parent;

    public TbxConfigScreen(Screen parent) {
        super(Component.translatable("screen.travelersbackpackextras.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int y = this.height / 2 - 30;
        this.addRenderableWidget(Button.builder(toggle("infinite_fluids", TbxConfig.infiniteFluids()), button -> {
            TbxConfig.setInfiniteFluids(!TbxConfig.infiniteFluids());
            TbxConfig.save();
            button.setMessage(toggle("infinite_fluids", TbxConfig.infiniteFluids()));
        }).bounds(left, y, 200, 20).build());
        this.addRenderableWidget(Button.builder(toggle("apple_flight", TbxConfig.appleGrantsFlight()), button -> {
            TbxConfig.setAppleGrantsFlight(!TbxConfig.appleGrantsFlight());
            TbxConfig.save();
            button.setMessage(toggle("apple_flight", TbxConfig.appleGrantsFlight()));
        }).bounds(left, y + 24, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(left, y + 56, 200, 20).build());
    }

    private static Component toggle(String key, boolean value) {
        return Component.translatable("screen.travelersbackpackextras." + key,
                Component.translatable(value ? "options.on" : "options.off"));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}