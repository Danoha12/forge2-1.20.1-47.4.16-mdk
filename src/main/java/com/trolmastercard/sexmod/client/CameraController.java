package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * CameraController — Portado a 1.20.1.
 * * Maneja las matemáticas de inercia y seguimiento de cámara (Boy-Cam).
 */
@OnlyIn(Dist.CLIENT)
public class CameraController {

    // Singleton Instance
    private static final CameraController INSTANCE = new CameraController();

    // ── Variables de Inercia y Rotación (Yaw) ──
    public float yawVelocity = 0.0F;
    public float prevYaw = 0.0F;
    public float smoothYawVelocity = 0.0F;

    // ── Variables de Inercia y Rotación (Pitch) ──
    public float pitchVelocity = 0.0F;
    public float prevPitch = 0.0F;
    public float smoothPitchVelocity = 0.0F;

    // ── Vector de movimiento del ratón ──
    public Vec3 mouseDelta = Vec3.ZERO;

    private CameraController() {
        // Constructor privado para el Singleton
    }

    public static CameraController getInstance() {
        return INSTANCE;
    }

    /**
     * Se llama cada frame para alinear la cámara del jugador con el hueso de la cabeza del NPC.
     */
    public void update(BaseNpcEntity npc, float partialTick) {
        if (npc == null || npc.level() == null) return;

        // Aquí irá la lógica pesada de extraer la posición del hueso "head" o "camera" de GeckoLib 4
        // y forzar la rotación del jugador (mc.player.setYRot / setXRot)
        // Por ahora lo dejamos vacío para que el compilador te deje en paz y pases al siguiente error.
    }
}