package com.trolmastercard.sexmod.client.handler;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.client.SexAnimationTracker; // Asumiendo esta ruta
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ClientStateManager — Portado a 1.20.1.
 * * Intercepta los controles (teclado/ratón) durante las animaciones para congelar al jugador.
 * * Permite usar Saltar (Jump) o Agacharse (Sneak) para cancelar o avanzar animaciones.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientStateManager {

    private static boolean canMove = true;

    public static boolean jumpPressed = false;
    public static boolean sneakPressed = false;

    // ── Intercepción de Controles (Event Subscriber) ─────────────────────────

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        var input = event.getInput();
        LocalPlayer player = Minecraft.getInstance().player;

        // Cacheamos el input actual para otras clases que lo necesiten
        jumpPressed = input.jumping;
        sneakPressed = input.shiftKeyDown;

        // Si el jugador es libre de moverse, no hacemos nada
        if (canMove) return;

        // ── Lógica de Salida / Avance de Animación ──
        if (input.shiftKeyDown) {
            PlayerKoboldEntity.exitAnimationForAll();
        }

        if (input.jumping && player != null) {
            BaseNpcEntity.cancelAnimation(player.getUUID());
        }

        if (input.shiftKeyDown && SexAnimationTracker.getProgress() >= 1.0 && player != null) {
            BaseNpcEntity.advanceAnimation(player.getUUID());
        }

        // ── Congelamiento del Jugador ──
        // Anulamos todos los inputs direccionales y de salto/agacharse
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;

        // Frenamos cualquier inercia física que tuviera el jugador
        if (player != null) {
            player.setDeltaMovement(0, 0, 0);
        }
    }

    // ── API Pública ──────────────────────────────────────────────────────────

    public static boolean canMove() {
        return canMove;
    }

    public static void setCanMove(boolean value) {
        canMove = value;
        if (!value) {
            showExitHint();
        }
    }

    /** Devuelve true si la transición de "pantalla negra" está activa. */
    public static boolean isBlackScreenActive() {
        return !canMove && !SexAnimationTracker.canUseAllieLamp();
    }

    /** Devuelve true si el overlay de interacción de la Lámpara de Allie está permitido. */
    public static boolean canUseAllieLamp() {
        return SexAnimationTracker.canUseAllieLamp();
    }

    @OnlyIn(Dist.CLIENT)
    private static void showExitHint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Asumiendo que isAnimating() comprueba si el jugador está transformado/animado
        if (!PlayerKoboldEntity.isAnimating(player)) return;

        // true en displayClientMessage manda el mensaje a la ActionBar (encima de la hotbar)
        player.displayClientMessage(Component.literal("Presiona SALTAR (Jump) para salir de la animación"), true);
    }
}