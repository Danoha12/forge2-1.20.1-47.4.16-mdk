package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.handler.KoboldShoulderRenderHandler;
import com.trolmastercard.sexmod.client.model.entity.GoblinModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GoblinEntity;
import com.trolmastercard.sexmod.entity.NpcModelCodeEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.util.EyeColor;
import com.trolmastercard.sexmod.util.GoblinColor;
import com.trolmastercard.sexmod.util.HairColor;
import com.trolmastercard.sexmod.util.NpcColorData;
import com.trolmastercard.sexmod.util.NpcSkinTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

/**
 * GoblinEntityRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador principal del Goblin.
 * * Aplica los colores genéticos (Model Code) a la piel, pelo y ojos.
 * * Sincroniza la rotación con el jugador durante escenas de interacción.
 */
@OnlyIn(Dist.CLIENT)
public class GoblinEntityRenderer extends GoblinBodyRenderer {

    public static final float SENTINEL = -420.69F;

    private static final HashSet<String> EYELASH_BONES = new HashSet<>(
            Arrays.asList("lashR", "lashL", "closedR", "closedL", "browL", "browR"));

    private float partialTick = 0.0F;

    public GoblinEntityRenderer(EntityRendererProvider.Context ctx) {
        // Inicializamos con el modelo específico del Goblin
        super(ctx, new GoblinModel());
    }

    // ── Resolución de Textura Base ───────────────────────────────────────────

    @Nullable
    @Override
    protected NpcSkinTexture getSkinTexture(GoblinEntity entity) {
        try {
            if (entity.level() instanceof FakeWorld) return null;
            if (entity.getCarrierUUID() != null) return null;
        } catch (RuntimeException ignored) {}

        UUID skinUUID = entity.getOwnerUUID();

        // Si no tiene dueño, intentamos usar el del jugador local (fallback)
        if (skinUUID == null && Minecraft.getInstance().player != null) {
            skinUUID = Minecraft.getInstance().player.getGameProfile().getId();
        }

        if (skinUUID == null) return NpcColorData.DEFAULT_TEXTURE;

        NpcSkinTexture cached = NpcSkinTexture.getCache().get(skinUUID);
        if (cached != null) return cached;

        return NpcColorData.loadSkinTexture(skinUUID, entity.level());
    }

    // ── Lógica Principal de Renderizado ──────────────────────────────────────

    @Override
    public void render(GoblinEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {

        this.partialTick = partialTick;
        AnimState state = entity.getAnimState();

        // 1. Guardias de recursión para el Shoulder-Ride
        if (entityYaw == SENTINEL) {
            if (state == AnimState.SHOULDER_IDLE || state == AnimState.PICK_UP) {
                return; // El renderizado ya está siendo manejado por el handler del hombro
            }
        }

        Minecraft mc = Minecraft.getInstance();

        // 2. Alineación de rotación durante escenas (Sex Mode)
        if (entity.isSexModeActive()) {
            UUID partnerId = entity.getPartnerUUID();
            if (partnerId != null) {
                Player partner = entity.level().getPlayerByUUID(partnerId);
                if (partner != null) {
                    // Forzar al Goblin a mirar en la misma dirección que el jugador
                    entity.yHeadRot = partner.getYRot();
                    entity.yBodyRot = partner.getYRot();
                    entityYaw = partner.getYRot(); // Actualizar el Yaw de renderizado
                }
            }
        }

        // 3. Renderizado Base (Ocultamientos de cámara primera persona manejados en super)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // 4. Aplicar shader de contorno (Si está seleccionado/mirado)
        NpcColorData.applyOutlineTinting(entity, partialTick);
    }

    // ── Aplicación de Colores Genéticos (Model Code) ─────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, GoblinEntity entity, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        // Evitar renderizar huesos físicos si está cargado
        if (entity.getCarrierUUID() != null && PHYSICS_ONLY_BONES.contains(boneName)) return;

        // Visibilidad de la Corona (Solo Reinas)
        if (boneName.contains("crown")) {
            if (!shouldRenderCrown(entity)) return;
        }

        // Calcular y aplicar el color genético basado en el Model Code
        Vec3 geneticColor = computeBoneColor(entity, boneName);
        if (geneticColor != null) {
            red = (float) geneticColor.x / 255.0F;
            green = (float) geneticColor.y / 255.0F;
            blue = (float) geneticColor.z / 255.0F;
        }

        super.renderRecursively(poseStack, entity, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── Helpers de Decodificación del "Model Code" ───────────────────────────

    @Nullable
    private Vec3 computeBoneColor(GoblinEntity entity, String boneName) {
        String[] dna = NpcModelCodeEntity.getModelCodeSegments(entity);
        if (dna == null || dna.length < 8) return null;

        // Bandas o accesorios blancos por defecto
        if (boneName.contains("band")) return new Vec3(255, 255, 255);

        // Ojos: Segmento [8]
        if (boneName.contains("eyeColor") || boneName.contains("eyeColor2")) {
            return getSafeColor(EyeColor.values(), dna[8]);
        }

        // Pelo y Pestañas: Segmento [6] (Original dice 6 para pelo, 7 para cuerpo)
        if (boneName.contains("hair") || EYELASH_BONES.contains(boneName)) {
            return getSafeColor(HairColor.values(), dna[6]);
        }

        // Piel y Cuerpo: Segmento [7]
        if (boneName.contains("variant") || boneName.contains("boob") || TINTABLE_BONES.contains(boneName)) {
            return getSafeColor(GoblinColor.values(), dna[7]);
        }

        return null;
    }

    private boolean shouldRenderCrown(GoblinEntity entity) {
        String[] dna = NpcModelCodeEntity.getModelCodeSegments(entity);
        if (dna == null || dna.length < 10) return false;
        try {
            return Integer.parseInt(dna[9]) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Helper genérico para extraer colores de tus Enums de forma segura
    private Vec3 getSafeColor(Enum<?>[] values, String dnaSegment) {
        try {
            int index = Integer.parseInt(dnaSegment);
            if (index >= 0 && index < values.length) {
                // Asumimos que todos tus enums de color tienen el método getColorVec()
                // Usamos reflexión ligera o un cast si comparten interfaz. Aquí asumo
                // que usas un método común o lo casteamos según el tipo:
                if (values instanceof EyeColor[] ec) return ec[index].getColorVec();
                if (values instanceof HairColor[] hc) return hc[index].getColorVec();
                if (values instanceof GoblinColor[] gc) return gc[index].getColorVec();
            }
        } catch (Exception ignored) {}
        return new Vec3(255, 255, 255); // Fallback a blanco
    }

    // ── Helpers Estáticos de Renderizado ─────────────────────────────────────

    public static void preloadAll() {
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (npc instanceof GoblinEntity goblin && goblin.level().isClientSide) {
                goblin.getAnimatableInstanceCache().getManagerForId(goblin.getId());
            }
        }
    }

    public static void renderForPlayer(GoblinEntity entity, Player player, double dx, double dy, double dz, float pt) {
        // Usamos el handler que ya portamos previamente
        KoboldShoulderRenderHandler.renderAsPlayer((PlayerKoboldEntity) (Object) entity, player,
                new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
                15728880, pt);
    }
}