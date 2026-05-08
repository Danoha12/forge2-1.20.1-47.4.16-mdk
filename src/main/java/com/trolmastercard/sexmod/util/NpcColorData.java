package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.util.NpcSkinTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;

/** NpcColorData - stub for NPC color/tinting data. */
public class NpcColorData {

    public static final NpcSkinTexture DEFAULT_TEXTURE = new NpcSkinTexture(
            new ResourceLocation("sexmod", "textures/entity/default.png")
    );

    /** Returns the set of bone names that should be hidden during rendering. */
    public static HashSet<String> getHiddenBones() {
        return new HashSet<>();
    }

    /** Applies outline tinting for the given entity (no-op stub). */
    public static void applyOutlineTinting(Object entity, float partialTick) {
        // stub - outline shader tinting
    }

    /** Applies tinting for the given entity (no-op stub). */
    public static void applyTinting(Object entity, float partialTick) {
        // stub
    }
// ── Lógica de Descarga de Skins ──────────────────────────────────────────

    /**
     * Carga una nueva textura de skin para el NPC y la guarda en el caché.
     */
    // ── Lógica de Descarga de Skins ──────────────────────────────────────────

    /**
     * Carga una nueva textura de skin para el NPC y la guarda en el caché.
     */
    public static NpcSkinTexture loadSkinTexture(java.util.UUID skinUUID, net.minecraft.world.level.Level level) {

        // 1. Creamos un perfil de jugador temporal solo con el UUID
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(skinUUID, null);

        // 2. Le pedimos a Minecraft 1.20.1 que nos dé la textura (ResourceLocation) de ese perfil
        net.minecraft.resources.ResourceLocation skinLoc = net.minecraft.client.Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile);

        // 3. 🚨 REPARADO: Ahora sí, le entregamos el ResourceLocation a tu constructor
        NpcSkinTexture newTexture = new NpcSkinTexture(skinLoc);

        // 4. Lo guardamos en el archivero (Caché) para no tener que repetir esto
        NpcSkinTexture.getCache().put(skinUUID, newTexture);

        return newTexture;
    }
}