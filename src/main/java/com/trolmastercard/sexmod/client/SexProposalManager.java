package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.AcceptSexProposalPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * SexProposalManager - ported from w.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Client-side singleton that manages incoming sex-act proposals.
 * When a proposal arrives, it:
 *  1. Displays a formatted chat message showing who is asking and for what act
 *  2. Shows an auto-deletion countdown
 *  3. Shows [Accept | Decline] instructions
 *  4. Listens to the next chat message - if it matches the accept/decline
 *     keys, it fires the appropriate response packet
 *
 * The countdown timer is decremented by the client tick handler every tick;
 * when it reaches zero the proposal is automatically dismissed.
 *
 * Singleton access: {@code SexProposalManager.INSTANCE}
 */
@OnlyIn(Dist.CLIENT)
public class SexProposalManager {

    // =========================================================================
    //  Singleton
    // =========================================================================

    public static final SexProposalManager INSTANCE = new SexProposalManager();

    private SexProposalManager() {}

    // =========================================================================
    //  State
    // =========================================================================

    /** Currently pending proposal, or null if none. */
    @Nullable
    private Proposal currentProposal = null;

    // =========================================================================
    //  Public API
    // =========================================================================

    /** Called every client tick to count down the auto-dismiss timer. */
    public void tick() {
        if (currentProposal == null) return;
        if (--currentProposal.timeoutTicks <= 0) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.literal(ChatFormatting.DARK_PURPLE
                    + I18n.get("genderswap.sexpromt.timeout")));
            dismiss();
        }
    }

    @Nullable
    public Proposal getCurrentProposal() {
        return currentProposal;
    }

    /** Clears the current proposal without sending any packet. */
    public void dismiss() {
        currentProposal = null;
    }

    /**
     * Presents a new proposal to the local player via chat messages.
     *
     * @param proposal  the proposal data to display
     */
    public void showProposal(@Nonnull Proposal proposal) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Player requester = level.getPlayerByUUID(proposal.requesterUUID);
        Player target    = level.getPlayerByUUID(proposal.targetUUID);
        if (requester == null || target == null) return;

        String askerName = proposal.requesterIsTarget
            ? target.getName().getString()
            : requester.getName().getString();

        String actName = I18n.get(proposal.actTranslationKey);

        Player localPlayer = Minecraft.getInstance().player;
        localPlayer.sendSystemMessage(Component.literal(
            ChatFormatting.LIGHT_PURPLE + askerName + " "
            + ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.playerxaskedfory") + " "
            + ChatFormatting.LIGHT_PURPLE + actName));
        localPlayer.sendSystemMessage(Component.literal(
            ChatFormatting.DARK_PURPLE + I18n.get("genderswap.sexpromt.autodeletion")));
        localPlayer.sendSystemMessage(Component.literal(
            ChatFormatting.DARK_PURPLE + "[ "
            + ChatFormatting.LIGHT_PURPLE + I18n.get("genderswap.sexpromt.accept")
            + ChatFormatting.DARK_PURPLE + " | "
            + ChatFormatting.LIGHT_PURPLE + I18n.get("genderswap.sexpromt.decline")
            + ChatFormatting.DARK_PURPLE + " ]"));

        this.currentProposal = proposal;
    }

    // =========================================================================
    //  Chat intercept
    // =========================================================================

    /**
     * Intercepts the player's chat input to check for accept/decline responses.
     * If matched, fires the appropriate packet and cancels the chat event.
     */
    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        if (currentProposal == null) return;

        String msg = event.getMessage().toLowerCase();

        String acceptKey  = I18n.get("genderswap.sexpromt.accept").toLowerCase();
        String declineKey = I18n.get("genderswap.sexpromt.decline").toLowerCase();

        if (msg.equals(acceptKey)) {
            sendAccept(currentProposal.actTranslationKey,
                       currentProposal.requesterUUID,
                       currentProposal.targetUUID);
            dismiss();
            event.setCanceled(true);
            return;
        }

        if (msg.equals(declineKey)) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                ChatFormatting.DARK_PURPLE
                + I18n.get("genderswap.sexpromt.declineconformation")));
            dismiss();
            event.setCanceled(true);
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Sends the accept packet to the server. */
    private void sendAccept(String actKey, UUID requesterUUID, UUID targetUUID) {
        ModNetwork.CHANNEL.sendToServer(
            new AcceptSexProposalPacket(requesterUUID, targetUUID, actKey));
    }

    // =========================================================================
    //  Data class
    // =========================================================================

    /**
     * Immutable proposal record.
     *
     * Fields:
     *  - {@code actTranslationKey}  - i18n key for the requested sex act
     *  - {@code targetUUID}         - UUID of the player receiving the proposal
     *  - {@code requesterUUID}      - UUID of the player making the proposal
     *  - {@code requesterIsTarget}  - if true, display the target's name; else the requester's
     *  - {@code timeoutTicks}       - mutable countdown (starts at 1200 = 60 seconds)
     *
     * Equivalent to the original inner class {@code w.a}.
     */
    public static class Proposal {
        public final String  actTranslationKey;
        public final UUID    targetUUID;
        public final UUID    requesterUUID;
        public final boolean requesterIsTarget;
        public       float   timeoutTicks;

        public Proposal(String actTranslationKey,
                        UUID   targetUUID,
                        UUID   requesterUUID,
                        boolean requesterIsTarget) {
            this.actTranslationKey  = actTranslationKey;
            this.targetUUID         = targetUUID;
            this.requesterUUID      = requesterUUID;
            this.requesterIsTarget  = requesterIsTarget;
            this.timeoutTicks       = 1200.0F;   // 60 seconds
        }
    }
}
