package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.registry.ClothingSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClothingScrollWidget extends ObjectSelectionList<ClothingScrollWidget.ClothingEntry> {

    static final int TEXT_COLOR = 0x3A120F;
    static final List<ClothingSlot> SLOT_ORDER = Arrays.asList(ClothingSlot.values());
    static final String PADDING_STRING = "MMMMMMMMMM";

    private final NpcCustomizeScreen screen;
    private boolean scrollToTop = false;

    public ClothingScrollWidget(Minecraft mc, NpcCustomizeScreen screen) {
        // Constructor: mc, ancho, alto, y0 (top), y1 (bottom), itemHeight
        super(mc, screen.width / 2, screen.height, 32, screen.height - 32, 30);
        this.screen = screen;
        updateEntries();
    }

    public void updateEntries() {
        this.clearEntries();
        if (screen.getSelectedNpc() == null) return;

        int customBoneCount = 0;
        for (Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>> entry : screen.getClothingData().entrySet()) {
            ClothingSlot slot = entry.getKey();
            List<String> names = entry.getValue().getKey();
            int index = entry.getValue().getValue();
            this.addEntry(new ClothingEntry(slot, names, index));
            if (slot == ClothingSlot.CUSTOM_BONE) customBoneCount++;
        }

        // Ordenar por el enum
        this.children().sort(Comparator.comparingInt(e -> e.slot != null ? SLOT_ORDER.indexOf(e.slot) : 999));

        // Footer para huesos custom
        this.addEntry(new ClothingEntry(customBoneCount > 0));

        if (scrollToTop) {
            this.setScrollAmount(0);
            scrollToTop = false;
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    // ── Entrada de la Lista ──────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public class ClothingEntry extends ObjectSelectionList.Entry<ClothingEntry> {
        private final ClothingSlot slot;
        private final List<String> names;
        private int selectedIndex;
        private final boolean isFooter;
        private final boolean canRemove;

        public ClothingEntry(ClothingSlot slot, List<String> names, int selectedIndex) {
            this.slot = slot;
            this.names = names;
            this.selectedIndex = selectedIndex;
            this.isFooter = false;
            this.canRemove = false;
        }

        public ClothingEntry(boolean canRemove) {
            this.isFooter = true;
            this.canRemove = canRemove;
            this.slot = null;
            this.names = null;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            if (isFooter) {
                renderFooter(graphics, left, top, mouseX, mouseY);
            } else if (slot == ClothingSlot.GIRL_SPECIFIC) {
                renderGirlSpecificRow(graphics, left, top, mouseX, mouseY);
            } else {
                renderStandardRow(graphics, left, top, mouseX, mouseY);
            }
        }

        private void renderStandardRow(GuiGraphics graphics, int left, int top, int mx, int my) {
            // Fondo del slot (U=0, V=119 o 256 dependiendo de si hay algo seleccionado)
            int bgV = (selectedIndex == 0) ? 119 : 256;
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left, top, 0, bgV, 60, 30, 256, 256);

            // Icono del Slot
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left + 5, top + 5, slot.iconXPos, 0, 20, 20, 256, 256);

            // Flechas
            int arrowX = left + 30;
            renderArrows(graphics, arrowX, top + 5, mx, my);

            // Texto descriptivo (usando Font del sistema)
            String name = getCurrentName();
            if (name.length() > 10) name = name.substring(0, 7) + "...";
            graphics.drawString(Minecraft.getInstance().font, name, left + 80, top + 10, TEXT_COLOR, false);
        }

        private void renderGirlSpecificRow(GuiGraphics graphics, int left, int top, int mx, int my) {
            boolean locked = screen.isGirlSpecificLocked(selectedIndex);
            int bgV = locked ? 60 : 90;
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left, top, 0, bgV, 119, 30, 256, 256);

            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left + 5, top + 5, slot.iconXPos, 0, 20, 20, 256, 256);

            if (locked) {
                renderSlider(graphics, left + 30, top + 5, mx, my, selectedIndex);
            } else {
                renderArrows(graphics, left + 30, top + 5, mx, my);
            }
        }

        private void renderFooter(GuiGraphics graphics, int left, int top, int mx, int my) {
            // Botón "Añadir"
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left + 10, top + 5, isMouseOver(mx, my, left + 10, top + 5, 20, 20) ? 40 : 20, 20, 20, 20, 256, 256);

            if (canRemove) {
                // Botón "Quitar"
                graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, left + 40, top + 5, isMouseOver(mx, my, left + 40, top + 5, 20, 20) ? 40 : 20, 20, 20, 20, 256, 256);
            }
        }

        private void renderArrows(GuiGraphics graphics, int x, int y, int mx, int my) {
            // Prev
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, x, y, isMouseOver(mx, my, x, y, 20, 20) ? 20 : 0, 20, 20, 20, 256, 256);
            // Next
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, x + 25, y, isMouseOver(mx, my, x + 25, y, 20, 20) ? 60 : 40, 20, 20, 20, 256, 256);
        }

        private void renderSlider(GuiGraphics graphics, int x, int y, int mx, int my, int id) {
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, x, y, 140, 20, 79, 20, 256, 256);
            float val = screen.getSliderValue(id) / 100.0f;
            int kx = x + 4 + (int)(val * 67);
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, kx, y, isMouseOver(mx, my, kx, y, 4, 20) ? 223 : 219, 20, 4, 20, 256, 256);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;

            // Aquí puedes calcular la lógica de clic relativa a 'left' y 'top'
            // Ejemplo para las flechas en un slot estándar:
            // if (mouseX >= left + 30 && mouseX <= left + 50) cycle...

            return true;
        }

        private boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        private String getCurrentName() {
            return (names != null && !names.isEmpty()) ? names.get(Math.min(selectedIndex, names.size() - 1)) : "";
        }
    }
}