package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.ClientProxy;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.NpcType;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CustomizeNpcPacket;
import com.trolmastercard.sexmod.ClothingSlot;
import com.trolmastercard.sexmod.registry.ClothingRegistry;
import com.trolmastercard.sexmod.registry.ModelWhitelist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.*;

/**
 * NPC Customization Screen - ported to 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class NpcCustomizeScreen extends Screen {

    public static final ResourceLocation ICONS_TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/clothing_icons.png");

    private static final int ICON_SIZE = 20;
    private static final float INERTIA_DECAY = 0.25F;

    public static final List<Integer> pendingDeltas = new ArrayList<>();
    public static int currentFrameDelta = 0;
    public static int previousFrameDelta = 0;

    public static float previewRotation = 0.0F;

    private int dragAnchorX = 0;
    private boolean isDragging = false;

    public static List<Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>>> slotEntries =
            new ArrayList<>();

    private int girlSpecificCount = 0;
    private BaseNpcEntity previewEntity;
    private final UUID npcUUID;
    private ClothingScrollWidget scrollWidget;
    public boolean clickConsumed = false;
    private int inertiaDelta = 0;
    private int inertiaSign = 1;

    private int previewX;
    private int previewY;
    private float previewScale;

    public NpcCustomizeScreen(BaseNpcEntity npc) {
        super(Component.translatable("gui.sexmod.customize"));
        this.npcUUID = npc.getUUID();

        NpcType npcType = NpcType.fromEntity(npc);
        if (npcType == null) npcType = NpcType.JENNY;

        try {
            Constructor<? extends BaseNpcEntity> ctor =
                    npcType.entityClass.getConstructor(
                            net.minecraft.world.entity.EntityType.class,
                            net.minecraft.world.level.Level.class
                    );
            this.previewEntity = ctor.newInstance(npcType.entityType, Minecraft.getInstance().level);
            this.previewEntity.setPreviewMode(true);
        } catch (Exception e) {
            Main.LOGGER.error("Failed to create preview entity for customisation screen", e);
        }

        applyCurrentCustomisation(npc);
        populateSlotEntries();
    }

    @Override
    protected void init() {
        this.scrollWidget = new ClothingScrollWidget(minecraft, this);
        this.previewX     = pctW(76.0F);
        this.previewY     = pctH(89.0F);
        this.previewScale = 90.0F;
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        this.previewX     = pctW(76.0F);
        this.previewY     = pctH(89.0F);
    }

    @Override
    public void removed() {
        super.removed();
        if (this.previewEntity != null && minecraft != null && minecraft.level != null) {
            minecraft.level.removeEntity(this.previewEntity.getId(),
                    net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        pendingDeltas.clear();
        slotEntries.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); // Actualizado para 1.20.1
        super.render(graphics, mouseX, mouseY, partialTick);

        if (isDragging) {
            previewRotation += interpolateDelta(currentFrameDelta, previousFrameDelta, partialTick);
        }
        applyRotationInertia();

        int btnX = previewX - pctW(15.0F);
        int btnY = previewY - ICON_SIZE;
        renderIcon(graphics, btnX, btnY, isHovered(mouseX, mouseY, btnX, btnY) ? 40 : 20, 20, 20);

        if (ModelWhitelist.getActiveModel() == null) {
            renderUtilityButtons(graphics, mouseX, mouseY, btnX);
        }

        if (previewEntity != null) {
            renderEntityPreview(graphics, previewX, previewY, previewScale, previewEntity, mouseX, mouseY);
        }

        if (scrollWidget != null) {
            scrollWidget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderUtilityButtons(GuiGraphics graphics, int mouseX, int mouseY, int x) {
        int y = previewY - 40;
        renderIcon(graphics, x, y, isHovered(mouseX, mouseY, x, y) ? 40 : 20, 20, 20);

        y -= ICON_SIZE;
        renderIcon(graphics, x, y, isHovered(mouseX, mouseY, x, y) ? 170 : 150, 20, 20);

        y -= ICON_SIZE;
        renderIcon(graphics, x, y, isHovered(mouseX, mouseY, x, y) ? 170 : 150, 20, 20);
    }

    private void renderEntityPreview(GuiGraphics graphics, int x, int y, float scale,
                                     BaseNpcEntity entity, int mouseX, int mouseY) {
        float savedYRot        = entity.yRot;
        float savedXRot        = entity.xRot;
        float savedYHeadRot    = entity.yHeadRot;
        float savedYHeadRotO   = entity.yHeadRotO;
        float savedYBodyRot    = entity.yBodyRot;

        entity.yRot        = 180.0F + previewRotation;
        entity.xRot        = 0.0F;
        entity.yHeadRot    = entity.yRot;
        entity.yHeadRotO   = entity.yRot;
        entity.yBodyRot    = entity.yRot;

        // Firma correcta para 1.20.1
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX((float) (-10 * Math.PI / 180.0));

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, (int) scale,
                (float) (x - mouseX), (float) (y - mouseY - scale), entity);

        entity.yRot        = savedYRot;
        entity.xRot        = savedXRot;
        entity.yHeadRot    = savedYHeadRot;
        entity.yHeadRotO   = savedYHeadRotO;
        entity.yBodyRot    = savedYBodyRot;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (scrollWidget != null) scrollWidget.mouseClicked(mouseX, mouseY, button);
        if (button != 0) return handled;

        this.clickConsumed = true;
        this.isDragging    = true;
        this.dragAnchorX   = (int) mouseX;

        int x = (int) mouseX;
        int y = (int) mouseY;
        int btnX = previewX - pctW(15.0F);
        int btnY = previewY - ICON_SIZE;

        if (isHovered(x, y, btnX, btnY)) {
            playClickSound();
            sendCustomisationToServer();
            onClose();
            return true;
        }

        if (ModelWhitelist.getActiveModel() == null) {
            handleUtilityButtonClick(x, y, btnX);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            this.isDragging = false;
            this.clickConsumed = false;
            this.inertiaDelta = currentFrameDelta;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (button != 0 || mouseX < (double)(width / 2)) return false;

        int delta = (int) mouseX - dragAnchorX;
        pendingDeltas.add(delta);
        dragAnchorX = (int) mouseX;
        return true;
    }

    private void handleUtilityButtonClick(int mouseX, int mouseY, int x) {
        int y = previewY - 40;

        if (isHovered(mouseX, mouseY, x, y)) {
            playClickSound();
            onClose();
            int available = ModelWhitelist.countAvailableModels(true);
            if (available != 0) {
                ModelWhitelist.setSelectingModel(true);
                return;
            }
            BaseNpcEntity npc = getNpcFromWorld();
            if (npc != null) openScreen(npc);
            return;
        }

        y -= ICON_SIZE;
        if (isHovered(mouseX, mouseY, x, y)) {
            try {
                Desktop.getDesktop().open(new File(ModelWhitelist.getModelsFolder()));
            } catch (Exception e) {
                Main.LOGGER.warn("Could not open models folder", e);
            }
            return;
        }

        y -= ICON_SIZE;
        if (isHovered(mouseX, mouseY, x, y)) {
            try {
                Desktop.getDesktop().browse(new URI("http://fapcraft.org/assets/video/tutorial/girl_wand.mp4"));
            } catch (Exception e) {
                Main.LOGGER.warn("Could not open tutorial URL", e);
            }
        }
    }

    private void applyRotationInertia() {
        if (isDragging) return;

        float fps = (float) Minecraft.getInstance().getFps();
        if (fps == 0.0F) fps = 0.1F;

        if (inertiaDelta == 0) {
            previewRotation += (inertiaSign * 10.0F) / fps;
            return;
        }

        previewRotation += inertiaDelta / fps;
        inertiaDelta = (int)(inertiaDelta * (1.0F - INERTIA_DECAY / fps));

        if (Math.abs(inertiaDelta) <= 10) {
            inertiaSign  = (inertiaDelta > 0) ? 1 : -1;
            inertiaDelta = 0;
        }
    }

    private float interpolateDelta(int current, int previous, float partialTick) {
        return previous + (current - previous) * partialTick;
    }

    public void cycleSlot(ClothingSlot slot, boolean forward, int entryIndex) {
        playClickSound();

        List<Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>>> matching = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int pos = 0;
        for (Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>> entry : slotEntries) {
            if (entry.getKey().equals(slot)) {
                matching.add(entry);
                indices.add(pos);
            }
            pos++;
        }

        if (matching.isEmpty()) return;

        Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>> target;
        int targetPos;

        if (matching.size() == 1) {
            target    = matching.get(0);
            targetPos = indices.get(0);
        } else {
            int adjusted = (girlSpecificCount == 0 || entryIndex > girlSpecificCount - 1 + ClothingSlot.extraCount())
                    ? entryIndex - girlSpecificCount + ClothingSlot.extraCount()
                    : entryIndex;
            target    = matching.get(adjusted);
            targetPos = indices.get(adjusted);
        }

        List<String> options  = target.getValue().getKey();
        int          selected = target.getValue().getValue();
        int          size     = options.size();

        selected = forward ? (selected + 1) % size : (selected - 1 + size) % size;

        slotEntries.set(targetPos,
                new AbstractMap.SimpleEntry<>(
                        target.getKey(),
                        new AbstractMap.SimpleEntry<>(options, selected)
                )
        );

        List<Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>>> girlSpecific = new ArrayList<>();
        for (var e : slotEntries) {
            if (e.getKey() == ClothingSlot.GIRL_SPECIFIC) girlSpecific.add(e);
        }
        if (previewEntity != null) previewEntity.applyCustomisationEntries(girlSpecific);
    }

    private void sendCustomisationToServer() {
        playClickSound();

        Set<String>  textures      = new HashSet<>();
        List<Integer> girlSpecific = new ArrayList<>();

        for (var entry : slotEntries) {
            if (entry.getKey() == ClothingSlot.GIRL_SPECIFIC) {
                girlSpecific.add(entry.getValue().getValue());
                continue;
            }
            int selected = entry.getValue().getValue();
            if (selected == 0) continue;
            textures.add(entry.getValue().getKey().get(selected));
        }

        ModNetwork.CHANNEL.sendToServer(
                new CustomizeNpcPacket(BaseNpcEntity.encodeTextures(textures), npcUUID, girlSpecific)
        );
    }

    private void populateSlotEntries() {
        slotEntries.clear();
        if (previewEntity == null) return;

        List<Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>>> npcEntries =
                previewEntity.getCustomisationEntries(npcUUID);
        girlSpecificCount = npcEntries.size();
        slotEntries.addAll(npcEntries);

        for (ClothingSlot slot : ClothingSlot.values()) {
            if (slot == ClothingSlot.GIRL_SPECIFIC) continue;
            List<String> options = new ArrayList<>();
            options.add("cross");
            slotEntries.add(new AbstractMap.SimpleEntry<>(
                    slot, new AbstractMap.SimpleEntry<>(options, 0)
            ));
        }

        Map<ClothingSlot, List<String>> available = ClothingRegistry.getAvailableTextures(previewEntity);
        for (var entry : available.entrySet()) {
            Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>> target = null;
            for (var e : slotEntries) {
                if (e.getKey().equals(entry.getKey())) { target = e; break; }
            }
            if (target == null) continue;
            int idx = slotEntries.indexOf(target);
            slotEntries.remove(target);
            target.getValue().getKey().addAll(entry.getValue());
            slotEntries.add(idx, target);
        }
    }

    private void applyCurrentCustomisation(BaseNpcEntity npc) {
        if (previewEntity == null) return;
        String modelData = npc.getModelData();
        previewEntity.entityData.set(BaseNpcEntity.MODEL_DATA, modelData);

        for (String texture : previewEntity.getActiveTextures()) {
            ClothingSlot slot = ClothingRegistry.slotForTexture(texture);
            if (slot == null) continue;

            for (var entry : slotEntries) {
                if (!entry.getKey().equals(slot)) continue;
                int idx = entry.getValue().getKey().indexOf(texture);
                if (idx == -1) idx = 0;
                entry.getValue().setValue(idx);
            }
        }
    }

    public void renderIcon(GuiGraphics graphics, int x, int y, int u, int texW, int texH) {
        graphics.blit(ICONS_TEXTURE, x, y, u, 0, texW, texH, 256, 256);
    }

    public void renderIcon(GuiGraphics graphics, int x, int y, UvCoord uv) {
        renderIcon(graphics, x, y, uv.u(), uv.v(), ICON_SIZE);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + ICON_SIZE
                && mouseY >= y && mouseY <= y + ICON_SIZE;
    }

    private int pctW(float pct) { return Math.round(width  * pct / 100.0F); }
    private int pctH(float pct) { return Math.round(height * pct / 100.0F); }

    private void playClickSound() {
        minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    @javax.annotation.Nullable
    private BaseNpcEntity getNpcFromWorld() {
        if (minecraft == null || minecraft.level == null) return null;

        // Forma correcta de buscar una entidad por UUID en el cliente
        for (net.minecraft.world.entity.Entity e : minecraft.level.entitiesForRendering()) {
            if (e.getUUID().equals(npcUUID) && e instanceof BaseNpcEntity) {
                return (BaseNpcEntity) e;
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static void openFor(BaseNpcEntity npc) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NpcCustomizeScreen) return;

        boolean whitelisted = ModelWhitelist.getServerUrl() != null && ModelWhitelist.isWhitelisted();
        if (!whitelisted) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(
                        "You have to whitelist the server to use its custom models. \u00a7e/whitelistserver"
                ));
            }
            return;
        }

        mc.execute(() -> mc.setScreen(new NpcCustomizeScreen(npc)));
    }

    public Minecraft getMinecraft() { return minecraft; }

    public String getSelectedTexture(int entryIndex) {
        if (entryIndex < 0 || entryIndex >= slotEntries.size()) return "cross";
        var entry = slotEntries.get(entryIndex);
        int sel   = entry.getValue().getValue();
        List<String> opts = entry.getValue().getKey();
        return (sel >= 0 && sel < opts.size()) ? opts.get(sel) : "cross";
    }

    public int getSlotCount() { return slotEntries.size(); }

    public ClothingSlot getSlot(int index) {
        if (index < 0 || index >= slotEntries.size()) return ClothingSlot.GIRL_SPECIFIC;
        return slotEntries.get(index).getKey();
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            // Asegúrate de que tu array ClientProxy.KEY_BINDINGS exista en 1.20.1
            if (!ClientProxy.KEY_BINDINGS[1].isDown()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (ModelWhitelist.isSelectingModel()) {
                boolean remaining = ModelWhitelist.countAvailableModels(true) != 0;
                ModelWhitelist.setSelectingModel(remaining);
                if (remaining) return;
            }

            BaseNpcEntity npc = BaseNpcEntity.getActiveNpcForPlayer(mc.player.getUUID());
            if (npc == null) {
                mc.player.sendSystemMessage(
                        Component.literal("You have to turn into the girl you want to customize")
                );
                return;
            }

            NpcCustomizeScreen.openFor(npc);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            previousFrameDelta = currentFrameDelta;
            currentFrameDelta  = 0;
            for (int d : pendingDeltas) currentFrameDelta += d;
            pendingDeltas.clear();
        }
    }

    public record UvCoord(int u, int v) {
        public static final UvCoord NONE = new UvCoord(0, 0);
    }
}