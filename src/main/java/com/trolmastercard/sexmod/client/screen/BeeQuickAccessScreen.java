package com.trolmastercard.sexmod.client.screen;

import com.trolmastercard.sexmod.entity.BeeEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.ChangeDataParameterPacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import com.trolmastercard.sexmod.network.packet.BindPlayerToNpcPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

/**
 * BeeQuickAccessScreen — Portado a 1.20.1 y enmascarado (SFW).
 *
 * Menú rápido animado que aparece al interactuar con el NPC Bee.
 * Opciones: Seguir/Parar, Ir a casa y Establecer casa.
 */
@OnlyIn(Dist.CLIENT)
public class BeeQuickAccessScreen extends Screen {

    private final BeeEntity beeEntity;
    private final Player player;
    private boolean isFollowing;
    private double animProgress = 0.0D;

    // Referencias a botones para actualizar su posición sin recrearlos
    private Button followBtn;
    private Button homeBtn;
    private Button setHomeBtn;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/girl_inventory.png");

    public BeeQuickAccessScreen(BeeEntity bee, Player player) {
        super(Component.empty());
        this.beeEntity = bee;
        this.player = player;

        // Obtener estado de seguimiento desde los datos sincronizados del NPC
        String masterUUID = bee.getEntityData().get(BaseNpcEntity.MASTER_UUID);
        this.isFollowing = !masterUUID.isEmpty();
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        // Inicializamos los botones con valores base (se moverán en el render)
        this.followBtn = addRenderableWidget(Button.builder(
                        Component.translatable(isFollowing ? "action.names.stopfollowme" : "action.names.followme"),
                        btn -> onFollowButton())
                .pos(centerX, 30)
                .size(100, 20)
                .build());

        this.homeBtn = addRenderableWidget(Button.builder(
                        Component.translatable("action.names.gohome"),
                        btn -> onGoHomeButton())
                .pos(centerX, 30)
                .size(100, 20)
                .build());

        this.setHomeBtn = addRenderableWidget(Button.builder(Component.empty(),
                        btn -> onSetHomeButton())
                .pos(centerX, 59)
                .size(20, 20)
                .build());

        // El botón de setHome suele ser invisible/transparente sobre el icono
        // this.setHomeBtn.setAlpha(0.0f);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscuro degradado de los menús
        this.renderBackground(graphics);

        // Avanzar animación (Aprox. 5 ticks para completar)
        animProgress = Math.min(1.0D, animProgress + (partialTick / 10.0F));

        int centerX = width / 2;
        int animWidth = (int)(animProgress * 100.0D);
        int animOffsetX = (int)(100.0D - 100.0D * animProgress);

        // Actualizar posiciones y anchos de los botones dinámicamente
        followBtn.setX(centerX - 119 + animOffsetX);
        followBtn.setWidth(animWidth);

        homeBtn.setX(centerX + 19);
        homeBtn.setWidth(animWidth);

        // Dibujar textura de la interfaz
        int iconY = 61 - (int)(15.0D - animProgress * 15.0D);

        // Renderizar el icono de la "Casa"
        graphics.blit(GUI_TEXTURE, centerX - 7, iconY, 32, 0, 15, 15);
        setHomeBtn.setY(iconY - 2);

        // Retrato del NPC: Estado Especial (SFW: Reemplaza "Pregnant") vs Normal
        // Asumiendo que IS_SPECIAL_STATE es el nuevo nombre del parámetro
        boolean isSpecialState = beeEntity.getEntityData().get(BeeEntity.DATA_TAMED);

        graphics.blit(GUI_TEXTURE,
                centerX - 20, 20,
                isSpecialState ? 0 : 40, 130,
                40, 40);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        boolean isSpecialState = beeEntity.getEntityData().get(BeeEntity.DATA_TAMED);

        // Interacción al hacer clic en el retrato del NPC
        if (isSpecialState) {
            if (mouseX >= centerX - 20 && mouseX <= centerX + 20 && mouseY >= 20 && mouseY <= 60) {
                ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                        new BindPlayerToNpcPacket(beeEntity.getUUID(), player.getUUID()));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // =========================================================================
    //  Manejadores de Botones
    // =========================================================================

    private void onFollowButton() {
        if (isFollowing) {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                    new ChangeDataParameterPacket(beeEntity.getUUID(), "master", ""));
            player.displayClientMessage(Component.translatable("bee.dialogue.sad"), true);
        } else {
            ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                    new ChangeDataParameterPacket(beeEntity.getUUID(), "master", player.getStringUUID()));
            player.displayClientMessage(Component.translatable("bee.dialogue.excited"), true);
        }
        isFollowing = !isFollowing;
        onClose();
    }

    private void onGoHomeButton() {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new SetNpcHomePacket(beeEntity.getUUID(), null));
        onClose();
    }

    private void onSetHomeButton() {
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new SetNpcHomePacket(beeEntity.getUUID(), new Vec3(beeEntity.getX(), beeEntity.getY(), beeEntity.getZ())));
        onClose();
        player.displayClientMessage(Component.translatable("bee.dialogue.home_set"), true);
    }
}