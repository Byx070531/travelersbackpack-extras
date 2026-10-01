package com.tbupgrades.extras.client;

import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.lang.reflect.Method;

/**
 * The two places where Inventory Profiles Next has to be told to stand down inside a Traveler's
 * Backpack, and nothing else.
 *
 * <p>Objects belonging to IPN are inspected reflectively: naming IPN types (or the mapped Minecraft
 * types in their signatures) would tie this addon to one IPN build and its mapping set. Every failure
 * path answers "no", so an IPN update that renames something leaves IPN untouched instead of broken.
 */
public final class IpnCompat {
    private IpnCompat() {
    }

    /** True when the widget belongs to a Traveler's Backpack screen. */
    public static boolean isBackpackScreen(Object ipnObject) {
        if (ipnObject == null) {
            return false;
        }
        try {
            Method getScreen = ipnObject.getClass().getMethod("getScreen");
            Object screen = getScreen.invoke(ipnObject);
            return screen instanceof AbstractContainerScreen<?> containerScreen
                    && containerScreen.getMenu() instanceof AbstractBackpackMenu;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * True while a crafting result slot holds an elytra.
     *
     * <p>The elytra recipe uses an elytra as a template that is handed back after the craft has
     * settled, so the grid is empty for a moment in between. IPN's continuous crafting clicks faster
     * than that and would stop after the first craft, which is why this one recipe keeps its manual
     * clicking - every other recipe keeps IPN's continuous crafting.
     */
    public static boolean isElytraRecipeOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu == null) {
            return false;
        }
        for (Slot slot : menu.slots) {
            if (slot instanceof ResultSlot && slot.getItem().is(Items.ELYTRA)) {
                return true;
            }
        }
        return false;
    }
}