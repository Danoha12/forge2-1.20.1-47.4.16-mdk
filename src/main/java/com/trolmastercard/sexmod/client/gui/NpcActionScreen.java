package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.RemoveItemsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * NpcActionScreen — Portado a 1.20.1.
 * * Menú lateral de acciones con animaciones de entrada (Fade-in).
 * * Muestra equipamiento y botones de interacción.
 */
@OnlyIn(Dist.CLIENT)
public class NpcActionScreen extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    private final BaseNpcEntity npc;
    private final Player player;
    private final String[] customLabels;
    private final ItemStack[] customCosts;
    private final boolean showEquipment;

    private float fadeIn = 0.0F;
    private float panelFade = 0.0F;

    private final String[] baseActions = {
            "action.names.followme",
            "action.names.stopfollowme",
            "action.names.gohome",
            "action.names.setnewhome",
            "action.names.equipment"
    };

    private final int[] buttonOffset = {0, 0, 0, 0, 0};
    private final int[] iconV = {64, 80, 47, 32, 96};
    private final int[] maxOffset = {50, 90, 50, 80, 60};

    // Guardamos los botones para actualizar su posición sin recrearlos
    private final List<Button> actionButtons = new ArrayList<>();

    public NpcActionScreen(BaseNpcEntity npc, Player player, String[] customLabels, @Nullable ItemStack[] customCosts, boolean showEquipment) {
        super(Component.empty());
        this.npc = npc;
        this.player = player;
        this.customLabels = customLabels != null ? customLabels : new String[0];
        this.customCosts = customCosts != null ? customCosts : new ItemStack[0];
        this.showEquipment = showEquipment;
    }

    @Override
    protected void init() {
        actionButtons.clear();
        int totalButtons = 5 + customLabels.length;

        // Creamos los botones una sola vez en el init
        for (int i = 0; i < totalButtons; i++) {
            int index = i;
            String labelKey = (i < 5) ? baseActions[i] : customLabels[i - 5];

            Button btn = Button.builder(Component.translatable(labelKey), b -> onActionButton(index))
                    .size(100, 20)
                    .build();

            this.addRenderableWidget(btn);
            actionButtons.add(btn);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscuro de la GUI
        this.renderBackground(graphics);

        // Actualizar animaciones
        float delta = Minecraft.getInstance().getDeltaFrameTime() / 5.0F;
        fadeIn = Math.min(1.0F, fadeIn + delta);
        if (fadeIn >= 1.0F) {
            panelFade = Math.min(1.0F, panelFade + delta);
        }

        // Calcular posiciones lerp
        int btnXBase = (int) Mth.lerp(fadeIn, -120.0F, 20.0F); // Cambiado para que entren desde la derecha
        int screenW = this.width;
        int screenH = this.height;
        int baseY = screenH - 70;

        // ── Renderizado y Posicionamiento de Botones ─────────────────────────
        for (int i = 0; i < actionButtons.size(); i++) {
            int typeIdx = Math.min(i, 4);
            Button btn = actionButtons.get(i);

            // Lógica de hover para el desplazamiento lateral (Offset)
            if (btn.isHoveredOrFocused()) {
                buttonOffset[typeIdx] = Math.min(maxOffset[typeIdx], buttonOffset[typeIdx] + 7);
            } else {
                buttonOffset[typeIdx] = Math.max(0, buttonOffset[typeIdx] - 7);
            }

            // Actualizar posición del botón basándonos en la animación
            int xPos = screenW - btnXBase - 100 - buttonOffset[typeIdx];
            int yPos = baseY - (i * 25) + (int) Mth.lerp(panelFade, 0.0F, 10.0F);

            btn.setX(xPos);
            btn.setY(yPos);

            // Dibujar Icono del botón
            int iconX = xPos - 22;
            int iconY = yPos + 2;
            graphics.blit(TEXTURE, iconX, iconY, iconV[typeIdx], 0, 16, 16);

            // Si hay costo, dibujarlo al lado del botón
            if (i >= 5 && customCosts.length > (i - 5)) {
                renderCost(graphics, i - 5, xPos - 45, yPos);
            }
        }

        // ── Panel de Equipamiento ────────────────────────────────────────────
        if (showEquipment && panelFade > 0) {
            renderEquipment(graphics, screenW, screenH);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCost(GuiGraphics graphics, int idx, int x, int y) {
        ItemStack cost = customCosts[idx];
        if (cost != null && !cost.isEmpty()) {
            graphics.renderFakeItem(cost, x, y);
            graphics.drawString(this.font, cost.getCount() + "x", x - 15, y + 4, 0xFFFFFF);
        }
    }

    private void renderEquipment(GuiGraphics graphics, int screenW, int screenH) {
        int equipX = screenW - (int) Mth.lerp(fadeIn, 0.0F, 130.0F);
        int baseY = screenH - 180;

        // Fondo del panel de equipo
        graphics.blit(TEXTURE, equipX, baseY, 0, 0, 32, 130);

        // Slots definidos en NpcInventoryEntity
        EntityDataAccessor<ItemStack>[] slots = new EntityDataAccessor[]{
                NpcInventoryEntity.SLOT_CHEST, NpcInventoryEntity.SLOT_LEGS,
                NpcInventoryEntity.SLOT_FEET, NpcInventoryEntity.SLOT_MAIN_HAND,
                NpcInventoryEntity.SLOT_OFF_HAND, NpcInventoryEntity.SLOT_EXTRA
        };

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = npc.getEntityData().get(slots[i]);
            if (!stack.isEmpty()) {
                graphics.renderFakeItem(stack, equipX + 8, baseY + 10 + (i * 20));
            }
        }
    }

    private void onActionButton(int idx) {
        // Lógica de costo para botones custom
        if (idx >= 5) {
            int customIdx = idx - 5;
            ItemStack cost = customCosts[customIdx];
            if (cost != null && !cost.isEmpty() && !player.getAbilities().instabuild) {
                if (!hasItems(player, cost)) {
                    // Sonido de desaprobación (Usando el sistema que definiste antes)
                    return;
                }
                ModNetwork.CHANNEL.sendToServer(new RemoveItemsPacket(player.getUUID(), cost));
            }
        }

        // Ejecutar acción
        String actionKey = (idx < 5) ? baseActions[idx] : customLabels[idx - 5];
        npc.triggerAction(actionKey, player.getUUID());
        this.onClose();
    }

    private boolean hasItems(Player player, ItemStack cost) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == cost.getItem()) count += stack.getCount();
        }
        return count >= cost.getCount();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        // npc.cancelInteraction(); // Descomentar si tienes el método en BaseNpcEntity
        super.onClose();
    }
}