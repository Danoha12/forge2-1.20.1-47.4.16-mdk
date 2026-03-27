package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcInventoryBase;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.network.datasync.EntityDataAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * NpcActionScreen - ported from m.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A floating side-panel action menu that appears when the player interacts with
 * an NPC. It shows up to 5 standard action buttons (follow, stop-follow, go-home,
 * set-home, equipment) plus any custom shop/quest buttons passed by the NPC.
 *
 * Custom buttons (index - 5) may have an associated {@link ItemStack} cost.
 * If the player doesn't have enough items, the action is rejected with a
 * disapproval voice line.
 *
 * The panel also renders the NPC's current equipment slots (from SynchedEntityData)
 * using the ItemRenderer at fixed positions.
 *
 * Animation: two floats ({@code fadeIn}, {@code panelFade}) slide from 0-1 on
 * open. {@code fadeIn} drives button x-offset (sliding in from off-screen).
 * {@code panelFade} drives the equipment panel y-offset.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - GuiScreen - Screen
 *   - GuiButton / func_146284_a - Button with onPress lambda
 *   - func_73863_a(int,int,float) - render(PoseStack,int,int,float)
 *   - func_73868_f() - isPauseScreen()
 *   - func_146281_b() - onClose()
 *   - ScaledResolution - Minecraft.getInstance().getWindow()
 *   - func_193989_ak() - getDeltaFrameTime()
 *   - GlStateManager - RenderSystem
 *   - RenderItem.func_175042_a - itemRenderer.renderAndDecorateFakeItem
 *   - func_135052_a(key, args) - I18n.get(key)
 *   - func_73729_b(x,y,u,v,w,h) - blit(ps,x,y,u,v,w,h)
 *   - EntityDataManager.func_187225_a - entity.getEntityData().get(key)
 *   - em.ac() - npc.stopAction() / cancel interaction
 *   - ge.b.sendToServer(new t(...)) - ModNetwork.CHANNEL.sendToServer(new RemoveItemsPacket(...))
 *   - e2.L/R/X/T/U/W - NpcEquipmentSlot data accessors (via NpcInventoryBase)
 *   - field_75098_d - player.getAbilities().instabuild (creative mode)
 *   - func_70005_c_() - npc.getName().getString()
 *   - g.a(str, playerId) - npc.triggerAction(str, playerId)
 *   - func_71053_j() - player.closeContainer() / mc.player.closeContainer()
 *   - a(List,int,int,FontRenderer) - renderTooltip (built-in in 1.20.1)
 *   - b6.a(from,to,t) - MathUtil.lerp(from, to, t)
 *   - func_175174_a - blit (adjusted)
 *   - field_146296_j.func_175042_a - itemRenderer.renderAndDecorateFakeItem
 */
@OnlyIn(Dist.CLIENT)
public class NpcActionScreen extends Screen {

    // -- Texture ----------------------------------------------------------------
    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/girlinventory.png");

    // -- NPC context ------------------------------------------------------------
    final BaseNpcEntity npc;
    final Player player;

    /** Labels for custom buttons (index 0 = button 5). */
    final String[] customLabels;

    /** Optional item cost for each custom button (parallel to customLabels). */
    @Nullable
    final ItemStack[] customCosts;

    /** Whether to also render the NPC equipment panel. */
    final boolean showEquipment;

    // -- Animation --------------------------------------------------------------
    /** Overall fade-in (0-1). Drives button x slide. */
    float fadeIn = 0.0F;
    /** Equipment-panel fade (0-1). Starts after fadeIn reaches 1. */
    float panelFade = 0.0F;

    // -- Localisation keys for the 5 base buttons -------------------------------
    String[] baseActions = new String[]{
            "action.names.followme",
            "action.names.stopfollowme",
            "action.names.gohome",
            "action.names.setnewhome",
            "action.names.equipment"
    };

    /**
     * Per-button animation offset accumulator.
     * 0 = topLeft (bed/chest), 1 = bottomLeft (follow), 2 = topRight, 3 = bottomRight.
     * Indices 0-4 cover the 5 base buttons; the rest cover custom buttons.
     */
    int[] buttonOffset = new int[]{ 0, 0, 0, 0, 0 };

    /** Texture V-offset in TEXTURE atlas for each button type icon. */
    int[] iconV = new int[]{ 64, 80, 47, 32, 96 };

    /** Space-padding count for button label (visual centering). */
    int[] labelPad = new int[]{ 4, 4, 5, 5, 4 };

    /** Maximum button offset value per type. */
    int[] maxOffset = new int[]{ 50, 90, 50, 80, 60 };

    // -- Constructors -----------------------------------------------------------

    /** Simple constructor - no custom buttons, no costs. */
    public NpcActionScreen(BaseNpcEntity npc, Player player) {
        super(Component.empty());
        this.npc           = npc;
        this.player        = player;
        this.customLabels  = new String[0];
        this.customCosts   = new ItemStack[0];
        this.showEquipment = true;
    }

    /**
     * Full constructor.
     *
     * @param customLabels    I18n keys for custom buttons (shown after base 5)
     * @param customCosts     Item costs parallel to customLabels (may be null entries)
     * @param showEquipment   Whether to render the NPC's equipment slots
     */
    public NpcActionScreen(BaseNpcEntity npc, Player player,
                           String[] customLabels,
                           @Nullable ItemStack[] customCosts,
                           boolean showEquipment) {
        super(Component.empty());
        this.npc           = npc;
        this.player        = player;
        this.customLabels  = customLabels;
        this.customCosts   = customCosts;
        this.showEquipment = showEquipment;
    }

    // -- Screen lifecycle -------------------------------------------------------

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        super.onClose();
        npc.cancelInteraction();
    }

    // -- Render -----------------------------------------------------------------

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        // Clear dynamic button list each frame (re-built below)
        clearWidgets();

        Minecraft mc = Minecraft.getInstance();
        int screenW = width;
        int screenH = height;

        // -- Advance fade ------------------------------------------------------
        fadeIn = Math.min(1.0F, fadeIn + mc.getDeltaFrameTime() / 5.0F);
        if (fadeIn >= 1.0F) {
            panelFade = Math.min(1.0F, panelFade + mc.getDeltaFrameTime() / 5.0F);
        }

        // Lerp-derived screen positions
        int btnX    = (int) MathUtil.lerp(-30.0F, 120.0F, fadeIn);
        int itemX   = (int) MathUtil.lerp( 91.0F, 137.0F, panelFade);
        int equipX  = (int) MathUtil.lerp(-30.0F, 120.0F, fadeIn);

        // Base layout constants
        int baseY  = 70;   // pixel from bottom of screen
        int iconW  = 16;
        int iconH  = 16;
        int btnH   = 20;
        int btnW   = 100;

        int currentY = screenH - baseY;
        int currentIconY = currentY + 2;

        int totalButtons = 5 + customLabels.length;

        // Render GL blend
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        for (int idx = 5; idx < totalButtons; idx++) {
            int customIdx = idx - 5;

            // Cost item rendering
            if (panelFade > 0.0F &&
                customCosts != null &&
                customIdx < customCosts.length &&
                customCosts[customIdx] != null &&
                !customCosts[customIdx].isEmpty()) {

                // Amount label
                String amtLabel = customCosts[customIdx].getCount() + "x    ";
                renderTooltip(ps, List.of(Component.literal(amtLabel)),
                        screenW - itemX, screenH - 52);

                // Item icon
                mc.getItemRenderer().renderAndDecorateFakeItem(
                        customCosts[customIdx],
                        screenW - (int) MathUtil.lerp(91.0F, 137.0F, panelFade),
                        screenH - 68);
            }

            // Determine button index for offset tracking (0-4 for types, reuse 0 for extras)
            int typeIdx = Math.min(idx, 4);

            // Button
            int  finalIdx   = idx;
            int  finalTypeIdx = typeIdx;
            addRenderableWidget(Button.builder(
                Component.translatable(customLabels[customIdx]),
                btn -> onActionButton(finalIdx))
                .pos(screenW - btnX - buttonOffset[typeIdx], currentY)
                .size(btnW, btnH)
                .build());

            // Draw icon
            RenderSystem.setShaderTexture(0, TEXTURE);
            blit(ps,
                    screenW - btnX - buttonOffset[typeIdx] - 18,
                    currentIconY + (int) MathUtil.lerp(0.0F, 23.0F, panelFade) + buttonOffset[typeIdx],
                    iconV[typeIdx], 0, iconW, iconH);

            currentY    += 30;
            currentIconY += 30;
        }

        // -- 5 base action buttons ---------------------------------------------
        boolean hasOwner = !npc.getEntityData().get(BaseNpcEntity.OWNER_UUID_STRING).equals("");
        int typeBase = hasOwner ? 0 : 2; // shifts which base style index to use for first btn

        currentY    = screenH - baseY;

        for (int b = 0; b < 5; b++) {
            int typeIdx = b;
            // If has owner: first button shifts by 1 (owner-aware layout)
            if (b == 0 && hasOwner) typeIdx = 1;
            else if (b == 1 && !hasOwner) typeIdx = 2;

            // Clamp typeIdx
            typeIdx = Math.min(typeIdx, buttonOffset.length - 1);

            // Hover detection - accumulate offset
            boolean hovered = mouseX >= screenW - btnX - buttonOffset[typeIdx] &&
                              mouseX <= screenW - btnX - buttonOffset[typeIdx] + 23 + buttonOffset[typeIdx] &&
                              mouseY >= currentY &&
                              mouseY <= currentY + btnH;

            if (hovered) {
                buttonOffset[typeIdx] = Math.min(maxOffset[typeIdx], buttonOffset[typeIdx] + 7);
            } else {
                buttonOffset[typeIdx] = Math.max(0, buttonOffset[typeIdx] - 7);
            }

            // Build padded label
            StringBuilder label = new StringBuilder(
                    net.minecraft.client.resources.language.I18n.get(baseActions[b]));
            for (int p = 0; p < labelPad[Math.min(b, labelPad.length - 1)]; p++) label.append(' ');

            // Don't show text when offset < 14
            String displayLabel = buttonOffset[typeIdx] > 14 ? label.toString() : "";
            int finalB = b;
            addRenderableWidget(Button.builder(
                Component.literal(displayLabel),
                btn -> onActionButton(finalB))
                .pos(screenW - btnX - buttonOffset[typeIdx] + 1,
                     currentY + (int) MathUtil.lerp(0.0F, 23.0F, panelFade) + buttonOffset[typeIdx])
                .size(btnW, btnH)
                .build());

            // Icon
            RenderSystem.setShaderTexture(0, TEXTURE);
            blit(ps,
                    screenW - btnX - buttonOffset[typeIdx] - 18 + buttonOffset[typeIdx],
                    currentY + (int) MathUtil.lerp(0.0F, 23.0F, panelFade) + buttonOffset[typeIdx] + 2,
                    iconV[Math.min(b, iconV.length - 1)], 0, iconW, iconH);

            currentY += 30;
        }

        // -- Equipment panel ---------------------------------------------------
        if (showEquipment) {
            renderEquipment(ps, screenW, screenH, btnX);
        }

        super.render(ps, mouseX, mouseY, partialTick);
        RenderSystem.disableBlend();
    }

    /**
     * Draws the NPC's 6 equipment item icons (head, chest, legs, feet, mainhand, offhand).
     * Positions match original layout: column to the right of the buttons.
     */
    private void renderEquipment(PoseStack ps, int screenW, int screenH, int btnX) {
        if (panelFade == 0.0F) return;

        int equipX = (int) MathUtil.lerp(-30.0F, 120.0F, fadeIn) - 105;
        int baseY  = screenH - 60;

        // Slot layout: matches e2.L / R / X / T / U / W (head, chest, legs, feet, main, off)
        EntityDataAccessor<?>[] slots = NpcInventoryBase.getEquipmentSlotKeys(npc);
        int[] yOffsets = { 68, 87, 109, 127, 146, 166 };

        for (int s = 0; s < Math.min(slots.length, yOffsets.length); s++) {
            try {
                @SuppressWarnings("unchecked")
                ItemStack stack = (ItemStack) npc.getEntityData().get(
                        (EntityDataAccessor<ItemStack>) slots[s]);
                if (stack != null && !stack.isEmpty()) {
                    Minecraft.getInstance().getItemRenderer()
                             .renderAndDecorateFakeItem(stack, screenW - equipX, baseY - yOffsets[s]);
                }
            } catch (Exception ignored) {}
        }

        // Draw equipment panel background
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(ps, screenW - equipX - 113, baseY - 60, 0, 0, 32, 130);
    }

    // -- Button handler ---------------------------------------------------------

    private void onActionButton(int idx) {
        // Custom buttons (idx - 5): check item cost
        if (idx >= 5) {
            int customIdx = idx - 5;
            if (customCosts != null && customIdx < customCosts.length &&
                customCosts[customIdx] != null && !customCosts[customIdx].isEmpty()) {

                if (!player.getAbilities().instabuild) {
                    // Search player inventory for matching item
                    boolean found = false;
                    for (ItemStack inv : player.getInventory().items) {
                        if (inv.getItem().equals(customCosts[customIdx].getItem()) &&
                            inv.getCount() >= customCosts[customIdx].getCount() &&
                            inv.getDamageValue() == customCosts[customIdx].getDamageValue()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        player.sendSystemMessage(
                                Component.literal("<" + npc.getName().getString() +
                                                  "> you cannot afford that..."));
                        npc.triggerAction(ModSounds.GIRLS_JENNY_SADOH[1],
                                          player.getUUID());
                        return;
                    }
                }
                // Send cost removal to server
                ModNetwork.CHANNEL.sendToServer(
                        new RemoveItemsPacket(player.getUUID(), customCosts[customIdx]));
            }
        }

        commitAction(idx);
    }

    private void commitAction(int idx) {
        String actionKey = idx < 5 ? baseActions[idx] : customLabels[idx - 5];
        npc.triggerAction(actionKey, player.getUUID());
        Minecraft.getInstance().player.closeContainer();
    }
}
