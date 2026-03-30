package com.trolmastercard.sexmod.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.client.model.StaffModel;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.item.StaffItem;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StaffItemRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderiza el orbe de cristal en la punta del bastón y los iconos de la tribu orbitando.
 * Incluye física de inclinación basada en el movimiento del jugador.
 */
@OnlyIn(Dist.CLIENT)
public class StaffItemRenderer extends GeoItemRenderer<StaffItem> {

    private static final ResourceLocation ENDER_CRYSTAL_TEX = new ResourceLocation("textures/entity/endercrystal/endercrystal.png");

    // Constantes de renderizado
    static final float TIP_Y_OFFSET = 1.5F;
    static final float ORB_BASE_SCALE = 0.175F;
    static final float SCALE_VARIATION = 0.1F;
    static final float TILT_FACTOR = 0.04F;
    static final float ORBIT_RADIUS = 10.0F;

    private static boolean scatterMode = false;
    private final Map<ItemStack, Vector3f> itemPositions = new HashMap<>();
    private ModelPart crystalGlass;
    private ModelPart crystalCube;

    public StaffItemRenderer() {
        super(new StaffModel());
    }

    public static void toggleScatter() { scatterMode = !scatterMode; }

    // ── Preparación del Renderizado ───────────────────────────────────────────

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Inicializar partes del modelo del cristal si no existen
        if (this.crystalGlass == null) {
            ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL);
            this.crystalGlass = root.getChild("glass");
            this.crystalCube = root.getChild("cube");
        }
        super.renderByItem(stack, ctx, ps, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public void renderRecursively(PoseStack ps, StaffItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        if (bone.getName().equals("staff_tip")) { // Asegúrate que el hueso se llame así en Blockbench
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            float gameTime = player.tickCount + partialTick;

            ps.pushPose();

            // Aplicar transformación del hueso
            RenderUtils.prepMatrixForBone(ps, bone);

            // Efecto de flotación (Bobbing)
            double bob = TIP_Y_OFFSET + Math.sin(gameTime * 0.05D) * 0.02D;
            ps.translate(0, bob, 0);

            // Escala pulsante del orbe
            float scale = ORB_BASE_SCALE + (float)Math.sin(gameTime * 0.1D) * 0.02F;
            ps.scale(scale, scale, scale);

            // Física de inclinación (Tilt)
            double dx = player.getX() - player.xOld;
            double dz = player.getZ() - player.zOld;
            ps.mulPose(Axis.XP.rotationDegrees((float)dz * 500F));
            ps.mulPose(Axis.ZP.rotationDegrees((float)-dx * 500F));

            // Rotación constante del orbe
            ps.mulPose(Axis.YP.rotationDegrees(gameTime * 2.0F));
            ps.mulPose(Axis.XP.rotationDegrees(gameTime * 1.5F));

            // Dibujar el Orbe de Cristal de Ender
            VertexConsumer crystalBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(ENDER_CRYSTAL_TEX));
            this.crystalCube.render(ps, crystalBuffer, packedLight, packedOverlay);
            this.crystalGlass.render(ps, crystalBuffer, packedLight, packedOverlay);

            ps.popPose();

            // Renderizar objetivos de la tribu orbitando
            renderTribeTargets(ps, bufferSource, player, gameTime);
        }

        super.renderRecursively(ps, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── Renderizado de la Tribu Orbitando ─────────────────────────────────────

    private void renderTribeTargets(PoseStack ps, MultiBufferSource buffers, Player player, float time) {
        List<Vec3> targets = KoboldEntity.staffTargetPositions;
        if (targets == null || targets.isEmpty()) return;

        float step = 360f / targets.size();
        for (int i = 0; i < targets.size(); i++) {
            ps.pushPose();

            // Posicionamiento en el anillo orbital
            float angle = (time * 2.0F) + (i * step);
            double ox = Math.cos(Math.toRadians(angle)) * 0.5D;
            double oz = Math.sin(Math.toRadians(angle)) * 0.5D;

            ps.translate(ox, TIP_Y_OFFSET + 0.2D, oz);
            ps.scale(0.2F, 0.2F, 0.2F);
            ps.mulPose(Axis.YP.rotationDegrees(-angle + 90));

            // Renderizar un bloque representativo (SFW: Usamos una gema o piedra)
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    new ItemStack(Items.EMERALD),
                    ItemDisplayContext.FIXED,
                    15728880, OverlayTexture.NO_OVERLAY,
                    ps, buffers, player.level(), 0
            );

            ps.popPose();
        }
    }
}