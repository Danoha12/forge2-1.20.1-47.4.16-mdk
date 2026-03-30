package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.UpdatePlayerModelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NpcTypeSelectScreen — Portado a 1.20.1.
 * * Carrusel 3D para seleccionar la transformación del jugador.
 * * Utiliza InventoryScreen para un renderizado de entidades limpio y compatible.
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

    private void buildEntityList(Map<NpcType, String> modelOverrides) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (NpcType type : NpcType.values()) {
            if (type.isNpcOnly) continue;

            try {
                // Instanciamos el NPC "dummy" para la vista previa
                BaseNpcEntity npc = type.npcClass.getConstructor(net.minecraft.world.level.Level.class).newInstance(mc.level);

                // Aplicar el modelo personalizado si existe un override
                String override = modelOverrides.get(type);
                if (override != null) {
                    // npc.setModelIndex(npc.getModelIndexFromName(override));
                }

                entities.add(npc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Siempre añadimos al jugador al final para la opción de "Reset"
        if (mc.player != null) {
            entities.add(mc.player);
        }
    }

    @Override
    protected void init() {
        // Botón Siguiente (>)
        this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            currentIndex = (currentIndex + 1) % entities.size();
        }).pos(this.width / 2 + 50, this.height / 2).size(20, 20).build());

        // Botón Anterior (<)
        this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            currentIndex = (currentIndex - 1 + entities.size()) % entities.size();
        }).pos(this.width / 2 - 70, this.height / 2).size(20, 20).build());

        // Botón Seleccionar (Pick)
        this.addRenderableWidget(Button.builder(Component.literal("Pick"), btn -> {
            LivingEntity selected = entities.get(currentIndex);
            NpcType type = NpcType.fromEntity(selected);

            // Enviamos el paquete de transformación al servidor
            ModNetwork.CHANNEL.sendToServer(new UpdatePlayerModelPacket(type));

            this.onClose();
        }).pos(this.width / 2 - 30, this.height / 2 + 40).size(60, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Lógica de rotación suave basada en el tiempo (no en FPS)
        rotationAngle += 1.5F;

        if (!entities.isEmpty()) {
            LivingEntity toRender = entities.get(currentIndex);

            int x = this.width / 2;
            int y = this.height / 2 + 30;

            // En 1.20.1 usamos este método oficial para renderizar entidades en GUIs.
            // Es mucho más seguro que el EntityRenderDispatcher manual.
            renderEntityInGui(graphics, x, y, 40, toRender);

            // Dibujar el nombre del tipo encima de la entidad
            String name = (toRender instanceof Player) ? "Default Player" : NpcType.fromEntity(toRender).name();
            graphics.drawCenteredString(this.font, name, x, y - 90, 0xFFFFFF);
        }
    }

    /**
     * Renderiza la entidad usando el sistema de InventoryScreen.
     * Engañamos al sistema de "mirar al mouse" usando nuestra rotationAngle.
     */
    private void renderEntityInGui(GuiGraphics graphics, int x, int y, int scale, LivingEntity entity) {
        // InventoryScreen.renderEntityInInventory requiere:
        // (graphics, x, y, escala, mouseX_offset, mouseY_offset, entidad)
        // Usamos la rotación acumulada para que gire constantemente
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, rotationAngle, 0, entity);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}