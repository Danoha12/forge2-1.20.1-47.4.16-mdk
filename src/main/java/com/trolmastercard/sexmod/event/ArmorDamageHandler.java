package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.ModConstants;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * ArmorDamageHandler - custom damage-reduction calculations for NpcInventoryEntity.
 * Ported from gu.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original obfuscation:
 *   gu        - ArmorDamageHandler
 *   e2        - NpcInventoryEntity
 *   e2.Q      - npc.getInventory() (ItemStackHandler)
 *   r.f       - ModConstants.RANDOM (shared Random)
 *
 * Migration notes:
 *   ItemArmor.ArmorMaterial enum - ArmorMaterial interface in 1.20.1;
 *   we key the protection table on ArmorMaterial reference instead.
 *   EntityEquipmentSlot - EquipmentSlot (no change in constant names).
 *   DamageSource helpers: isUnblockable() - isBypassArmor(), isProjectile() stays,
 *   isExplosion() stays, isFire() stays.
 *   DamageSource.field_76373_n.equals("fall") - source.is(DamageTypeTags.IS_FALL).
 */
public class ArmorDamageHandler {

    private static final Random RAND = new Random();
    private static final ArmorTable TABLE = new ArmorTable();

    public ArmorDamageHandler() {
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
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof NpcInventoryEntity npcEntity)) return;

        // Collect armour pieces from inventory slots 2-5
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

        // Base armor and toughness totals
        int totalArmor    = 0;
        int totalToughness = 0;
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            for (ArmorItem armor : armorItems) {
                totalArmor    += TABLE.getProtection(armor.getEquipmentSlot(), armor.getMaterial());
                totalToughness += TABLE.getToughness(armor.getEquipmentSlot(), armor.getMaterial());
            }
        }

        float dmg = event.getAmount();
        // Vanilla-style armor reduction
        dmg *= 1.0f - Math.min(20.0f, Math.max(
                totalArmor / 5.0f,
                totalArmor - 4.0f * dmg / (totalToughness + 8.0f))) / 25.0f;

        // Enchantment reductions
        float thorns = 0.0f;
        for (ItemStack stack : armorStacks) {
            int prot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
            dmg -= prot * 0.04f * dmg;

            int t = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.THORNS, stack);
            thorns += (RAND.nextFloat() < 0.15f * t) ? (RAND.nextFloat() * 4.0f + 1.0f) : 0.0f;
            thorns = Math.min(4.0f, thorns);

            if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                int proj = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, stack);
                dmg -= proj * 0.08f * dmg;
            }
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                int blast = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, stack);
                dmg -= blast * 0.08f * dmg;
            }
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                int feather = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FALL_PROTECTION, stack);
                dmg -= feather * 0.12f * dmg;
            }
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
                dmg -= fire * 0.08f * dmg;
            }
        }

        // Thorns damage to attacker
        if (thorns > 0.0f && source.getDirectEntity() != null) {
            source.getDirectEntity().hurt(
                    npcEntity.level().damageSources().thorns(npcEntity), thorns);
        }

        event.setAmount(dmg);
    }

    // -- Inner armor table ----------------------------------------------------

    static class ArmorTable {
        // In 1.20.1, ArmorMaterial is an interface/enum-like via ArmorMaterials class
        final HashMap<String, int[]> data = new HashMap<>();

        void add(EquipmentSlot slot, ArmorMaterial mat, int protection, int toughness) {
            data.put(key(slot, mat), new int[]{protection, toughness});
        }

        int getProtection(EquipmentSlot slot, ArmorMaterial mat) {
            int[] v = data.get(key(slot, mat));
            return v != null ? v[0] : 3;
        }

        int getToughness(EquipmentSlot slot, ArmorMaterial mat) {
            int[] v = data.get(key(slot, mat));
            return v != null ? v[1] : 0;
        }

        private String key(EquipmentSlot slot, ArmorMaterial mat) {
            return slot.getName() + mat.getName();
        }
    }
}
