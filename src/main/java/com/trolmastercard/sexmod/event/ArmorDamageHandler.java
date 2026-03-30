package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.entity.NpcInventoryEntity; // Asumiendo que esta es la ruta
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * ArmorDamageHandler — Portado a 1.20.1 y optimizado.
 *
 * Cálculos personalizados de reducción de daño para NPCs que usan
 * un inventario interno en lugar de los slots de armadura vainilla.
 */
@Mod.EventBusSubscriber
public class ArmorDamageHandler {

    private static final Random RAND = new Random();
    private static final ArmorTable TABLE = new ArmorTable();

    // Inicialización estática de la tabla de valores de armadura
    static {
        // HEAD
        TABLE.add(EquipmentSlot.HEAD, ArmorMaterials.LEATHER, 1, 0);
        TABLE.add(EquipmentSlot.HEAD, ArmorMaterials.GOLD,    2, 0);
        TABLE.add(EquipmentSlot.HEAD, ArmorMaterials.CHAIN,   2, 0);
        TABLE.add(EquipmentSlot.HEAD, ArmorMaterials.IRON,    2, 0);
        TABLE.add(EquipmentSlot.HEAD, ArmorMaterials.DIAMOND, 3, 3);
        // CHEST
        TABLE.add(EquipmentSlot.CHEST, ArmorMaterials.LEATHER, 3, 0);
        TABLE.add(EquipmentSlot.CHEST, ArmorMaterials.GOLD,    5, 0);
        TABLE.add(EquipmentSlot.CHEST, ArmorMaterials.CHAIN,   5, 0);
        TABLE.add(EquipmentSlot.CHEST, ArmorMaterials.IRON,    6, 0);
        TABLE.add(EquipmentSlot.CHEST, ArmorMaterials.DIAMOND, 8, 3);
        // LEGS
        TABLE.add(EquipmentSlot.LEGS, ArmorMaterials.LEATHER, 2, 0);
        TABLE.add(EquipmentSlot.LEGS, ArmorMaterials.GOLD,    3, 0);
        TABLE.add(EquipmentSlot.LEGS, ArmorMaterials.CHAIN,   4, 0);
        TABLE.add(EquipmentSlot.LEGS, ArmorMaterials.IRON,    5, 0);
        TABLE.add(EquipmentSlot.LEGS, ArmorMaterials.DIAMOND, 6, 3);
        // FEET
        TABLE.add(EquipmentSlot.FEET, ArmorMaterials.LEATHER, 1, 0);
        TABLE.add(EquipmentSlot.FEET, ArmorMaterials.GOLD,    1, 0);
        TABLE.add(EquipmentSlot.FEET, ArmorMaterials.CHAIN,   1, 0);
        TABLE.add(EquipmentSlot.FEET, ArmorMaterials.IRON,    2, 0);
        TABLE.add(EquipmentSlot.FEET, ArmorMaterials.DIAMOND, 3, 3);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // Ignorar si no es nuestro NPC con inventario
        if (!(event.getEntity() instanceof NpcInventoryEntity npcEntity)) return;

        // Recolectar piezas de armadura de los slots del inventario personalizado (asumiendo 2-5)
        ItemStack[] armorSlots = {
                npcEntity.getInventory().getStackInSlot(2),
                npcEntity.getInventory().getStackInSlot(3),
                npcEntity.getInventory().getStackInSlot(4),
                npcEntity.getInventory().getStackInSlot(5)
        };

        ArrayList<ArmorItem>  armorItems  = new ArrayList<>();
        ArrayList<ItemStack>  armorStacks = new ArrayList<>();

        for (ItemStack stack : armorSlots) {
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armor) {
                armorItems.add(armor);
                armorStacks.add(stack);
            }
        }

        if (armorItems.isEmpty()) return;

        var source = event.getSource();

        // Totales base de armadura y dureza
        int totalArmor    = 0;
        int totalToughness = 0;

        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            for (ArmorItem armor : armorItems) {
                totalArmor    += TABLE.getProtection(armor.getEquipmentSlot(), armor.getMaterial());
                totalToughness += TABLE.getToughness(armor.getEquipmentSlot(), armor.getMaterial());
            }
        }

        float dmg = event.getAmount();

        // Reducción de armadura estilo vainilla
        dmg *= 1.0f - Math.min(20.0f, Math.max(
                totalArmor / 5.0f,
                totalArmor - 4.0f * dmg / (totalToughness + 8.0f))) / 25.0f;

        // Reducciones por encantamientos
        float thorns = 0.0f;
        for (ItemStack stack : armorStacks) {
            int prot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
            dmg -= prot * 0.04f * dmg;

            int t = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.THORNS, stack);
            thorns += (RAND.nextFloat() < 0.15f * t) ? (RAND.nextFloat() * 4.0f + 1.0f) : 0.0f;
            thorns = Math.min(4.0f, thorns);

            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                int proj = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, stack);
                dmg -= proj * 0.08f * dmg;
            }
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                int blast = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, stack);
                dmg -= blast * 0.08f * dmg;
            }
            if (source.is(DamageTypeTags.IS_FALL)) {
                int feather = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FALL_PROTECTION, stack);
                dmg -= feather * 0.12f * dmg;
            }
            if (source.is(DamageTypeTags.IS_FIRE)) {
                int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
                dmg -= fire * 0.08f * dmg;
            }
        }

        // Daño de espinas al atacante
        if (thorns > 0.0f && source.getDirectEntity() != null) {
            source.getDirectEntity().hurt(
                    npcEntity.level().damageSources().thorns(npcEntity), thorns);
        }

        event.setAmount(Math.max(dmg, 0.0f)); // Asegurar que el daño no sea negativo
    }

    // ── Inner armor table ────────────────────────────────────────────────────

    static class ArmorTable {
        final HashMap<String, int[]> data = new HashMap<>();

        void add(EquipmentSlot slot, ArmorMaterial mat, int protection, int toughness) {
            data.put(key(slot, mat), new int[]{protection, toughness});
        }

        int getProtection(EquipmentSlot slot, ArmorMaterial mat) {
            int[] v = data.get(key(slot, mat));
            return v != null ? v[0] : 3; // Valor por defecto si no se encuentra
        }

        int getToughness(EquipmentSlot slot, ArmorMaterial mat) {
            int[] v = data.get(key(slot, mat));
            return v != null ? v[1] : 0; // Valor por defecto si no se encuentra
        }

        private String key(EquipmentSlot slot, ArmorMaterial mat) {
            // En 1.20.1, ArmorMaterial sigue siendo una interfaz pero mat.getName() es el estándar
            return slot.getName() + mat.getName();
        }
    }
}