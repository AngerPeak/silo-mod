package com.silomod.registry;

import com.silomod.SiloMod;
import com.silomod.item.JournalItem;
import com.silomod.item.SiloArmorMaterial;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SiloMod.MODID);

    public static final RegistryObject<Item> JOURNAL = ITEMS.register("journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> RATION = ITEMS.register("ration",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(6).saturationMod(0.6F).build())));

    public static final RegistryObject<Item> BADGE = ITEMS.register("badge",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MEDALLION = ITEMS.register("medallion",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KEY_FRAGMENT = ITEMS.register("key_fragment",
            () -> new Item(new Item.Properties().stacksTo(3)));

    public static final RegistryObject<Item> ACCESS_KEY = ITEMS.register("access_key",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUIT_HELMET = ITEMS.register("suit_helmet",
            () -> new ArmorItem(SiloArmorMaterial.CLEANING_SUIT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SUIT_CHESTPLATE = ITEMS.register("suit_chestplate",
            () -> new ArmorItem(SiloArmorMaterial.CLEANING_SUIT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SUIT_LEGGINGS = ITEMS.register("suit_leggings",
            () -> new ArmorItem(SiloArmorMaterial.CLEANING_SUIT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SUIT_BOOTS = ITEMS.register("suit_boots",
            () -> new ArmorItem(SiloArmorMaterial.CLEANING_SUIT, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> TERMINAL = ITEMS.register("terminal",
            () -> new BlockItem(ModBlocks.TERMINAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> SUPPLY_CRATE = ITEMS.register("supply_crate",
            () -> new BlockItem(ModBlocks.SUPPLY_CRATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> EVIDENCE = ITEMS.register("evidence",
            () -> new BlockItem(ModBlocks.EVIDENCE.get(), new Item.Properties()));
    public static final RegistryObject<Item> AIRLOCK = ITEMS.register("airlock",
            () -> new BlockItem(ModBlocks.AIRLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> MACHINE = ITEMS.register("machine",
            () -> new BlockItem(ModBlocks.MACHINE.get(), new Item.Properties()));

    private ModItems() {}
}
