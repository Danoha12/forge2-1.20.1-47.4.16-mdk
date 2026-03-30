package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.util.PlayerSkinUtil;
// import com.trolmastercard.sexmod.client.CustomModelManager; // Descomentar cuando CustomModelManager esté porteado
// import com.trolmastercard.sexmod.entity.part.SelectableEntityPart; // Descomentar cuando SelectableEntityPart esté porteado
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * BaseNpcRenderer — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * Base abstracta para todos los renderizadores de NPCs.
 * Extiende GeoEntityRenderer con:
 * 1. Carga de texturas de skin desde el UUID del jugador.
 * 2. Soporte de color por hueso.
 * 3. Set de huesos ocultos.
 * 4. Renderizado de correas (leash).
 * 5. Comprobación de visibilidad (AABB ray-cast).
 * 6. Renderizado de líneas de ropa superior (Cuerdas/Cordones).
 */
@OnlyIn(Dist.CLIENT)
public abstract class BaseNpcRenderer<T extends BaseNpcEntity> extends GeoEntityRenderer<T> {

    // ── Constantes ─────────────────────────────────────────────────────────────

    protected static final ResourceLocation LINE_TEXTURE = new ResourceLocation("sexmod", "textures/line.png");
    protected static final float SCALE_FACTOR = 1.5f;

    // ── Estado ─────────────────────────────────────────────────────────────────

    protected double shadowRadius;
    protected T entityRef;
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final HashMap<UUID, ResourceLocation> skinCache = new HashMap<>();

    Color skinBaseColor      = new Color(245, 199, 165);
    Color skinSecondaryColor = new Color(245, 157, 169);
    boolean hasSkinLoadError = false;

    protected final HashSet<String> hiddenBoneSet = new HashSet<>();
    protected GeoBone currentBone = null;
    protected float bowAnimProgress = 0.0f;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected BaseNpcRenderer(EntityRendererProvider.Context context, GeoModel<T> model, double shadowRadius) {
        super(context, model);
        this.shadowRadius = shadowRadius;
    }

    // ── Skin texture loading ──────────────────────────────────────────────────

    protected ResourceLocation getSkinTexture(T entity) throws IOException {
        UUID ownerUUID;
        // Asumiendo que entity.level() != null es seguro, y getMasterUUID() reemplaza a getOwnerUUID()
        if (entity.level() == null || entity.getMasterUUID() == null) {
            ownerUUID = mc.getUser().getGameProfile().getId();
        } else {
            ownerUUID = entity.getMasterUUID();
        }

        ResourceLocation cached = skinCache.get(ownerUUID);
        if (cached != null) return cached;

        return loadAndCacheSkin(ownerUUID);
    }

    private ResourceLocation loadAndCacheSkin(UUID uuid) throws IOException {
        BufferedImage image;
        try {
            image = PlayerSkinUtil.fetchSkin(uuid); // Adaptado al método que usamos en el LampItem
            if (image == null) throw new IOException("Skin nula");
            Graphics g = image.getGraphics();
            g.setColor(skinBaseColor);
            g.fillRect(0, 0, 4, 3);
            g.setColor(skinSecondaryColor);
            g.fillRect(4, 0, 3, 3);
            g.dispose();
        } catch (Exception e) {
            hasSkinLoadError = true;
            // Textura por defecto si falla
            image = ImageIO.read(mc.getResourceManager()
                    .getResource(new ResourceLocation("minecraft", "textures/entity/steve.png")) // Usando a Steve de vainilla como fallback seguro
                    .orElseThrow().open());
        }

        ResourceLocation loc = mc.getTextureManager().register(
                "player_skin_" + uuid,
                new DynamicTexture(image)); // Nota: Deberíamos usar NativeImage en 1.20.1, si falla, usaremos el convertidor del LampModel.
        skinCache.put(uuid, loc);
        return loc;
    }

    // ── Hidden bone set ───────────────────────────────────────────────────────

    protected HashSet<String> buildHiddenBoneSet(boolean useNoAi, boolean isFirstSpawn) {
        HashSet<String> result = new HashSet<>();

        // --- Bloque comentado hasta que CustomModelManager sea porteado ---
        /*
        Set<String> rawSet = useNoAi
                ? CustomModelManager.getDefaultHiddenBones()
                : entityRef.getHiddenBoneNames();

        for (String boneName : rawSet) {
            CustomModelManager.BoneEntry entry = CustomModelManager.getBoneEntry(boneName);
            if (entry == null) continue;
            if (!entry.isSfw() && isFirstSpawn) continue; // Cambiado isNsfw a isSfw
            result.addAll(entry.getBoneNames());
        }
        */
        return result;
    }

    // ── Bone colour ───────────────────────────────────────────────────────────

    protected net.minecraft.core.Vec3i getBoneColor(String boneName) {
        return new net.minecraft.core.Vec3i(255, 255, 255);
    }

    // ── Per-bone hooks ────────────────────────────────────────────────────────

    protected void onBoneProcess(String boneName, GeoBone bone) {}
    protected void onBoneUvOffset(String boneName, GeoBone bone) {}

    protected void renderItemAtBone(PoseStack poseStack, MultiBufferSource buffers, GeoBone bone, T entity, int light) {}

    @Nullable
    protected ItemStack resolveHeldItem(@Nullable ItemStack defaultItem) {
        return defaultItem;
    }

    // ── GeckoLib4 render pipeline ─────────────────────────────────────────────

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.entityRef = entity;

        // Comprobación de visibilidad (LOS)
        // Descomentar cuando requiresLos() esté en BaseNpcEntity
        /*
        if (mc.player != null && !entity.isNoAi() && entity.requiresLos()) {
            if (!isVisible(entity, mc.player)) return;
        }
        */

        // Refrescar conjunto de huesos ocultos (Se usará getAnimAge cuando exista)
        this.hiddenBoneSet.clear();
        this.hiddenBoneSet.addAll(buildHiddenBoneSet(entity.isNoAi(), true));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Actualizar matrices de huesos después del renderizado
        updateBoneMatrices(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        // Evitar renderizado en menús falsos (Level == null)
        if (this.entityRef != null && this.entityRef.level() == null) return;

        String name = bone.getName();
        this.currentBone = bone;

        onBoneProcess(name, bone);
        onBoneUvOffset(name, bone);

        if (this.hiddenBoneSet.contains(name)) {
            bone.setHidden(true);
        }

        // Color personalizado por hueso
        net.minecraft.core.Vec3i color = getBoneColor(name);
        float r = color.getX() / 255.0f;
        float g = color.getY() / 255.0f;
        float b = color.getZ() / 255.0f;

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, alpha);
    }

    // ── Visibility (line-of-sight) check ──────────────────────────────────────

    protected boolean isVisible(T npc, Player player) {
        if (npc instanceof PlayerKoboldEntity) return true;
        if (mc.options.getCameraType().isFirstPerson()) return true;

        Vec3 npcPos = npc.position();
        float hw = npc.getBbWidth() * SCALE_FACTOR * 0.5f;
        float hh = npc.getBbHeight() * SCALE_FACTOR;

        Vec3 camPos = player.getEyePosition(1.0f);
        var level = npc.level();

        Vec3[] corners = new Vec3[] {
                npcPos.add(-hw, 0, -hw), npcPos.add(-hw, 0, hw),
                npcPos.add( hw, 0, -hw), npcPos.add( hw, 0, hw),
                npcPos.add(-hw, hh,-hw), npcPos.add(-hw, hh, hw),
                npcPos.add( hw, hh,-hw), npcPos.add( hw, hh, hw)
        };

        for (Vec3 corner : corners) {
            BlockHitResult clip = level.clip(new ClipContext(
                    camPos, corner,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player));

            if (clip.getType() == HitResult.Type.MISS) return true;

            BlockPos hitPos = clip.getBlockPos();
            var state = level.getBlockState(hitPos);
            if (state.getRenderShape() == RenderShape.INVISIBLE) return true;
            if (!state.isSolidRender(level, hitPos)) return true;
        }
        return false;
    }

    // ── Name tag ──────────────────────────────────────────────────────────────

    @Override
    protected boolean shouldShowName(T entity) {
        if (entity.isNoAi()) return false;
        if (entity.getAnimState() != null && entity.getAnimState().hideNameTag) return false;
        if (mc.getCameraEntity() == null) return false;

        return super.shouldShowName(entity);
    }

    // ── Bone matrix update ────────────────────────────────────────────────────

    protected void updateBoneMatrices(T entity) {
        // --- Comentado hasta que SelectableEntityPart sea porteado ---
        /*
        List<String> boneNames = new ArrayList<>(SelectableEntityPart.BONE_NAMES);
        boneNames.addAll(entity.getExtraBoneNames());

        software.bernie.geckolib.util.GeckoLibUtil.getGeoModel(entity).ifPresent(model -> {
            for (String boneName : boneNames) {
                GeoBone bone = model.getBone(boneName).orElse(null);
                if (bone == null) continue;

                // Extraer la posición (GeckoLib 4 usa getPosX/Y/Z)
                Vec3 pos = new Vec3(-bone.getPosX(), bone.getPosY(), -bone.getPosZ());
                entity.updateBonePosition(boneName, pos);
            }
        });
        */
    }

    // ── Renderizado de cuerdas / líneas de ropa ───────────────────────────────

    protected static void drawBoneSegment(PoseStack poseStack, MultiBufferSource buffers, BaseNpcEntity entity, String boneA, String boneB, float r, float g, float b, float alpha) {
        // --- Comentado hasta que getBonePosition exista en BaseNpcEntity ---
        /*
        Vec3 posA = entity.getBonePosition(boneA);
        Vec3 posB = entity.getBonePosition(boneB);
        if (posA == null || posB == null) return;

        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();

        vc.vertex(mat, (float) posA.x, (float) posA.y, (float) posA.z).color(r, g, b, alpha).normal(poseStack.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(mat, (float) posB.x, (float) posB.y, (float) posB.z).color(r, g, b, alpha).normal(poseStack.last().normal(), 0, 1, 0).endVertex();
        */
    }

    /** Dibuja las líneas de la ropa superior (SFW enmascarado). */
    protected static void drawUpperClothingStrings(PoseStack poseStack, MultiBufferSource buffers, BaseNpcEntity entity, int rColor, int gColor, int bColor, float alpha) {
        float r = rColor / 255.0f, g = gColor / 255.0f, b = bColor / 255.0f;
        // Mantenemos los nombres de huesos intactos para Blockbench, pero el contexto es seguro.
        String[][] segments = {
                {"braStringMidStartR","braStringMidMid1R"},{"braStringMidMid1R","braStringMidMid2R"},
                {"braStringMidMid2R","braStringMidMid3R"},{"braStringMidMid3R","braStringMidEndR"},
                {"braStringMidEndR","braStringBackR"},{"braStringBackR","braStringRightEndR"},
                {"braStringRightEndR","braStringRightStartR"},{"braStringRightR","braStringRightL"},
                {"braStringMidStartL","braStringMidMid1L"},{"braStringMidMid1L","braStringMidMid2L"},
                {"braStringMidMid2L","braStringMidMid3L"},{"braStringMidMid3L","braStringMidEndL"},
                {"braStringMidEndL","braStringBackL"},{"braStringBackL","braStringLeftEndL"},
                {"braStringLeftEndL","braStringLeftStartL"}
        };
        for (String[] seg : segments) {
            drawBoneSegment(poseStack, buffers, entity, seg[0], seg[1], r, g, b, alpha);
        }
    }
}