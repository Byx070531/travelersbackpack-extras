package com.tbupgrades.extras.init;

import com.tbupgrades.extras.TravelersBackpackExtras;
import com.tbupgrades.extras.api.CapacityTier;
import com.tbupgrades.extras.common.TbxConfig;
import com.tbupgrades.extras.item.CapacityUpgradeItem;
import com.tiviacz.travelersbackpack.init.ModItemGroups;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ModItems {
    /** The nine storage upgrades, keyed by their tier. */
    public static final Map<CapacityTier, CapacityUpgradeItem> CAPACITY_UPGRADES = new LinkedHashMap<>();

    public static Item OMEGA_UPGRADE_SMITHING_TEMPLATE;
    public static Item ENCHANTED_DIAMOND_GOLDEN_APPLE;

    /**
     * Dropped by an Ender Dragon that the players revived. Reviving the dragon is the only renewable
     * source of an elytra in the game, and this scale is what turns that into a second elytra.
     */
    public static Item DRAGON_SCALE;

    /** Ten minutes, in ticks - duration of every potion effect of the apple. */
    public static final int APPLE_EFFECT_DURATION = 20 * 60 * 10;
    /** Half an hour, in ticks - duration of the granted creative flight. */
    public static final int APPLE_FLIGHT_DURATION = 20 * 60 * 30;

    private ModItems() {
    }

    public static void init() {
        for (CapacityTier tier : CapacityTier.values()) {
            Item.Properties properties = new Item.Properties().stacksTo(16).setId(key(tier.id() + "_upgrade"));
            if (tier.isOmega()) {
                properties = properties.rarity(Rarity.EPIC);
            }
            CapacityUpgradeItem item = new CapacityUpgradeItem(properties, tier);
            CAPACITY_UPGRADES.put(tier, Registry.register(BuiltInRegistries.ITEM, id(tier.id() + "_upgrade"), item));
        }

        OMEGA_UPGRADE_SMITHING_TEMPLATE = Registry.register(
                BuiltInRegistries.ITEM,
                id("omega_upgrade_smithing_template"),
                new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).setId(key("omega_upgrade_smithing_template"))) {
                    @Override
                    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                                Consumer<Component> consumer, TooltipFlag flag) {
                        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.omega_template").withStyle(ChatFormatting.GRAY));
                    }
                }
        );

        ENCHANTED_DIAMOND_GOLDEN_APPLE = Registry.register(
                BuiltInRegistries.ITEM,
                id("enchanted_diamond_golden_apple"),
                new Item(new Item.Properties()
                        .stacksTo(16)
                        .rarity(Rarity.EPIC)
                        .setId(key("enchanted_diamond_golden_apple"))
                        .food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.6F).alwaysEdible().build(), appleConsumable())
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)) {
                    @Override
                    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                                Consumer<Component> consumer, TooltipFlag flag) {
                        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.apple").withStyle(ChatFormatting.BLUE));
                        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.apple.flight").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
        );

        DRAGON_SCALE = Registry.register(
                BuiltInRegistries.ITEM,
                id("dragon_scale"),
                new Item(new Item.Properties().rarity(Rarity.RARE).setId(key("dragon_scale"))) {
                    @Override
                    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                                Consumer<Component> consumer, TooltipFlag flag) {
                        consumer.accept(Component.translatable("tooltip.travelersbackpackextras.dragon_scale").withStyle(ChatFormatting.GRAY));
                    }
                }
        );

        ItemGroupEvents.modifyEntriesEvent(ModItemGroups.TRAVELERS_BACKPACK).register(output -> {
            for (CapacityUpgradeItem item : CAPACITY_UPGRADES.values()) {
                output.accept(item);
            }
            output.accept(OMEGA_UPGRADE_SMITHING_TEMPLATE);
            output.accept(ENCHANTED_DIAMOND_GOLDEN_APPLE);
            output.accept(DRAGON_SCALE);
        });
    }

    /** Only used when the config allows the apple to grant flight. */
    public static Consumable appleConsumable() {
        Consumable.Builder builder = Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                        new MobEffectInstance(MobEffects.REGENERATION, APPLE_EFFECT_DURATION, 4),
                        new MobEffectInstance(MobEffects.RESISTANCE, APPLE_EFFECT_DURATION, 4),
                        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, APPLE_EFFECT_DURATION, 0),
                        new MobEffectInstance(MobEffects.ABSORPTION, APPLE_EFFECT_DURATION, 3)
                )));
        if (TbxConfig.appleGrantsFlight()) {
            builder.onConsume(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(ModEffects.CREATIVE_FLIGHT, APPLE_FLIGHT_DURATION, 0)));
        }
        return builder.build();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TravelersBackpackExtras.MOD_ID, path);
    }

    public static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }
}
