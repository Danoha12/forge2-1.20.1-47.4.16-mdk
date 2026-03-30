package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete de renderizadores

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * TailPhysicsNpcRenderer — Portado a 1.20.1.
 * * Añade físicas inerciales a la cola, cuerpo y brazos del NPC.
 * * 🚨 Optimizado para no usar listas estáticas ni causar Memory Leaks.
 */
public class TailPhysicsNpcRenderer extends NpcHandRenderer {

    // ── Constantes Físicas ───────────────────────────────────────────────────
    private static final float MAX_SWAY = 8.0F;
    private static final float MAX_LEAN = 1.68F; // radianes
    private static final float BODY_SCALE = 5.0F;

    // Variables internas para suavizado visual (Lerp) de este renderizador específico
    private float tailRotX = 0.0F;
    private float tailRotZ = 0.0F;
    private float bodyBob = 0.0F;

    public TailPhysicsNpcRenderer(EntityRendererProvider.Context context,
                                  GeoModel<BaseNpcEntity> model,
                                  double shadowRadius) {
        super(context, model, shadowRadius);
        // Eliminada la lista estática ALL para evitar fugas de memoria masivas
    }

    // ── Transformaciones Base ────────────────────────────────────────────────

    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        poseStack.translate(0.0F, -1.1F, 0.0F);
        poseStack.scale(0.7F, 0.7F, 0.7F);
    }

    // ── Transformaciones de Ítems ────────────────────────────────────────────

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack, boolean isMainHand, ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);
        if (isActionItem(itemStack)) return;

        if (!isMainHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        }
        poseStack.translate(0.0D, 0.05D, 0.0D);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isMainHand) {
        super.applyEmptyHandTransform(poseStack, isMainHand);
        if (isMainHand) {
            poseStack.translate(0.15D, 0.0D, 0.0D);
        } else {
            poseStack.translate(-0.05D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack, boolean isMainHand, boolean isOffHand) {
        super.applyTwoHandedTransform(poseStack, isMainHand, isOffHand);
        if (isMainHand && !isOffHand) {
            poseStack.translate(-0.025D, -0.1D, -0.1D);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
        } else if (!isMainHand && !isOffHand) {
            poseStack.translate(-0.05D, -0.125D, 0.125D);
            poseStack.mulPose(Axis.XP.rotationDegrees(50.0F));
        }
    }

    // ── Físicas de Huesos (On-the-fly) ───────────────────────────────────────

    // Asumo que tienes un método similar en NpcHandRenderer o que lo llamas desde el preRender de GeckoLib
    protected void onBoneProcess(String boneName, CoreGeoBone bone, BaseNpcEntity npc) {
        if (npc == null || npc.getEntityData().get(BaseNpcEntity.FROZEN)) return;

        // Intentamos obtener al dueño para calcular la inercia en base a él
        Player owner = null;
        if (npc.getOwnerUUID() != null) {
            owner = npc.level().getPlayerByUUID(npc.getOwnerUUID());
        }

        // Si no hay dueño cerca, usamos la propia velocidad del NPC
        double dx = owner != null ? owner.getX() - owner.xo : npc.getX() - npc.xo;
        double dz = owner != null ? owner.getZ() - owner.zo : npc.getZ() - npc.zo;

        float partialTick = Minecraft.getInstance().getPartialTick();

        switch (boneName) {
            case "tail":
                applyTailPhysics(bone, npc, dx, dz, 0.0F, 0.0F, 1.0F, partialTick);
                break;
            case "body":
                applyBodyPhysics(bone, npc, dx, dz, partialTick);
                break;
            case "armL":
                if (npc.getAnimState() != AnimState.BOW) {
                    applyTailPhysics(bone, npc, dx, dz, 0.0F, -0.34906584F, 0.15F, partialTick);
                }
                break;
            case "armR":
                if (npc.getAnimState() != AnimState.BOW && npc.getAnimState() != AnimState.ATTACK) {
                    applyTailPhysics(bone, npc, dx, dz, 0.0F, 0.34906584F, 0.15F, partialTick);
                }
                break;
        }
    }

    private void applyTailPhysics(CoreGeoBone bone, BaseNpcEntity npc, double dx, double dz,
                                  float baseX, float baseZ, float scale, float partialTick) {
        double yaw = Math.toRadians(npc.getYRot());

        // Velocidad en espacio local
        float localX = (float) (dx * Math.cos(yaw) + dz * Math.sin(yaw));
        float localZ = (float) (-dx * Math.sin(yaw) + dz * Math.cos(yaw));

        float targetX = Mth.clamp(localX * -MAX_SWAY, -MAX_LEAN, MAX_LEAN);
        float targetZ = Mth.clamp(localZ * MAX_SWAY, -MAX_LEAN, MAX_LEAN);

        // Suavizado dinámico sin necesidad de Ticker estático
        this.tailRotX = Mth.lerp(partialTick * 0.5f, this.tailRotX, targetX);
        this.tailRotZ = Mth.lerp(partialTick * 0.5f, this.tailRotZ, targetZ);

        bone.setRotX(baseX + this.tailRotX * scale);
        bone.setRotZ(baseZ + this.tailRotZ * scale);
    }

    private void applyBodyPhysics(CoreGeoBone bone, BaseNpcEntity npc, double dx, double dz, float partialTick) {
        float speed = Mth.clamp((float) ((Math.abs(dx) + Math.abs(dz)) * BODY_SCALE), 0.0F, 1.0F);

        // Suavizado dinámico
        this.bodyBob = Mth.lerp(partialTick * 0.5f, this.bodyBob, speed);

        bone.setPosY(Mth.lerp(this.bodyBob, BODY_SCALE, 0.0F));

        if (npc instanceof AllieEntity allie) {
            allie.extraBobAmount = Mth.lerp(this.bodyBob, 0.3F, 0.0F);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isActionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        UseAnim action = stack.getUseAnimation();
        return action == UseAnim.BOW || action == UseAnim.CROSSBOW;
    }
}