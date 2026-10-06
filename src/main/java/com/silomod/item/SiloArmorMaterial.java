package com.silomod.item;

import com.silomod.SiloMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum SiloArmorMaterial implements ArmorMaterial {
    CLEANING_SUIT;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 600; // ~10 минут снаружи (1 единица в секунду)
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET, BOOTS -> 1;
            case LEGGINGS -> 2;
            case CHESTPLATE -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return SiloMod.MODID + ":cleaning_suit";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
