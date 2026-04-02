package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.client.renderer.entity.NpcArmRenderer;
import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.Collection;

/**
 * AllieBodyRenderer — Portado a 1.20.1 y enmascarado (SFW).
 * Renderizador de brazos y cuerpo para NPCs tipo Allie.
 * Añade físicas de balanceo (cola y cuerpo) basadas en el movimiento del jugador al que sigue.
 */
@OnlyIn(Dist.CLIENT)
public class AllieBodyRenderer<T extends AllieEntity> extends NpcArmRenderer<T> {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final float SWAY_SCALE = 8.0f;
    private static final float MAX_SWAY   = 1.68f;
    private static final float BOB_SCALE  = 5.0f;

    // ── Shared instance registry ──────────────────────────────────────────────

    static final Collection<AllieBodyRenderer<?>> ALL_INSTANCES = new ArrayList<>();

    // ── Physics state ─────────────────────────────────────────────────────────

    private double curX  = 0, prevX = 0;
    private double curZ  = 0, prevZ = 0;
    private float  swayX = 0, prevSwayX = 0;
    private float  swayZ = 0, prevSwayZ = 0;
    private double bodyBob = 0, prevBob = 0;

    /** Partial ticks almacenados por la llamada de renderizado. */
    private float partialTick = 0.0f;

    public AllieBodyRenderer(GeoModel<T> model) {
        super(model);
        ALL_INSTANCES.add(this);
    }

    // ── World-space transform ─────────────────────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.1, 0);
        poseStack.scale(0.7f, 0.7f, 0.7f);
    }

    // ── Item transforms ───────────────────────────────────────────────────────

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        UseAnim anim = stack.getItem().getUseAnimation(stack);
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        if (!isRightHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0f));
        }
        poseStack.translate(0, 0.05, 0);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyEmptyHandTransform(poseStack, isRightHand);
        poseStack.translate(isRightHand ? 0.15 : -0.05, 0, 0);
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0f));
        } else if (!isRightHand && !isOffHand) {
            poseStack.translate(-0.05, -0.125, 0.125);
            poseStack.mulPose(Axis.XP.rotationDegrees(50.0f));
        }
    }

    // ── Physics per-bone (GeckoLib 4 update) ─────────────────────────────────

    @Override
    protected void onBoneProcess(String name, GeoBone bone, float partialTick) {
        this.partialTick = partialTick; // Guardar partial tick para los cálculos
        if (this.entityRef == null || this.entityRef.isNoAi()) return; // isNoAi() reemplaza a isFrozen()

        if ("tail".equals(name)) {
            applySwayToBone(bone, 0.0f, 0.0f, 1.0f);
        } else if ("body".equals(name)) {
            applyBodyBobToBone(bone);
        } else if ("armL".equals(name) && this.entityRef.getAnimState() != AnimState.BOW) {
            applySwayToBone(bone, 0.0f, -0.34906584f, 0.15f);
        } else if ("armR".equals(name) && this.entityRef.getAnimState() != AnimState.BOW
                && this.entityRef.getAnimState() != AnimState.ATTACK) {
            applySwayToBone(bone, 0.0f, 0.34906584f, 0.15f);
        }
    }

    /**
     * Calcula los ángulos de balanceo desde el delta de movimiento del "master" y los aplica al hueso.
     */
    private void applySwayToBone(GeoBone bone, float baseRotX, float baseRotZ, float influence) {
        double dx = this.curX - this.prevX;
        double dz = this.curZ - this.prevZ;
        double yawRad = Math.toRadians(this.entityRef.getYRot());

        // Rotar delta al espacio local de la entidad
        Vec2 local = new Vec2(
                (float)(dx * Math.cos(yawRad) + dz * Math.sin(yawRad)),
                (float)(-dx * Math.sin(yawRad) + dz * Math.cos(yawRad))
        );

        float rawX = local.x * -SWAY_SCALE;
        float rawZ = local.y * SWAY_SCALE;
        rawX = Mth.clamp(rawX, -MAX_SWAY, MAX_SWAY);
        rawZ = Mth.clamp(rawZ, -MAX_SWAY, MAX_SWAY);

        // Suavizado (Lerp)
        this.swayX = Mth.lerp(this.partialTick, this.prevSwayX, rawX);
        this.swayZ = Mth.lerp(this.partialTick, this.prevSwayZ, rawZ);

        // GeckoLib 4 usa updateRotation()
        bone.updateRotation(baseRotX + this.swayX * influence, bone.getRotY(), baseRotZ + this.swayZ * influence);
    }

    /**
     * Aplica el desplazamiento vertical (bob) al cuerpo.
     */
    private void applyBodyBobToBone(GeoBone bone) {
        double dx = this.curX - this.prevX;
        double dz = this.curZ - this.prevZ;
        double bobTarget = Math.min(1.0, (Math.abs(dx) + Math.abs(dz)) * BOB_SCALE);

        float smoothedBob = Mth.lerp(this.partialTick, (float)this.prevBob, (float)bobTarget);

        // GeckoLib 4 usa updatePosition()
        bone.updatePosition(bone.getPosX(), Mth.lerp(smoothedBob, 5.0f, 0.0f), bone.getPosZ());

        this.entityRef.bobScale = Mth.lerp(smoothedBob, 0.3f, 0.0f);
    }

    // ── Tick update (owner position tracking) ────────────────────────────────

    void tickPhysics() {
        if (this.entityRef == null) return;

        // Guardar estado previo
        this.prevSwayX = this.swayX;
        this.prevSwayZ = this.swayZ;
        this.prevBob   = this.bodyBob;

        // Seguir al Master Player
        if (this.entityRef.getMasterUUID() == null) return;
        var player = this.entityRef.level().getPlayerByUUID(this.entityRef.getMasterUUID());
        if (player == null) return;

        this.prevX = this.curX;
        this.prevZ = this.curZ;
        this.curX  = player.getX();
        this.curZ  = player.getZ();
    }

    // ── Static tick event subscriber ─────────────────────────────────────────

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class TickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                for (AllieBodyRenderer<?> renderer : AllieBodyRenderer.ALL_INSTANCES) {
                    renderer.tickPhysics();
                }
            }
        }
    }
}