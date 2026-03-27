package com.trolmastercard.sexmod.item;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * NpcEditorWandItem - ported from hj.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Developer tool item. When held:
 *   - Right-clicking an NPC - opens the NPC customise screen
 *   - Left-clicking an NPC  - prints/copies the NPC's model code to chat+clipboard
 *   - Left-clicking empty   - reads the player's own PlayerKobold model code
 *
 * The "damage" value (0 or 1) toggles the item texture between "inactive" and
 * "active" depending on whether the crosshair is hovering over a valid NPC.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - Item extends Item; func_77637_a(CreativeTabs) - constructor Item.Properties
 *   - field_77777_bU = 1 - .stacksTo(1) in Properties
 *   - func_77663_a(stack, world, entity, slot, selected) - inventoryTick(...)
 *   - ModelLoader.setCustomModelResourceLocation - use item model JSON
 *   - TileEntityItemStackRenderer - BlockEntityWithoutLevelRenderer (not needed here, no geo)
 *   - Minecraft.func_71410_x().field_71476_x - mc.hitResult
 *   - func_77964_b(damage) - stack.setDamageValue(damage)
 *   - func_184614_ca() / func_184592_cb() - getMainHandItem() / getOffhandItem()
 *   - be.a(str) - ModUtil.copyToClipboard(str)
 *   - func_145747_a(component) - sendSystemMessage(component)
 *   - func_146105_b(component, bool) - displayClientMessage(component, bool)
 *   - em.C() - npc.getModelCode()
 *   - em.h(em.f()) / em.c(code) - BaseNpcEntity helpers for variant code
 *   - ei.d(uuid) - PlayerKoboldEntity.getForPlayer(uuid)
 *   - fy.a(entity) - NpcType.getTypeOf(entity)
 *   - br.d / br.b(true) - CustomModelManager.isDevMode / loadCount
 *   - a.a(npc.E()) - NpcCustomizeScreen.open(npc.getState())
 */
public class NpcEditorWandItem extends Item {

    public static final NpcEditorWandItem INSTANCE = new NpcEditorWandItem();

    public NpcEditorWandItem() {
        super(new Properties()
                .tab(CreativeModeTab.TAB_MISC)
                .stacksTo(1)
                .durability(1));   // 0=inactive, 1=active texture
    }

    // -- Inventory tick (update damage/texture) ---------------------------------

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide() && holder instanceof Player) {
            updateActiveState(holder, stack);
        }
        super.inventoryTick(stack, level, holder, slot, selected);
    }

    /** Sets stack damage to 1 (active) if crosshair points at a valid NPC, 0 otherwise. */
    private void updateActiveState(Entity holder, ItemStack stack) {
        if (!(holder instanceof Player player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        if (!main.equals(stack) && !off.equals(stack)) {
            stack.setDamageValue(0);
            return;
        }
        var hit = Minecraft.getInstance().hitResult;
        boolean active = hit != null &&
                hit instanceof net.minecraft.world.phys.EntityHitResult ehr &&
                isValidNpc(ehr.getEntity());
        stack.setDamageValue(active ? 1 : 0);
    }

    private boolean isValidNpc(Entity entity) {
        return entity instanceof BaseNpcEntity && BaseNpcEntity.isValidTarget(entity);
    }

    // -- Right-click NPC - open customise screen --------------------------------

    @SubscribeEvent
    public void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!(target instanceof BaseNpcEntity npc)) return;
        if (!BaseNpcEntity.isValidTarget(target)) return;

        Player player = event.getEntity();
        if (player == null) return;

        ItemStack held = getHeldWand(player);
        if (held.isEmpty()) return;

        event.setCanceled(true);
        if (!event.getLevel().isClientSide()) return;

        // Custom-model dev-mode check
        if (CustomModelManager.isDevMode) {
            CustomModelManager.isDevMode = CustomModelManager.getLoadedModelCount(true) != 0;
            if (CustomModelManager.isDevMode) return;
        }

        NpcCustomizeScreen.open(npc.getEntityState());
    }

    // -- Left-click NPC - print model code -------------------------------------

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target == null || !(target instanceof BaseNpcEntity npc)) return;

        Player player = event.getEntity();
        if (player == null) return;

        ItemStack held = getHeldWand(player);
        if (held.isEmpty()) return;

        event.setCanceled(true);
        if (!player.level.isClientSide()) return;

        String modelCode  = npc.getModelCode();
        String variantCode = BaseNpcEntity.getVariantCode(BaseNpcEntity.getNpcVariant(npc.getNpcType()));
        String fullCode    = modelCode + "$" + variantCode;

        player.sendSystemMessage(Component.literal(
                npc.getName().getString() + "'s model-code: " +
                ChatFormatting.YELLOW + fullCode));
        player.sendSystemMessage(Component.literal(
                ChatFormatting.ITALIC + "copied to clipboard"));
        ModUtil.copyToClipboard(fullCode);
    }

    // -- Left-click block / empty - print player's own model code --------------

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (printSelfModelCode(event.getEntity(), event.getLevel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        printSelfModelCode(event.getEntity(), event.getLevel());
    }

    private boolean printSelfModelCode(Player player, Level level) {
        if (player == null) return false;
        ItemStack held = getHeldWand(player);
        if (held.isEmpty()) return false;
        if (!level.isClientSide()) return true;

        PlayerKoboldEntity pk = PlayerKoboldEntity.getForPlayer(player.getUUID());
        if (pk == null) {
            player.displayClientMessage(
                    Component.literal("you gotta turn into the girl, you want to copy the model-code off"),
                    true);
            return true;
        }

        String modelCode   = pk.getModelCode();
        String variantCode = BaseNpcEntity.getVariantCode(BaseNpcEntity.getNpcVariant(pk.getNpcType()));
        String fullCode    = modelCode + "$" + variantCode;

        player.sendSystemMessage(Component.literal(
                NpcType.getTypeOf(pk).toString() + "'s model-code: " +
                ChatFormatting.YELLOW + fullCode));
        player.sendSystemMessage(Component.literal(
                ChatFormatting.ITALIC + "copied to clipboard"));
        ModUtil.copyToClipboard(fullCode);
        return true;
    }

    // -- Registration -----------------------------------------------------------

    public static void register() {
        INSTANCE.setRegistryName("sexmod", "npc_editor_wand");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(NpcEditorWandItem.class);
    }

    // -- Helpers ----------------------------------------------------------------

    /** Returns the wand ItemStack from the player's hands, or EMPTY. */
    private ItemStack getHeldWand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == INSTANCE) return main;
        ItemStack off  = player.getOffhandItem();
        if (off.getItem() == INSTANCE) return off;
        return ItemStack.EMPTY;
    }
}
