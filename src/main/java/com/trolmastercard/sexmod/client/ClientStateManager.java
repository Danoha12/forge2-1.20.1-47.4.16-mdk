package com.trolmastercard.sexmod.client;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ClientStateManager (d3) - Singleton event subscriber that intercepts
 * keyboard/mouse input while an animation is playing to freeze the player.
 *
 * canMove (c)  - true = normal input allowed; false = all movement blocked.
 * jumpPressed  - cached from the last input tick.
 * sneakPressed - cached from the last input tick.
 */
@OnlyIn(Dist.CLIENT)
public class ClientStateManager {

    private static boolean canMove = true;

    public static boolean jumpPressed  = false;
    public static boolean sneakPressed = false;

    // -- Event subscribers -----------------------------------------------------

    @SubscribeEvent
    public void onMovementInput(MovementInputUpdateEvent event) {
        var input = event.getInput();

        jumpPressed  = input.jumping;
        sneakPressed = input.shiftKeyDown;

        if (canMove) return;

        // Notify player how to exit the animation
        if (input.shiftKeyDown) {
            PlayerKoboldEntity.exitAnimationForAll();
        }
        if (input.jumping) {
            BaseNpcEntity.cancelAnimation(Minecraft.getInstance().player.getUUID());
        }
        if (input.shiftKeyDown && SexAnimationTracker.getProgress() >= 1.0) {
            BaseNpcEntity.advanceAnimation(Minecraft.getInstance().player.getUUID());
        }

        // Block all movement
        input.jumping      = false;
        input.shiftKeyDown = false;
        input.forwardImpulse = 0f;
        input.leftImpulse    = 0f;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.setDeltaMovement(0, 0, 0);
        }
    }

    // -- API -------------------------------------------------------------------

    public static boolean canMove() { return canMove; }

    public static void setCanMove(boolean value) {
        canMove = value;
        if (!value) showExitHint();
    }

    /** True if the "black screen" transition is active. */
    public static boolean isBlackScreenActive() {
        return !canMove && !SexAnimationTracker.canUseAllieLamp();
    }

    /** True if the Allie Lamp interaction overlay is allowed. */
    public static boolean canUseAllieLamp() {
        return SexAnimationTracker.canUseAllieLamp();
    }

    @OnlyIn(Dist.CLIENT)
    private static void showExitHint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!PlayerKoboldEntity.isAnimating(player)) return;
        player.displayClientMessage(
            Component.literal("Jump to get out of the animation"), true);
    }

    // -- Missing methods used by AllieEntity -----------------------------------

    public static void setAllieActive(boolean active) { canMove = !active; }

    public static void triggerBlackScreen() { canMove = false; }

    /** Returns true if this client is the "leader" controlling the animation pace. */
    public static boolean isLeader() { return true; }

}