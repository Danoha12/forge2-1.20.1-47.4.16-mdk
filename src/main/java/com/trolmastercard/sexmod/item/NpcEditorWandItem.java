package com.trolmastercard.sexmod.item; // Ajusta tu paquete

import com.trolmastercard.sexmod.client.gui.NpcCustomizeScreen;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NpcEditorWandItem — Portado a 1.20.1.
 * * Click Derecho NPC: Abre menú de personalización.
 * * Click Izquierdo NPC: Copia el código de modelo al portapapeles.
 * * Click Izquierdo Aire: Copia tu propio código de modelo (si eres una chica).
 */
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcEditorWandItem extends Item {

    public NpcEditorWandItem() {
        // Adiós a la durabilidad 2. ¡Ya no la necesitamos para la textura!
        super(new Item.Properties().stacksTo(1));
    }

    // ── Lógica de Interacción (Eventos de Forge) ──────────────────────────────

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof BaseNpcEntity npc)) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (held.getItem() instanceof NpcEditorWandItem) {
            event.setCanceled(true); // Cancela para que no pase el clic al juego base
            if (event.getLevel().isClientSide()) {
                openCustomizationScreen(npc);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof BaseNpcEntity npc)) return;

        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof NpcEditorWandItem) {
            event.setCanceled(true);
            if (player.level().isClientSide()) {
                copyNpcCode(player, npc);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof NpcEditorWandItem) {
            if (player.level().isClientSide()) {
                copySelfCode(player);
            }
        }
    }

    // ── Helpers de Cliente Aislados (Previene crashes en Servidor) ────────────

    @OnlyIn(Dist.CLIENT)
    private static void openCustomizationScreen(BaseNpcEntity npc) {
        // Al encapsular esto, el Servidor Dedicado nunca lee la clase Screen
        NpcCustomizeScreen.open(npc.getEntityState());
    }

    @OnlyIn(Dist.CLIENT)
    private static void copyNpcCode(Player player, BaseNpcEntity npc) {
        String fullCode = npc.getModelCode() + "$" + BaseNpcEntity.getVariantCode(npc.getNpcVariant());
        player.sendSystemMessage(Component.literal(npc.getName().getString() + " Code: ")
                .append(Component.literal(fullCode).withStyle(ChatFormatting.YELLOW)));
        ModUtil.copyToClipboard(fullCode);
    }

    @OnlyIn(Dist.CLIENT)
    private static void copySelfCode(Player player) {
        PlayerKoboldEntity pk = PlayerKoboldEntity.getForPlayer(player.getUUID());
        if (pk == null) {
            player.displayClientMessage(Component.literal("Debes estar transformado en una chica para copiar tu código")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        String fullCode = pk.getModelCode() + "$" + BaseNpcEntity.getVariantCode(pk.getNpcVariant());
        player.sendSystemMessage(Component.literal("Tu Code: ")
                .append(Component.literal(fullCode).withStyle(ChatFormatting.YELLOW)));
        ModUtil.copyToClipboard(fullCode);
    }
}