package com.tbupgrades.extras.common;

import com.tbupgrades.extras.init.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.dimension.end.EndDragonFight;

/**
 * Dragon scales come from an Ender Dragon the players brought back, not from the first one.
 *
 * <p>Reviving the dragon is the only repeatable boss fight in the game, and an elytra is the reward
 * that normally exists exactly once per world. Tying the scale to a revived dragon keeps the first
 * kill special (it still gives the egg and the advancement) while making the elytra recipe
 * renewable, which is the whole point of it.
 *
 * <p>{@link EndDragonFight#hasPreviouslyKilledDragon()} is the flag the arena itself keeps for "a
 * dragon has already been defeated here", so it is true precisely when the dragon being killed was a
 * revived one. The event fires from inside {@code LivingEntity.die}, before the arena records this
 * kill, so the first dragon still reads as "never killed here".
 *
 * <p>Looting applies: one scale is guaranteed, and each level on the killer's weapon is an
 * independent 30% roll for one more.
 */
public final class DragonScaleDrop {
    /** Scales a revived dragon always leaves behind. */
    public static final int BASE_SCALES = 1;

    /** Chance per Looting level of one extra scale. */
    public static final float LOOTING_EXTRA_CHANCE = 0.3F;

    private DragonScaleDrop() {
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register(DragonScaleDrop::onDeath);
    }

    /**
     * The extra scales the killer's Looting earns: one independent roll per level, so Looting III can
     * add up to three.
     */
    public static int extraScales(int lootingLevel, RandomSource random) {
        int extra = 0;
        for (int i = 0; i < lootingLevel; i++) {
            if (random.nextFloat() < LOOTING_EXTRA_CHANCE) {
                extra++;
            }
        }
        return extra;
    }

    private static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) {
            return;
        }
        EndDragonFight fight = level.getDragonFight();
        if (fight == null || !fight.hasPreviouslyKilledDragon()) {
            return;
        }

        Player killer = source.getEntity() instanceof Player player ? player : null;
        ItemStack scales = new ItemStack(ModItems.DRAGON_SCALE, BASE_SCALES + extraScales(lootingLevel(level, killer), level.random));
        if (killer != null) {
            // Straight into the killer's inventory: the fight happens right above the exit portal,
            // and an item dropped in that spot can be swallowed by the portal and sent to the
            // overworld spawn. placeItemBackInInventory still drops it at the player's feet if the
            // inventory happens to be full.
            killer.getInventory().placeItemBackInInventory(scales);
        } else {
            level.addFreshEntity(new ItemEntity(level, dragon.getX(), dragon.getY(), dragon.getZ(), scales));
        }
    }

    /** Looting on the killer's weapon; 0 for anything that is not a player. */
    private static int lootingLevel(ServerLevel level, Player killer) {
        if (killer == null) {
            return 0;
        }
        try {
            Registry<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> looting = enchantments.getOrThrow(Enchantments.LOOTING);
            return EnchantmentHelper.getEnchantmentLevel(looting, killer);
        } catch (RuntimeException e) {
            // A datapack without the looting enchantment must not break the kill.
            return 0;
        }
    }
}
