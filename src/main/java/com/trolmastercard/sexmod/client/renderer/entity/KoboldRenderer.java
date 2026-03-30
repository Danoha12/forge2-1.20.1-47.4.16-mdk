package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.entity.ClothingOverlayEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.util.LightUtil;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * KoboldRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el renderizado de la entidad Kobold y sus capas de ropa dinámicas.
 * * Sistema de iluminación "Sexmod": Iluminación cinemática direccional.
 */
@OnlyIn(Dist.CLIENT)
public class KoboldRenderer extends GeoEntityRenderer<KoboldEntity> {

  public static final float SCALE_NORMAL = 1.876945F;
  public static final float SCALE_LARGE  = 2.876945F;
  public static boolean renderingOverlay = false;

  private final Map<String, String> lowerBodyRemap = new HashMap<>();
  private final Map<String, String> upperBodyRemap = new HashMap<>();

  private Vector3f currentVertexTint = new Vector3f(1.0F, 1.0F, 1.0F);
  private Vector3f sexmodLightDir = null;

  public KoboldRenderer(EntityRendererProvider.Context ctx, GeoModel<KoboldEntity> model) {
    super(ctx, model);
    initRemapTables();
  }

  private void initRemapTables() {
    lowerBodyRemap.put("customLegL", "legL");
    lowerBodyRemap.put("customLegR", "legR");
    upperBodyRemap.put("top", "upperBody");
    upperBodyRemap.put("customArmL", "armL");
    upperBodyRemap.put("customArmR", "armR");
  }

  @Override
  public void render(KoboldEntity kobold, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    // Guardias de contexto (GUI vs Mundo)
    if (!isValidRenderContext(partialTick)) return;

    // Configurar Iluminación Direccional (Si el bundle lo requiere)
    if (kobold.getLightingMode() == KoboldEntity.LightingMode.SEXMOD) {
      sexmodLightDir = LightUtil.computeSexmodLightDir(kobold, partialTick);
    } else {
      sexmodLightDir = null;
    }

    if (partialTick != SCALE_NORMAL && partialTick != SCALE_LARGE) {
      renderInWorld(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    } else {
      currentVertexTint.set(1.0F, 1.0F, 1.0F);
      super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
  }

  private void renderInWorld(KoboldEntity kobold, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    UUID ownerUUID = kobold.getOwnerUUID();
    if (ownerUUID == null) return;

    BaseNpcEntity owner = BaseNpcEntity.getById(ownerUUID);
    if (owner == null) return;

    // Sincronizar posición del Overlay con el Host (NPC o Jugador)
    LivingEntity host = resolveHost(owner);
    Vec3 hostPos = host.getPosition(partialTick);
    Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    Vec3 offset = hostPos.subtract(camPos);

    poseStack.pushPose();
    poseStack.translate(offset.x, offset.y, offset.z);

    // Aplicar rotación si el NPC tiene un override de orientación
    if (owner.hasOrientationOverride()) {
      poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(owner.getOrientationYaw()));
    }

    super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    poseStack.popPose();
  }

  // ── Lógica de Renderizado Recursivo de Huesos ────────────────────────────

  @Override
  public void renderRecursively(PoseStack poseStack, KoboldEntity animatable, GeoBone bone, RenderType renderType,
                                MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                float partialTick, int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {

    poseStack.pushPose();

    // Aplicar transformaciones del hueso de GeckoLib al PoseStack de Minecraft
    applyBoneTransforms(poseStack, bone);

    if (!bone.isHidden()) {
      for (GeoCube cube : bone.getChildCubes()) {
        renderCubeWithSexmodLighting(cube, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
      }
    }

    for (GeoBone childBone : bone.getChildBones()) {
      renderRecursively(poseStack, animatable, childBone, renderType, bufferSource, buffer, isReRender,
              partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    poseStack.popPose();
  }

  private void renderCubeWithSexmodLighting(GeoCube cube, PoseStack poseStack, VertexConsumer buffer,
                                            int packedLight, int packedOverlay, float r, float g, float b, float a) {
    Matrix4f poseMatrix = poseStack.last().pose();
    Matrix3f normalMatrix = poseStack.last().normal();

    for (GeoQuad quad : cube.quads()) {
      if (quad == null) continue;

      Vector3f normal = new Vector3f(quad.normal().x(), quad.normal().y(), quad.normal().z());
      normalMatrix.transform(normal);

      // Cálculo de Iluminación Cinemática (Sexmod Light)
      float tintFactor = 1.0F;
      if (sexmodLightDir != null) {
        tintFactor = Math.max(0.2F, normal.dot(sexmodLightDir)); // Luz ambiental mínima de 0.2
      }

      for (GeoVertex vertex : quad.vertices()) {
        Vector4f pos = new Vector4f(vertex.position().x(), vertex.position().y(), vertex.position().z(), 1.0F);
        poseMatrix.transform(pos);

        buffer.vertex(pos.x(), pos.y(), pos.z())
                .color(r * tintFactor, g * tintFactor, b * tintFactor, a)
                .uv(vertex.texU(), vertex.texV())
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
      }
    }
  }

  // ── Auxiliares de Sistema ────────────────────────────────────────────────

  private void applyBoneTransforms(PoseStack poseStack, GeoBone bone) {
    poseStack.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
    if (bone.getRotZ() != 0) poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(bone.getRotZ()));
    if (bone.getRotY() != 0) poseStack.mulPose(com.mojang.math.Axis.YP.rotation(bone.getRotY()));
    if (bone.getRotX() != 0) poseStack.mulPose(com.mojang.math.Axis.XP.rotation(bone.getRotX()));
    poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    poseStack.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
  }

  private LivingEntity resolveHost(BaseNpcEntity owner) {
    if (owner instanceof PlayerKoboldEntity pk && pk.getPlayerUUID() != null) {
      Player p = Minecraft.getInstance().level.getPlayerByUUID(pk.getPlayerUUID());
      if (p != null) return p;
    }
    return owner;
  }

  private boolean isValidRenderContext(float partialTick) {
    return partialTick == SCALE_LARGE || partialTick == SCALE_NORMAL || renderingOverlay;
  }

  /**
   * Renderiza todos los overlays de ropa para un NPC específico.
   * Llamado desde el renderer principal del NPC.
   */
  public static void renderOverlaysFor(BaseNpcEntity npc, float partialTick, PoseStack ps, MultiBufferSource buffers) {
    if (npc.isRemoved() || !npc.hasActiveTextures()) return;

    renderingOverlay = true;
    for (String texture : npc.getActiveTextures()) {
      ClothingOverlayEntity overlay = new ClothingOverlayEntity(npc.level(), npc.getUUID(), texture);
      Minecraft.getInstance().getEntityRenderDispatcher().render(overlay, 0, 0, 0, 0, partialTick, ps, buffers, 0xF000F0);
    }
    renderingOverlay = false;
  }
}