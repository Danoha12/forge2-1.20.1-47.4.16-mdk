package com.trolmastercard.sexmod.client.handler;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.AcceptSexProposalPacket;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * SexProposalManager — Portado a 1.20.1.
 * * Maneja las propuestas de interacciones entrantes en el cliente.
 * * Muestra mensajes formateados, gestiona el tiempo de expiración e intercepta el chat.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SexProposalManager {

  public static final SexProposalManager INSTANCE = new SexProposalManager();
  private SexProposalManager() {}

  @Nullable
  private Proposal currentProposal = null;

  // ── Lógica de Tiempo (Expiración) ────────────────────────────────────────

  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END || INSTANCE.currentProposal == null) return;

    INSTANCE.currentProposal.timeoutTicks--;

    if (INSTANCE.currentProposal.timeoutTicks <= 0) {
      if (Minecraft.getInstance().player != null) {
        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal(ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.timeout"))
        );
      }
      INSTANCE.dismiss();
    }
  }

  // ── Intercepción del Chat (Aceptar/Rechazar) ─────────────────────────────

  @SubscribeEvent
  public static void onClientChat(ClientChatEvent event) {
    if (INSTANCE.currentProposal == null) return;

    // Limpiamos el mensaje para comparar (quitamos espacios y pasamos a minúsculas)
    String msg = event.getMessage().trim().toLowerCase();

    String acceptKey = I18n.get("genderswap.sexpromt.accept").toLowerCase();
    String declineKey = I18n.get("genderswap.sexpromt.decline").toLowerCase();

    if (msg.equals(acceptKey)) {
      INSTANCE.sendAccept();
      INSTANCE.dismiss();
      event.setCanceled(true); // Evita que "aceptar" se envíe como un mensaje de chat normal
    } else if (msg.equals(declineKey)) {
      if (Minecraft.getInstance().player != null) {
        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal(ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.declineconformation"))
        );
      }
      INSTANCE.dismiss();
      event.setCanceled(true);
    }
  }

  // ── API Pública ───────────────────────────────────────────────────────────

  public void showProposal(@Nonnull Proposal proposal) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;

    Player requester = mc.level.getPlayerByUUID(proposal.requesterUUID);
    Player target = mc.level.getPlayerByUUID(proposal.targetUUID);
    if (requester == null || target == null) return;

    String askerName = proposal.requesterIsTarget ? target.getName().getString() : requester.getName().getString();
    String actName = I18n.get(proposal.actTranslationKey);

    // Mensaje: "[Nombre] te ha pedido una interacción: [Acto]"
    mc.player.sendSystemMessage(Component.literal(
            ChatFormatting.LIGHT_PURPLE + askerName + " " +
                    ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.playerxaskedfory") + " " +
                    ChatFormatting.LIGHT_PURPLE + actName
    ));

    // Mensaje: "Esta propuesta expirará pronto..."
    mc.player.sendSystemMessage(Component.literal(
            ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.autodeletion")
    ));

    // Instrucciones: "[ Aceptar | Rechazar ]"
    mc.player.sendSystemMessage(Component.literal(
            ChatFormatting.DARK_PURPLE + "[ " +
                    ChatFormatting.LIGHT_PURPLE + I18n.get("genderswap.sexpromt.accept") +
                    ChatFormatting.DARK_PURPLE + " | " +
                    ChatFormatting.LIGHT_PURPLE + I18n.get("genderswap.sexpromt.decline") +
                    ChatFormatting.DARK_PURPLE + " ]"
    ));

    this.currentProposal = proposal;
  }

  public void dismiss() {
    this.currentProposal = null;
  }

  private void sendAccept() {
    if (currentProposal != null) {
      ModNetwork.CHANNEL.sendToServer(new AcceptSexProposalPacket(
              currentProposal.requesterUUID,
              currentProposal.targetUUID,
              currentProposal.actTranslationKey
      ));
    }
  }

  // ── Clase de Datos (Proposal Record) ─────────────────────────────────────

  public static class Proposal {
    public final String actTranslationKey;
    public final UUID targetUUID;
    public final UUID requesterUUID;
    public final boolean requesterIsTarget;
    public int timeoutTicks; // Cambiado a int (Ticks de juego)

    public Proposal(String actTranslationKey, UUID targetUUID, UUID requesterUUID, boolean requesterIsTarget) {
      this.actTranslationKey = actTranslationKey;
      this.targetUUID = targetUUID;
      this.requesterUUID = requesterUUID;
      this.requesterIsTarget = requesterIsTarget;
      this.timeoutTicks = 1200; // 60 segundos (20 ticks * 60)
    }
  }
}