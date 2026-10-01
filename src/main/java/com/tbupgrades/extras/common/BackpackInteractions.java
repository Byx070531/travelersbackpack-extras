package com.tbupgrades.extras.common;

import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.VirtualStack;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu;
import com.tiviacz.travelersbackpack.inventory.menu.slot.BackpackSlotItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Mouse and key interactions with the storage slots of an upgraded backpack.
 *
 * <p>Rules (one "group" is 64 items):
 * <ul>
 *     <li>Left click - take one group onto the cursor.</li>
 *     <li>Right click - take half a group (32) onto the cursor.</li>
 *     <li>Shift + left click - move one group straight into the player's inventory.</li>
 *     <li>Shift + right click - move a single item into the player's inventory.</li>
 *     <li>Alt + left click - fill every empty inventory slot with one group.</li>
 *     <li>Control + drop key - drop one group on the ground.</li>
 *     <li>Control + shift + drop key - drop 1024 items (16 groups), or everything if fewer.</li>
 * </ul>
 *
 * <p>These rules only take over slots of a backpack that actually has a storage upgrade installed
 * (or that still holds an oversized stack after the upgrade was removed). Every other backpack, and
 * every slot outside the backpack, keeps behaving exactly like vanilla.
 */
public final class BackpackInteractions {
    public static final long GROUP = 64L;
    public static final long HALF_GROUP = 32L;
    public static final long BIG_DROP = 1024L;

    private BackpackInteractions() {
    }

    // ------------------------------------------------------------------ slot classification

    /** True when the slot id refers to one of the backpack's own storage slots. */
    public static boolean isStorageSlot(AbstractContainerMenu menu, int slotId) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu)
                || slotId < 0 || slotId >= menu.slots.size()
                || !(menu.getSlot(slotId) instanceof BackpackSlotItemHandler slot)) {
            return false;
        }
        // Only slots genuinely backed by the backpack's own storage may be taken over. The player's
        // own inventory slots live in the same menu, and if they happen to use the same slot class
        // this check is what keeps the addon from hijacking them - which would break the vanilla
        // handling of those slots, creative pick-block on your own items above all.
        // Only the backpack's own storage slots may be taken over. The player's inventory slots live
        // in the same menu, so they are excluded by their container. The test must stay this loose:
        // Traveler's Backpack hands its slots a wrapper around the storage handler, so comparing
        // against the handler itself matches nothing and silently switches every custom rule off -
        // the vanilla handling then moves one physical item out of an oversized stack and clears the
        // slot, which loses the rest.
        return !(slot.container instanceof Inventory);
    }

    /**
     * True when this slot should be driven by the rules above: it belongs to the backpack's storage
     * and the backpack either has a storage upgrade installed or still holds an oversized stack.
     */
    public static boolean isManagedSlot(AbstractContainerMenu menu, int slotId) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return false;
        }
        if (CapacityHelper.multiplier(backpackMenu.getWrapper()) > 1L) {
            return true;
        }
        return VirtualStack.isVirtual(menu.getSlot(slotId).getItem());
    }

    // ------------------------------------------------------------------ click dispatch

    /**
     * Handles the vanilla click types on a managed slot.
     *
     * @return {@code true} when the click was fully handled and vanilla must not run
     */
    public static boolean handleClick(AbstractContainerMenu menu, int slotId, int button, ClickType clickType, Player player) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return false;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        int index = menu.getSlot(slotId).getContainerSlot();
        if (!isManagedSlot(menu, slotId)) {
            return false;
        }

        return switch (clickType) {
            case PICKUP -> pickup(menu, backpackMenu.getWrapper(), storage, index, button == 0);
            case QUICK_MOVE -> button == 0
                    ? moveToPlayer(player, storage, index, GROUP)
                    : button == 1 && moveToPlayer(player, storage, index, 1L);
            case THROW -> button == 0
                    ? drop(player, storage, index, 1L)
                    : button == 1 && drop(player, storage, index, GROUP);
            case CLONE -> cloneToCursor(menu, storage, index, player);
            case SWAP -> swapWithHotbar(menu, storage, index, button, player);
            // QUICK_CRAFT (drag distribution) is handled separately, in AbstractContainerMenuMixin:
            // it is a multi-step sequence whose final step needs the menu's private drag state.
            default -> false;
        };
    }

    /**
     * Number key / offhand key on an oversized slot.
     *
     * <p>Vanilla swaps the two stacks outright, which would drop the entire oversized stack - count
     * component included - into the hotbar, bypassing the "at most one group per action" rule and
     * creating a stack that later loses everything past its physical count. Instead at most one group
     * moves into the hotbar slot, exactly like a normal left click.
     *
     * @return {@code false} when the button is not a hotbar/offhand key, so vanilla can decide
     */
    private static boolean swapWithHotbar(AbstractContainerMenu menu, ItemStackHandler storage, int index,
                                          int button, Player player) {
        boolean hotbar = button >= 0 && button < Inventory.SELECTION_SIZE;
        boolean offhand = button == Inventory.SLOT_OFFHAND;
        if (!hotbar && !offhand) {
            return false;
        }
        if (!menu.getCarried().isEmpty()) {
            return false;
        }
        ItemStack current = storage.getStackInSlot(index);
        if (current.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        ItemStack held = inventory.getItem(button);
        long logical = VirtualStack.count(current);

        if (held.isEmpty()) {
            ItemStack part = VirtualStack.copyWithCount(current, Math.min(GROUP, logical));
            if (!part.isEmpty()) {
                inventory.setItem(button, part);
                removeLogical(storage, index, part.getCount());
            }
            return true;
        }

        if (VirtualStack.mergeable(held, current)) {
            long space = Math.max(0L, VirtualStack.baseMax(held) - held.getCount());
            ItemStack part = VirtualStack.copyWithCount(current, Math.min(Math.min(GROUP, logical), space));
            if (!part.isEmpty()) {
                held.grow(part.getCount());
                inventory.setItem(button, held);
                removeLogical(storage, index, part.getCount());
            }
            return true;
        }

        // A different item is already there: leave the oversized stack alone.
        return true;
    }

    /**
     * Creative pick-block (middle click).
     *
     * <p>Vanilla copies the slot stack with {@code copyWithCount(getMaxStackSize())}, which for an
     * oversized stack keeps the count component and would hand the player the entire slot. The copy
     * given here is always a plain stack of the item's own maximum size.
     *
     * @return {@code false} to let vanilla handle the cases it should keep handling
     */
    private static boolean cloneToCursor(AbstractContainerMenu menu, ItemStackHandler storage, int index, Player player) {
        if (!player.getAbilities().instabuild || !menu.getCarried().isEmpty()) {
            return false;
        }
        ItemStack current = storage.getStackInSlot(index);
        if (current.isEmpty()) {
            return false;
        }
        menu.setCarried(VirtualStack.vanillaStackCopy(current));
        return true;
    }

    /**
     * Left / right click with or without something on the cursor.
     */
    private static boolean pickup(AbstractContainerMenu menu, BackpackWrapper wrapper, ItemStackHandler storage,
                                  int index, boolean primary) {
        ItemStack current = storage.getStackInSlot(index);
        ItemStack carried = menu.getCarried();

        if (carried.isEmpty()) {
            if (current.isEmpty()) {
                return true;
            }
            long take = Math.min(primary ? GROUP : HALF_GROUP, VirtualStack.count(current));
            ItemStack taken = VirtualStack.copyWithCount(current, take);
            if (taken.isEmpty()) {
                return true;
            }
            menu.setCarried(taken);
            removeLogical(storage, index, taken.getCount());
            return true;
        }

        if (current.isEmpty()) {
            long accepted = Math.min(primary ? carried.getCount() : 1L,
                    Math.min(carried.getCount(), CapacityHelper.slotLimit(wrapper, carried)));
            if (accepted > 0L) {
                storage.setStackInSlot(index, carried.copyWithCount((int) accepted));
                carried.shrink((int) accepted);
            }
            return true;
        }

        if (VirtualStack.mergeable(current, carried)) {
            long space = CapacityHelper.slotLimit(wrapper, current) - VirtualStack.count(current);
            long wanted = Math.min(primary ? carried.getCount() : 1L, Math.min(carried.getCount(), Math.max(0L, space)));
            if (wanted > 0L) {
                ItemStack toAdd = carried.copyWithCount((int) wanted);
                ItemStack remainder = storage.insertItem(index, toAdd, false);
                int inserted = (int) wanted - remainder.getCount();
                if (inserted > 0) {
                    carried.shrink(inserted);
                }
            }
            return true;
        }

        // A different item. Swapping is only safe while the stored stack is an ordinary one that
        // fits on the cursor; an oversized stack must never be silently reduced to 64.
        if (!VirtualStack.isVirtual(current)
                && carried.getCount() <= CapacityHelper.slotLimit(wrapper, carried)) {
            ItemStack taken = current.copy();
            storage.setStackInSlot(index, carried.copy());
            menu.setCarried(taken);
        }
        return true;
    }

    // ------------------------------------------------------------------ packet driven actions

    /**
     * Alt + left click: one group into every empty inventory slot.
     *
     * @return the number of inventory slots that were filled
     */
    public static int fillInventory(AbstractContainerMenu menu, int slotId, Player player) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return 0;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        int index = menu.getSlot(slotId).getContainerSlot();

        Inventory inventory = player.getInventory();
        int filled = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                continue;
            }
            ItemStack group = VirtualStack.copyWithCount(storage.getStackInSlot(index), GROUP);
            if (group.isEmpty()) {
                break;
            }
            inventory.setItem(i, group);
            removeLogical(storage, index, group.getCount());
            filled++;
        }
        return filled;
    }

    /** Control + drop key: drop one group (or the whole stack when it is smaller). */
    public static boolean dropGroup(AbstractContainerMenu menu, int slotId, Player player) {
        return dropFrom(menu, slotId, player, GROUP);
    }

    /**
     * Shift + right click: a single item straight into the player's inventory.
     *
     * <p>Driven from the raw mouse press rather than from vanilla's {@code QUICK_MOVE} click type,
     * because in a large modpack a container handler installed by another mod can consume that click
     * before the backpack screen ever sees it.
     */
    public static boolean moveOneToInventory(AbstractContainerMenu menu, int slotId, Player player) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return false;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        int index = menu.getSlot(slotId).getContainerSlot();
        return moveToPlayer(player, storage, index, 1L);
    }

    /** Control + shift + drop key: drop up to 1024 items. */
    public static boolean dropBig(AbstractContainerMenu menu, int slotId, Player player) {
        return dropFrom(menu, slotId, player, BIG_DROP);
    }

    private static boolean dropFrom(AbstractContainerMenu menu, int slotId, Player player, long wanted) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return false;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        int index = menu.getSlot(slotId).getContainerSlot();
        return drop(player, storage, index, wanted);
    }

    // ------------------------------------------------------------------ shift-click support

    /**
     * Merges {@code stack} into backpack storage slots inside {@code [start, end)}.
     *
     * <p>Vanilla's {@code moveItemStackTo} decides whether two stacks match with
     * {@code ItemStack.isSameItemSameComponents}, which is never true for an oversized stack because
     * of the extra count component. This runs first and does the merge through the backpack handler,
     * where the boosted slot limit is known.
     *
     * @return {@code true} when at least one item was moved
     */
    public static boolean mergeIntoBackpack(AbstractContainerMenu menu, ItemStack stack, int start, int end) {
        if (stack.isEmpty() || !(menu instanceof AbstractBackpackMenu backpackMenu)) {
            return false;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        boolean moved = false;
        for (int i = Math.max(0, start); i < Math.min(end, menu.slots.size()) && !stack.isEmpty(); i++) {
            if (!(menu.getSlot(i) instanceof BackpackSlotItemHandler slot)) {
                continue;
            }
            int index = slot.getContainerSlot();
            ItemStack existing = storage.getStackInSlot(index);
            if (existing.isEmpty() || !VirtualStack.mergeable(existing, stack)) {
                continue;
            }
            if (!slot.mayPlace(stack)) {
                continue;
            }
            ItemStack remainder = storage.insertItem(index, stack.copy(), false);
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                moved = true;
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------ primitives

    /** Moves up to {@code wanted} items from the slot into the player's inventory. */
    public static boolean moveToPlayer(Player player, ItemStackHandler storage, int index, long wanted) {
        if (storage.getStackInSlot(index).isEmpty()) {
            return false;
        }
        long remaining = wanted;
        boolean moved = false;
        while (remaining > 0L) {
            ItemStack current = storage.getStackInSlot(index);
            long logical = VirtualStack.count(current);
            if (logical <= 0L) {
                break;
            }
            ItemStack part = VirtualStack.copyWithCount(current, Math.min(remaining, logical));
            if (part.isEmpty()) {
                break;
            }
            int offered = part.getCount();
            player.getInventory().add(part);
            long accepted = offered - part.getCount();
            if (accepted <= 0L) {
                break;
            }
            removeLogical(storage, index, accepted);
            remaining -= accepted;
            moved = true;
        }
        return moved;
    }

    /** Drops up to {@code wanted} items from the slot into the world. */
    public static boolean drop(Player player, ItemStackHandler storage, int index, long wanted) {
        if (storage.getStackInSlot(index).isEmpty()) {
            return false;
        }
        long remaining = wanted;
        boolean dropped = false;
        while (remaining > 0L) {
            ItemStack current = storage.getStackInSlot(index);
            long logical = VirtualStack.count(current);
            if (logical <= 0L) {
                break;
            }
            ItemStack part = VirtualStack.copyWithCount(current, Math.min(remaining, logical));
            if (part.isEmpty()) {
                break;
            }
            int amount = part.getCount();
            player.drop(part, false);
            removeLogical(storage, index, amount);
            remaining -= amount;
            dropped = true;
        }
        return dropped;
    }

    /**
     * Control + the transfer button: takes the FIRST oversized stack out of the backpack and spreads it
     * over the free inventory slots, one group each, leaving whatever does not fit in the backpack.
     *
     * <p>Without Control the transfer button leaves oversized stacks alone entirely - moving them the
     * ordinary way would move a single physical item and then clear the slot.
     *
     * @return how many inventory slots were filled
     */
    public static int transferFirstOversized(AbstractContainerMenu menu, Player player) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu)) {
            return 0;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            if (!isStorageSlot(menu, slotId)) {
                continue;
            }
            if (VirtualStack.isVirtual(storage.getStackInSlot(menu.getSlot(slotId).getContainerSlot()))) {
                return fillInventory(menu, slotId, player);
            }
        }
        return 0;
    }
    /** Which slot each player picked up with Control + left click, waiting for a target slot. */
    private static final java.util.Map<java.util.UUID, Integer> PENDING_SWAP = new java.util.HashMap<>();

    /**
     * Control + left click on a storage slot: the first click takes the slot, the next click swaps the
     * two slots, and clicking the same slot again puts it back down.
     *
     * <p>This is how oversized stacks get rearranged. Dragging one around is not an option: a cursor
     * stack cannot hold an oversized amount, so any drag would clamp the pile to one group and lose
     * the rest. Swapping writes both slots directly instead, so the amounts survive untouched.
     */
    public static void ctrlClickSwap(AbstractContainerMenu menu, int slotId, Player player) {
        if (!(menu instanceof AbstractBackpackMenu backpackMenu) || !isStorageSlot(menu, slotId)) {
            return;
        }
        ItemStackHandler storage = backpackMenu.getWrapper().getStorage();
        int index = menu.getSlot(slotId).getContainerSlot();
        Integer picked = PENDING_SWAP.remove(player.getUUID());
        if (picked == null || picked == index) {
            PENDING_SWAP.put(player.getUUID(), index);
            return;
        }
        ItemStack first = storage.getStackInSlot(picked);
        ItemStack second = storage.getStackInSlot(index);
        storage.setStackInSlot(picked, second);
        storage.setStackInSlot(index, first);
    }
    /** Removes {@code amount} items from the slot, normalising the representation again. */
    public static void removeLogical(ItemStackHandler storage, int index, long amount) {
        ItemStack current = storage.getStackInSlot(index);
        if (current.isEmpty() || amount <= 0L) {
            return;
        }
        long before = VirtualStack.count(current);
        long left = before - amount;
        if (left <= 0L) {
            storage.setStackInSlot(index, ItemStack.EMPTY);
            return;
        }
        VirtualStack.setCount(current, left);
        storage.setStackInSlot(index, current);
    }
}
