package com.trolmastercard.sexmod.client.screen;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.UpdatePlayerModelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NpcTypeSelectScreen - ported from b5.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A GUI carousel that lets the player preview and select which NPC type to
 * attach to themselves as a {@link com.trolmastercard.sexmod.entity.PlayerKoboldEntity}.
 *
 * Renders each entity type as a spinning 3-D preview using
 * {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher}.
 *
 * Navigation:
 *   ">" button - next entity in carousel
 *   "<" button - previous entity in carousel
 *   "pick" button - sends {@link UpdatePlayerModelPacket} to server
 *
 * The last element in the carousel is always the local player (for the
 * "player" / no-NPC mode).
 *
 * In 1.12.2:
 *   - Extended {@code GuiScreen} - now extends {@code Screen}.
 *   - {@code GuiButton} - {@code Button} (using {@code Button.builder}).
 *   - {@code func_73863_a} - {@link #render(PoseStack, int, int, float)}.
 *   - {@code func_146284_a} - button {@code onPress} lambdas.
 *   - Entity preview: GlStateManager + RenderHelper + RenderManager -
 *     JOML + RenderSystem + {@link EntityRenderDispatcher}.
 *   - {@code ge.b.sendToServer(new b_(...)} - {@code ModNetwork.CHANNEL.sendToServer(...)}.
 *   - The auto-rotating angle accumulator ({@code c}) has been moved to an
 *     instance field ({@link #rotationAngle}) to avoid static state leaking between screens.
 */
@OnlyIn(Dist.CLIENT)
public class NpcTypeSelectScreen extends Screen {

    private final List<LivingEntity> entities = new ArrayList<>();
    private int currentIndex = 0;
    private float rotationAngle = 0.0F;

    public NpcTypeSelectScreen(Map<NpcType, String> modelOverrides) {
        super(Component.literal("Choose NPC"));
        buildEntityList(modelOverrides);
    }

    // =========================================================================
    //  Build carousel entries
    // =========================================================================

    private void buildEntityList(Map<NpcType, String> modelOverrides) {
        Minecraft mc = Minecraft.getInstance();
        for (NpcType type : NpcType.values()) {
            if (type.isNpcOnly) continue;   // skip NPC-only types in the picker

            try {
                Constructor<? extends BaseNpcEntity> ctor =
                    type.npcClass.getConstructor(net.minecraft.world.level.Level.class);
                BaseNpcEntity npc = ctor.newInstance(mc.level);
                npc.setSuppressRendering(true);         // em.b(true)

                String override = modelOverrides.get(type);
                if (override != null) {
                    npc.setModelIndex(npc.getModelIndexFromName(override));  // em.c(name)
                }

                entities.add(npc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Always add the local player as the last option
        if (mc.player != null) {
            entities.add(mc.player);
        }
    }

    // =========================================================================
    //  Screen overrides
    // =========================================================================

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);

        // Advance rotation
        float fps = Minecraft.getInstance().getFps();
        if (fps == 0) fps = 0.1F;
        rotationAngle += 60.0F / fps;

        // Draw entity preview
        int cx = width / 2;
        int cy = height / 2 + 20;
        renderEntityPreview(cx, cy, 30, entities.get(currentIndex));
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (++currentIndex >= entities.size()) currentIndex = 0;
        }).pos(width / 2 + 30, height / 2 - 10).size(20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (--currentIndex < 0) currentIndex = entities.size() - 1;
        }).pos(width / 2 - 50, height / 2 - 10).size(20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("pick"), btn -> {
            LivingEntity selected = entities.get(currentIndex);

            // Determine the NpcType (null = player)
            NpcType type = null;
            if (selected instanceof BaseNpcEntity npc) {
                type = NpcType.fromNpcClass(npc.getClass());
            }

            ModNetwork.CHANNEL.sendToServer(new UpdatePlayerModelPacket(type));

            // Reset player eye height and flight
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
                mc.setScreen(null);
                player.eyeHeight = player.getDefaultEyeHeight(
                    net.minecraft.world.entity.Pose.STANDING,
                    player.getDimensions(net.minecraft.world.entity.Pose.STANDING));
                if (!player.getAbilities().invulnerable) {
                    player.getAbilities().flying = player.getAbilities().mayfly;
                }
            }
        }).pos(width / 2 - 30, height / 2 + 30).size(60, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =========================================================================
    //  Entity preview renderer
    //  Original: b5.a(int, int, int, EntityLivingBase) - now static
    // =========================================================================

    /**
     * Renders a spinning 3-D entity preview at screen coordinates ({@code x}, {@code y}).
     *
     * Ported from b5 which used GlStateManager + RenderHelper directly.
     * In 1.20.1 we build a JOML transform and pass it to
     * {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher}.
     */
    public void renderEntityPreview(int x, int y, int scale, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Preserve rotation state
        float prevYaw        = entity.yRotO;
        float prevBodyRot    = entity.yBodyRot;
        float prevHeadYaw    = entity.yHeadRotO;
        float prevPitch      = entity.xRotO;
        float prevHeadPitch  = entity.getXRot();

        // Zero entity position for preview (non-player entities)
        double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
        if (!(entity instanceof Player)) {
            entity.setPos(0, 0, 0);
        }

        entity.yRotO         = 0;
        entity.yBodyRot      = 0;
        entity.setXRot(0);
        entity.yHeadRotO     = 0;

        // Build camera transform
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(x, y, 50.0);
        modelView.scale(-scale, scale, scale);
        modelView.mulPose(new Quaternionf().rotateZ((float)Math.toRadians(180)));
        modelView.mulPose(new Quaternionf().rotateY((float)Math.toRadians(135)));

        net.minecraft.client.renderer.LightTexture.turnOffLights();

        modelView.mulPose(new Quaternionf().rotateY((float)Math.toRadians(-135)));
        modelView.mulPose(new Quaternionf().rotateY((float)Math.toRadians(rotationAngle)));
        modelView.translate(0, 0, 0);

        RenderSystem.applyModelViewMatrix();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        var bufferSource = mc.renderBuffers().bufferSource();
        dispatcher.render(
            entity,
            0.0, 0.0, 0.0,
            0.0F,
            1.2345679F,
            new PoseStack(),
            bufferSource,
            LightTexture.FULL_BRIGHT);

        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();

        net.minecraft.client.renderer.LightTexture.turnOnLights();

        // Restore entity state
        if (!(entity instanceof Player)) {
            entity.setPos(ox, oy, oz);
        }
        entity.yRotO        = prevYaw;
        entity.yBodyRot     = prevBodyRot;
        entity.setXRot(prevHeadPitch);
        entity.yHeadRotO    = prevHeadYaw;
    }
}
