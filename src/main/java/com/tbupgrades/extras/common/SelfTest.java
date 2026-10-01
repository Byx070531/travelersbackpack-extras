package com.tbupgrades.extras.common;

import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.api.CapacityHelper;
import com.tbupgrades.extras.api.CapacityTier;
import com.tbupgrades.extras.api.CompactNumbers;
import com.tbupgrades.extras.api.ModDataComponents;
import com.tbupgrades.extras.api.VirtualStack;
import com.tbupgrades.extras.init.ModItems;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.inventory.sorter.SortSelector;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostics that run on a real server boot when the {@code travelersbackpackextras.selfTest}
 * system property is set. It checks the parts that are easy to get subtly wrong - the multiplier
 * arithmetic, the oversized count representation, the number formatting and the presence of every
 * data driven recipe - and prints a single PASS/FAIL summary.
 */
public final class SelfTest {
    private static final String[] RECIPES = {
            "wooden_upgrade", "copper_upgrade", "iron_upgrade", "golden_upgrade", "emerald_upgrade",
            "diamond_upgrade", "netherite_upgrade", "ultimate_upgrade",
            "omega_upgrade_smithing_template", "omega_upgrade", "enchanted_diamond_golden_apple",
            "heart_of_the_sea", "elytra"
    };

    private SelfTest() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
    }

    private static void run(MinecraftServer server) {
        if (!Boolean.getBoolean("travelersbackpackextras.selfTest")) {
            return;
        }
        List<String> failures = new ArrayList<>();
        StringBuilder report = new StringBuilder("\n===== TravelersBackpackExtras self test =====\n");

        // Loading every common mixin target first proves the injections apply before anything else
        // is checked.
        MixinTargetVerifier.verifyCommon();

        check(report, failures, "compact 365", "365", CompactNumbers.format(365));
        check(report, failures, "compact 46587", "46k", CompactNumbers.format(46587));
        check(report, failures, "compact 64", "64", CompactNumbers.format(64));
        check(report, failures, "compact 999", "999", CompactNumbers.format(999));
        check(report, failures, "compact 1000", "1.0k", CompactNumbers.format(1000));
        check(report, failures, "compact 999999", "999k", CompactNumbers.format(999999));
        check(report, failures, "compact 1000000", "1.0m", CompactNumbers.format(1000000));
        check(report, failures, "compact 2147483648", "2.1b", CompactNumbers.format(2147483648L));

        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        VirtualStack.setCount(cobble, 365L);
        check(report, failures, "virtual count", "365", Long.toString(VirtualStack.count(cobble)));
        check(report, failures, "virtual physical count", "1", Integer.toString(cobble.getCount()));
        check(report, failures, "virtual component present", "true", Boolean.toString(VirtualStack.isVirtual(cobble)));
        VirtualStack.setCount(cobble, 30L);
        check(report, failures, "normalises back to 30", "30", Integer.toString(cobble.getCount()));
        check(report, failures, "component removed", "false", Boolean.toString(VirtualStack.isVirtual(cobble)));
        check(report, failures, "copies never invent items", "30",
                Integer.toString(VirtualStack.copyWithCount(cobble, 64L).getCount()));

        // Creative pick-block must hand out a plain full stack, never the oversized bookkeeping.
        VirtualStack.setCount(cobble, 365L);
        ItemStack cloned = VirtualStack.vanillaStackCopy(cobble);
        check(report, failures, "pick-block copy is a plain stack", "false",
                Boolean.toString(VirtualStack.isVirtual(cloned)));
        check(report, failures, "pick-block copy is one group", "64", Integer.toString(cloned.getCount()));
        check(report, failures, "pick-block copy counts as 64", "64", Long.toString(VirtualStack.count(cloned)));

        // The count component is bookkeeping, not identity: an oversized stack must still match a
        // plain stack of the same item everywhere the game compares stacks by components.
        ItemStack plainCobble = new ItemStack(Items.COBBLESTONE);
        VirtualStack.setCount(cobble, 365L);
        check(report, failures, "oversized matches a plain stack", "true",
                Boolean.toString(ItemStack.isSameItemSameComponents(plainCobble, cobble)));
        check(report, failures, "different item still differs", "false",
                Boolean.toString(ItemStack.isSameItemSameComponents(new ItemStack(Items.DIRT), cobble)));
        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        enchanted.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("x"));
        ItemStack enchantedOther = enchanted.copy();
        enchantedOther.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("y"));
        ItemStack enchantedVirtual = enchanted.copy();
        VirtualStack.setCount(enchantedVirtual, 300L);
        check(report, failures, "real component differences still count", "false",
                Boolean.toString(ItemStack.isSameItemSameComponents(enchantedOther, enchantedVirtual)));

        // Shrinking an oversized stack must decrement the logical amount without ever driving the
        // physical count to zero - that is what made the feeding upgrade swallow a whole stack.
        VirtualStack.setCount(cobble, 300L);
        cobble.shrink(1);
        check(report, failures, "shrink decrements the logical count", "299", Long.toString(VirtualStack.count(cobble)));
        check(report, failures, "shrink keeps the stack non-empty", "false", Boolean.toString(cobble.isEmpty()));
        check(report, failures, "shrink keeps the representation valid", "1", Integer.toString(cobble.getCount()));
        cobble.shrink(290);
        check(report, failures, "shrink normalises below one group", "9", Long.toString(VirtualStack.count(cobble)));
        check(report, failures, "normalised stack is plain again", "false", Boolean.toString(VirtualStack.isVirtual(cobble)));

        // The feeding upgrade eats one item by copying the slot stack, lowering the copy to a single
        // item and using that copy - and afterwards it inserts whatever is left of the copy back into
        // the backpack. A copy that still carries the count component survives the bite, is mistaken
        // for a leftover and is inserted right back, cancelling the decrement: the amount appeared
        // frozen. The upgrade's copy is therefore stripped of the component, and a meal copy has to
        // behave like one plain item.
        VirtualStack.setCount(cobble, 300L);
        ItemStack meal = cobble.copy();
        meal.remove(ModDataComponents.VIRTUAL_COUNT);
        meal.setCount(1);
        check(report, failures, "a meal copy holds one item", "1", Long.toString(VirtualStack.count(meal)));
        meal.shrink(1);
        check(report, failures, "an eaten meal copy leaves no leftover", "true", Boolean.toString(meal.isEmpty()));
        check(report, failures, "eating a copy keeps the stack intact", "300", Long.toString(VirtualStack.count(cobble)));

        // The sort button merges everything it collected. Merging has to move logical amounts (moving
        // physical counts is what corrupts the representation), keep the total unchanged and stop at
        // the slot limit of the backpack rather than at the item's own 64.
        VirtualStack.setCount(cobble, 100L);
        ItemStack spare = new ItemStack(Items.COBBLESTONE);
        VirtualStack.setCount(spare, 64L);
        long mergeLimit = 128L;
        check(report, failures, "merge moves only what fits", "28",
                Long.toString(VirtualStack.transfer(cobble, spare, mergeLimit - VirtualStack.count(cobble))));
        check(report, failures, "merged target reaches the slot limit", "128", Long.toString(VirtualStack.count(cobble)));
        check(report, failures, "merge leaves the remainder in place", "36", Long.toString(VirtualStack.count(spare)));
        check(report, failures, "merge never changes the total", "164",
                Long.toString(VirtualStack.count(cobble) + VirtualStack.count(spare)));
        check(report, failures, "merged target keeps one physical item", "1", Integer.toString(cobble.getCount()));
        check(report, failures, "merge normalises the remainder again", "36", Integer.toString(spare.getCount()));
        check(report, failures, "the remainder is a plain stack again", "false",
                Boolean.toString(VirtualStack.isVirtual(spare)));

        // A storage upgrade may not be taken out while the backpack still holds such a stack.
        ItemStackHandler probe = new ItemStackHandler(2);
        probe.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        check(report, failures, "ordinary storage holds nothing oversized", "false",
                Boolean.toString(CapacityHelper.hasOversizedStack(probe)));
        probe.setStackInSlot(1, cobble);
        check(report, failures, "an oversized stack is detected", "true",
                Boolean.toString(CapacityHelper.hasOversizedStack(probe)));

        // Sorting by amount has to total the real amounts, not the physical counts.
        ItemStack countedUp = new ItemStack(Items.COBBLESTONE);
        VirtualStack.setCount(countedUp, 189L);
        List<ItemStack> toCount = new ArrayList<>();
        toCount.add(countedUp);
        toCount.add(new ItemStack(Items.COBBLESTONE, 3));
        check(report, failures, "sort by amount totals the real amounts", "192",
                String.valueOf(SortSelector.calculateCount(toCount).get(Items.COBBLESTONE)));

        // Change detection: the container menu decides what to send to the client with
        // ItemStack.matches, and container components use it for equality. For an oversized stack the
        // physical counts are always 1, so if a count-only change compares equal the client is never
        // told and keeps showing the stale amount.
        VirtualStack.setCount(cobble, 300L);
        ItemStack before = cobble.copy();
        ItemStack after = cobble.copy();
        VirtualStack.setCount(after, 299L);
        check(report, failures, "a count change counts as a change", "false",
                Boolean.toString(ItemStack.matches(before, after)));
        check(report, failures, "the same amount still matches", "true",
                Boolean.toString(ItemStack.matches(before, cobble)));
        check(report, failures, "a different item is a change", "false",
                Boolean.toString(ItemStack.matches(before, new ItemStack(Items.DIRT))));

        for (String id : RECIPES) {
            boolean present = server.getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, id)))
                    .isPresent();
            check(report, failures, "recipe " + id, "true", Boolean.toString(present));
        }

        // Dragon scales: one per revived dragon, plus an independent 30% roll per Looting level.
        check(report, failures, "dragon scale is registered", "travelersbackpackextras:dragon_scale",
                String.valueOf(BuiltInRegistries.ITEM.getKey(ModItems.DRAGON_SCALE)));
        check(report, failures, "one scale is always dropped", "1",
                Integer.toString(DragonScaleDrop.BASE_SCALES));
        RandomSource rolls = RandomSource.create(20260930L);
        check(report, failures, "no looting means no extra scale", "0",
                Integer.toString(DragonScaleDrop.extraScales(0, rolls)));
        int samples = 20000;
        long totalExtra = 0L;
        int mostExtra = 0;
        for (int i = 0; i < samples; i++) {
            int extra = DragonScaleDrop.extraScales(3, rolls);
            totalExtra += extra;
            mostExtra = Math.max(mostExtra, extra);
        }
        double average = (double) totalExtra / samples;
        check(report, failures, "looting III can add up to three", "3", Integer.toString(mostExtra));
        check(report, failures, "looting III averages 0.9 extra scales", "true",
                Boolean.toString(average > 0.85D && average < 0.95D));

        report.append(checkMultipliers(failures));
        report.append(checkEndlessTank(failures));

        report.append(failures.isEmpty()
                ? "RESULT: PASS\n================================"
                : "RESULT: FAIL (" + failures.size() + ")\n================================");
        TravelersBackpackExtras.LOGGER.info(report.toString());
    }

    private static String checkMultipliers(List<String> failures) {
        StringBuilder out = new StringBuilder();
        ItemStack backpack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("travelersbackpack", "standard")));
        if (backpack.isEmpty()) {
            failures.add("travelersbackpack:standard missing");
            out.append("  backpack item missing\n");
            return out.toString();
        }
        // A leather backpack only has two upgrade slots; widen it so the stacking rules can be
        // exercised with three upgrades at once.
        backpack.set(com.tiviacz.travelersbackpack.init.ModDataComponents.UPGRADE_SLOTS, 6);
        try {
            BackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
            check(out, failures, "multiplier with no upgrade", "1", Long.toString(CapacityHelper.multiplier(wrapper)));
            check(out, failures, "test backpack has 3+ upgrade slots", "true",
                    Boolean.toString(wrapper.getUpgrades().getSlots() >= 3));

            setUpgrades(wrapper, CapacityTier.WOODEN);
            check(out, failures, "wooden multiplier", "2", Long.toString(CapacityHelper.multiplier(wrapper)));
            check(out, failures, "wooden slot limit (64 base)", "128",
                    Long.toString(CapacityHelper.slotLimit(wrapper, new ItemStack(Items.COBBLESTONE))));
            check(out, failures, "unstackable stays unstackable", "1",
                    Long.toString(CapacityHelper.slotLimit(wrapper, new ItemStack(Items.DIAMOND_SWORD))));
            check(out, failures, "16-stack item scales by its own limit", "32",
                    Long.toString(CapacityHelper.slotLimit(wrapper, new ItemStack(Items.SNOWBALL))));

            setUpgrades(wrapper, CapacityTier.WOODEN, CapacityTier.COPPER);
            check(out, failures, "wooden * copper", "16", Long.toString(CapacityHelper.multiplier(wrapper)));

            // Taking an upgrade out is allowed as long as the upgrades that stay behind still cover
            // what is stored: 1024 items need a 16x multiplier, so with three upgrades installed any
            // single one may be removed, while a lone wooden upgrade (2x, and only 1x left behind)
            // may not.
            wrapper.getStorage().setStackInSlot(0, oversized(Items.COBBLESTONE, 1024L));
            setUpgrades(wrapper, CapacityTier.WOODEN);
            check(out, failures, "1024 items need 16x", "16", Long.toString(CapacityHelper.requiredMultiplier(wrapper)));
            check(out, failures, "the last upgrade cannot be taken out", "false",
                    Boolean.toString(CapacityHelper.canRemoveUpgrade(wrapper, 0)));

            setUpgrades(wrapper, CapacityTier.WOODEN, CapacityTier.COPPER, CapacityTier.IRON);
            check(out, failures, "three upgrades hold 1024 items", "true",
                    Boolean.toString(CapacityHelper.canRemoveUpgrade(wrapper, 0)));
            check(out, failures, "any of the three may be removed", "true",
                    Boolean.toString(CapacityHelper.canRemoveUpgrade(wrapper, 1)
                            && CapacityHelper.canRemoveUpgrade(wrapper, 2)));
            check(out, failures, "removing the third leaves wooden * copper", "16",
                    Long.toString(CapacityHelper.multiplierWithout(wrapper, 2)));
            check(out, failures, "removing the second leaves wooden * iron", "32",
                    Long.toString(CapacityHelper.multiplierWithout(wrapper, 1)));

            setUpgrades(wrapper, CapacityTier.WOODEN, CapacityTier.COPPER);
            check(out, failures, "1024 items keep the pair together", "false",
                    Boolean.toString(CapacityHelper.canRemoveUpgrade(wrapper, 0)
                            || CapacityHelper.canRemoveUpgrade(wrapper, 1)));

            // A stack that fits in a vanilla slot never blocks anything.
            wrapper.getStorage().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
            check(out, failures, "64 items need no upgrade", "1",
                    Long.toString(CapacityHelper.requiredMultiplier(wrapper)));
            check(out, failures, "a 64 stack never blocks removal", "true",
                    Boolean.toString(CapacityHelper.canRemoveUpgrade(wrapper, 0)));
            wrapper.getStorage().setStackInSlot(0, ItemStack.EMPTY);

            setUpgrades(wrapper, CapacityTier.EMERALD, CapacityTier.DIAMOND, CapacityTier.NETHERITE);
            check(out, failures, "sub-ultimate product capped at 4096", "4096",
                    Long.toString(CapacityHelper.multiplier(wrapper)));
            check(out, failures, "netherite unlocks endless fluids", "true",
                    Boolean.toString(CapacityHelper.hasInfiniteFluidTier(wrapper)));

            setUpgrades(wrapper, CapacityTier.WOODEN);
            check(out, failures, "wooden does not unlock endless fluids", "false",
                    Boolean.toString(CapacityHelper.hasInfiniteFluidTier(wrapper)));

            setUpgrades(wrapper, CapacityTier.ULTIMATE, CapacityTier.OMEGA);
            check(out, failures, "omega multiplier", "33554432", Long.toString(CapacityHelper.multiplier(wrapper)));
            check(out, failures, "omega slot limit is 2^31", "2147483648",
                    Long.toString(CapacityHelper.slotLimit(wrapper, new ItemStack(Items.COBBLESTONE))));

            setUpgrades(wrapper, CapacityTier.OMEGA, CapacityTier.OMEGA, CapacityTier.OMEGA);
            check(out, failures, "omega copies change nothing", "33554432", Long.toString(CapacityHelper.multiplier(wrapper)));
            check(out, failures, "installed count", "3", Integer.toString(CapacityHelper.installedCount(wrapper)));
        } catch (RuntimeException e) {
            failures.add("wrapper construction failed: " + e);
            out.append("  wrapper failed: ").append(e).append('\n');
        }
        return out.toString();
    }

    /**
     * Exercises the endless tank rules directly on a {@code FluidTank}, which is where the subtle
     * cases live: the transfer API must still see a full tank, and the copy-and-write-back path used
     * by the fluid slot handler must not be able to end the endless state.
     */
    private static String checkEndlessTank(List<String> failures) {
        StringBuilder out = new StringBuilder();
        try {
            com.tiviacz.travelersbackpack.inventory.FluidTank tank = new com.tiviacz.travelersbackpack.inventory.FluidTank(162000L);
            com.tbupgrades.extras.api.InfiniteTank endless = (com.tbupgrades.extras.api.InfiniteTank) (Object) tank;
            endless.tbx$setEnabled(true);

            check(out, failures, "plain water is not endless", "false", Boolean.toString(endless.tbx$isInfinite()));

            tank.setFluid(new com.tiviacz.travelersbackpack.inventory.FluidVariantWrapper(
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 3000L));
            check(out, failures, "two buckets plus a drop is endless", "true", Boolean.toString(endless.tbx$isInfinite()));
            check(out, failures, "endless reports full capacity as amount", "162000", Long.toString(tank.getAmount()));
            check(out, failures, "endless reports full capacity as fluid amount", "162000", Long.toString(tank.getFluidAmount()));

            try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx =
                         net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long extracted = tank.extract(
                        net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 81000L, tx);
                check(out, failures, "endless yields a whole bucket", "81000", Long.toString(extracted));
                tx.abort();
            }
            check(out, failures, "extracting does not drain it", "3000",
                    Long.toString(tank.getFluid().getAmount()));

            // The fluid slot handler fills a bucket through a copy of the tank and writes the result
            // back; that must not be able to end the endless state.
            tank.setFluid(new com.tiviacz.travelersbackpack.inventory.FluidVariantWrapper(
                    net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 2000L));
            check(out, failures, "write back cannot end the endless state", "3000",
                    Long.toString(tank.getFluid().getAmount()));

            check(out, failures, "water at exactly two buckets is not endless", "false", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.WATER, 2000L, true)));
            check(out, failures, "water above two buckets is endless", "true", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.WATER, 2001L, true)));
            check(out, failures, "flowing water is endless too", "true", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.FLOWING_WATER, 9000L, true)));
            check(out, failures, "lava at exactly 10000 buckets is not endless", "false", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.LAVA, 10_000_000L, true)));
            check(out, failures, "lava above 10000 buckets is endless", "true", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.LAVA, 10_000_001L, true)));
            check(out, failures, "potion is never endless", "false", Boolean.toString(
                    isEndless(com.tiviacz.travelersbackpack.init.ModFluids.POTION_STILL, 900_000_000L, true)));
            check(out, failures, "without a netherite tier nothing is endless", "false", Boolean.toString(
                    isEndless(net.minecraft.world.level.material.Fluids.WATER, 900_000_000L, false)));

            endless.tbx$clear();
            check(out, failures, "clearing empties the tank", "true", Boolean.toString(tank.isEmpty()));
            check(out, failures, "cleared tank is no longer endless", "false", Boolean.toString(endless.tbx$isInfinite()));
        } catch (RuntimeException e) {
            failures.add("endless tank check failed: " + e);
            out.append("  endless tank check threw: ").append(e).append('\n');
        }
        return out.toString();
    }

    private static boolean isEndless(net.minecraft.world.level.material.Fluid fluid, long amount, boolean enabled) {
        com.tiviacz.travelersbackpack.inventory.FluidTank tank =
                new com.tiviacz.travelersbackpack.inventory.FluidTank(1_000_000_000L);
        com.tbupgrades.extras.api.InfiniteTank endless = (com.tbupgrades.extras.api.InfiniteTank) (Object) tank;
        endless.tbx$setEnabled(enabled);
        tank.setFluid(new com.tiviacz.travelersbackpack.inventory.FluidVariantWrapper(
                net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid), amount));
        return endless.tbx$isInfinite();
    }

    /** A stack holding {@code count} items, in the oversized representation when it has to be. */
    private static ItemStack oversized(Item item, long count) {
        ItemStack stack = new ItemStack(item);
        VirtualStack.setCount(stack, count);
        return stack;
    }

    private static void setUpgrades(BackpackWrapper wrapper, CapacityTier... tiers) {
        for (int i = 0; i < wrapper.getUpgrades().getSlots(); i++) {
            wrapper.getUpgrades().setStackInSlot(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < tiers.length && i < wrapper.getUpgrades().getSlots(); i++) {
            wrapper.getUpgrades().setStackInSlot(i, new ItemStack(ModItems.CAPACITY_UPGRADES.get(tiers[i])));
        }
    }

    private static void check(StringBuilder out, List<String> failures, String what, String expected, String actual) {
        boolean ok = expected.equals(actual);
        if (!ok) {
            failures.add(what + ": expected " + expected + " but was " + actual);
        }
        out.append(ok ? "  OK   " : "  FAIL ").append(what).append(" = ").append(actual).append('\n');
    }
}
