package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete correcto

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * PlayerKoboldRenderHandler — Portado a 1.20.1.
 * * Intercepta el renderizado del jugador y dibuja un Kobold en su lugar.
 * * Utiliza ThreadLocal como candado anti-recursión para no arruinar los FPS ni GeckoLib.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerKoboldRenderHandler {

    // 🛡️ ESCUDO ANTI-RECURSIÓN: Reemplaza el "sentinel partial tick".
    // Esto asegura que no haya ciclos infinitos, pero permite pasar el partial tick real.
    private static final ThreadLocal<Boolean> IS_RENDERING = ThreadLocal.withInitial(() -> false);

    Vec3 savedCamPos = null;
    Vec3 savedCamPrev = null;
    PlayerKoboldEntity activeKobold = null;
    boolean active = false;

    // ── Intercepción del Renderizado ─────────────────────────────────────────

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // Si ya estamos renderizando el reemplazo, ignoramos para evitar ciclos infinitos
        if (IS_RENDERING.get()) return;

        PlayerKoboldEntity.cleanup();
        Player player = event.getEntity();
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player.getUUID());

        if (kobold == null) return;

        // Cancelamos el renderizado normal del jugador
        event.setCanceled(true);

        // Copiamos la posición, rotación y estado del jugador al Kobold
        kobold.mirrorPlayer(player);

        Minecraft mc = Minecraft.getInstance();

        // 1.20.1: Usamos UUID en lugar del viejo GameProfile para comparar
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(player.getUUID());

        // Si es el jugador local en primera persona y no debe renderizarse, salimos
        if (isLocalPlayer && !kobold.isRenderable()) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        // Bloqueamos el candado antes de renderizar
        IS_RENDERING.set(true);
        try {
            // Pasamos 0,0,0 porque el PoseStack ya está en las coordenadas del jugador
            dispatcher.render(
                    kobold,
                    0.0D, 0.0D, 0.0D,
                    player.getYRot(), // Yaw del jugador
                    event.getPartialTick(), // ¡PartialTick REAL para animaciones fluidas!
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight() // Luz ya calculada
            );
        } finally {
            // Liberamos el candado
            IS_RENDERING.set(false);
        }
    }
}