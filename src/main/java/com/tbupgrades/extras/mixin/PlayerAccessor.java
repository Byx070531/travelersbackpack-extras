package com.tbupgrades.extras.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code Player.canGlide()} is protected, and it is exactly the question the flight charge needs
 * answered: it is true when an elytra is worn <em>or</em> provided by an accessory mod, so accessory
 * mods keep working without this addon knowing about them.
 */
@Mixin(Player.class)
public interface PlayerAccessor {
    @Invoker("canGlide")
    boolean tbx$canGlide();
}