package com.trolmastercard.sexmod.client.gui;
import com.trolmastercard.sexmod.ClothingSlot;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * ClothingScrollWidget - scrollable list widget for NpcCustomizeScreen clothing slots.
 * Ported from gq.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original obfuscation:
 *   gq            - ClothingScrollWidget (extends GuiListExtended)
 *   gq.a (class)  - ClothingScrollWidget.Entry (implements IGuiListEntry)
 *   gw            - ClothingSlot (enum)
 *   gq.c (field)  - TEXT_COLOR = 0x3A120F (dark brownish)
 *   gq.f (field)  - SLOT_ORDER = Arrays.asList(ClothingSlot.values())
 *   gq.a (field)  - PADDING_STRING = "MMMMMMMMMM" (used for width measurement)
 *   gq.i (field)  - ICON_X (static, tracks running X for icons)
 *   gq.e (field)  - listWidth (half screen width)
 *
 * Migration notes:
 *   GuiListExtended        - ObjectSelectionList<E>
 *   IGuiListEntry          - ObjectSelectionList.Entry<E>
 *   func_148128_a(x,y,pt)  - render(GuiGraphics, mouseX, mouseY, partialTick)
 *   func_148179_a(x,y,b)   - mouseClicked(double,double,int)
 *   func_192634_a(...)      - Entry.render(GuiGraphics,...)
 *   Gui.func_73729_b        - GuiGraphics.blit(...)
 *   FontRenderer            - Font via Minecraft.getInstance().font
 *   Mouse.getEventDWheel()  - use scrolling via AbstractSelectionList.mouseScrolled
 */
@OnlyIn(Dist.CLIENT)
public class ClothingScrollWidget extends ObjectSelectionList<ClothingScrollWidget.ClothingEntry> {

    static final int   TEXT_COLOR     = 0x3A120F;
    static final List<ClothingSlot> SLOT_ORDER = Arrays.asList(ClothingSlot.values());
    static final String PADDING_STRING = "MMMMMMMMMM";

    protected static int iconX    = 5;
    protected static int listWidth = 200;

    /** The NpcCustomizeScreen that owns this widget. */
    private final NpcCustomizeScreen screen;
    /** Whether the list should scroll to top on next render. */
    boolean scrollToTop = false;

    public ClothingScrollWidget(Minecraft mc, NpcCustomizeScreen screen) {
        super(mc, screen.width / 2, screen.height, 0, screen.height, 30);
        listWidth       = screen.width / 2;
        this.screen     = screen;
        updateEntries();
    }

    // -- Entry population ------------------------------------------------------

    public void updateEntries() {
        clearEntries();
        if (screen.getSelectedNpc() == null) return;

        int customBoneCount = 0;
        for (Map.Entry<ClothingSlot, Map.Entry<List<String>, Integer>> entry
                : screen.getClothingData().entrySet()) {
            ClothingSlot slot   = entry.getKey();
            List<String> names  = entry.getValue().getKey();
            int          index  = entry.getValue().getValue();
            addEntry(new ClothingEntry(slot, names, index));
            if (slot == ClothingSlot.CUSTOM_BONE) customBoneCount++;
        }

        // Sort by slot order
        children().sort(Comparator.comparingInt(e -> SLOT_ORDER.indexOf(e.slot)));

        // Add "add/remove custom bone" footer entry
        addEntry(new ClothingEntry(customBoneCount > 1));

        // Centre vertically if list fits on screen
        centreIfFits();

        if (scrollToTop) {
            setScrollAmount(999_999);
            scrollToTop = false;
        }
    }

    private void centreIfFits() {
        int totalH = children().size() * itemHeight;
        if (totalH <= height) {
            setY0(0);
        }
    }

    // -- Rendering -------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected int getScrollbarPosition() {
        return listWidth - 6;
    }

    @Override
    public int getRowWidth() { return listWidth - 20; }

    @Override
    protected boolean isFocused() { return screen.getFocused() == this; }

    // -- Mouse -----------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        handleRowClick((int)mouseX, (int)mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleRowClick(int mx, int my, int button) {
        if (mx > listWidth) return;
        int scrolledY = my + (int)getScrollAmount() - 5;
        int row       = Math.floorDiv(scrolledY, itemHeight);
        int rowOffset = Math.round((scrolledY % itemHeight) / (float)itemHeight * itemHeight);
        if (row < 0 || row >= children().size()) return;
        children().get(row).onClickLocal(mx, rowOffset, button, row);
    }

    // -- Inner entry -----------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public class ClothingEntry extends Entry<ClothingEntry> {

        public ClothingSlot   slot;
        public List<String>   names;
        public int            selectedIndex;

        /** True if this is the footer "add/remove custom bone" entry. */
        private final boolean isFooter;
        /** Only meaningful for footer: show "remove" button if there are multiple bones. */
        private final boolean canRemove;

        ClothingEntry(ClothingSlot slot, List<String> names, int selectedIndex) {
            this.slot          = slot;
            this.names         = names;
            this.selectedIndex = selectedIndex;
            this.isFooter      = false;
            this.canRemove     = false;
        }

        ClothingEntry(boolean canRemove) {
            this.isFooter  = true;
            this.canRemove = canRemove;
            this.slot      = null;
            this.names     = null;
            this.selectedIndex = 0;
        }

        // -- Rendering ---------------------------------------------------------

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isHovered, float partialTick) {
            if (isFooter) {
                renderFooter(graphics, left, top, mouseX, mouseY);
            } else if (slot == ClothingSlot.GIRL_SPECIFIC) {
                renderGirlSpecificRow(graphics, left, top, mouseX, mouseY);
            } else {
                renderStandardRow(graphics, left, top, mouseX, mouseY);
            }
        }

        private void renderStandardRow(GuiGraphics graphics, int x, int y, int mx, int my) {
            // Background icon for slot
            int bgU = (selectedIndex == 0) ? 119 : 256;
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                    iconX, y, 0, bgU, 60, 30);

            int curX = iconX + 10;
            x += 5;

            // Slot icon
            renderSlotIcon(graphics, curX, x, slot.iconXPos);
            curX += 25;

            // Prev/next arrows
            curX = renderArrows(graphics, curX, x, mx, my);

            // Name label
            String name = getCurrentName();
            if (name.length() > PADDING_STRING.length()) {
                name = name.substring(0, PADDING_STRING.length() - 3) + "...";
            }
            renderText(graphics, name, curX, x + 10);
            curX += Minecraft.getInstance().font.width(PADDING_STRING);

            // Modifier label (e.g. percentage or custom value)
            String modifier = getModifierLabel();
            if (modifier.length() > PADDING_STRING.length()) {
                modifier = modifier.substring(0, PADDING_STRING.length() - 3) + "...";
            }
            renderText(graphics, modifier, curX, x + 10);
        }

        private void renderGirlSpecificRow(GuiGraphics graphics, int x, int y, int mx, int my) {
            // Girl-specific slot uses a different layout with a slider
            boolean locked = screen.isGirlSpecificLocked(selectedIndex);
            if (locked) {
                graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                        iconX, y, 0, 60, 119, 30);
            } else {
                graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                        iconX, y, 0, 90, 95, 30);
            }
            int curX = iconX + 10;
            x += 5;
            renderSlotIcon(graphics, curX, x, slot.iconXPos);
            curX += 25;
            if (locked) {
                renderSlider(graphics, curX, x, mx, my, selectedIndex);
            } else {
                renderArrows(graphics, curX, x, mx, my);
            }
        }

        private void renderFooter(GuiGraphics graphics, int x, int y, int mx, int my) {
            int bx = 30;
            // "Add" button
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                    bx, y, isHovering(mx, my, bx, y, bx+20, y+20) ? 40 : 20, 20, 20);
            bx += 40;
            // "Remove" button (only if canRemove)
            if (canRemove) {
                graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                        bx, y, isHovering(mx, my, bx, y, bx+20, y+20) ? 40 : 20, 20, 20);
            }
        }

        // -- Helpers -----------------------------------------------------------

        private void renderSlotIcon(GuiGraphics graphics, int gx, int gy, int iconXPos) {
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                    gx, gy, iconXPos, 0, 20, 20);
        }

        private int renderArrows(GuiGraphics graphics, int ax, int ay, int mx, int my) {
            // Prev arrow (U=0, active=20)
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                    ax, ay, isHovering(mx, my, ax, ay, ax+20, ay+20) ? 20 : 0, 20, 20);
            ax += 20;
            // Next arrow (U=20, active=40)
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX,
                    ax, ay, isHovering(mx, my, ax, ay, ax+20, ay+20) ? 40 : 20, 20, 20);
            return ax + 40;
        }

        private void renderSlider(GuiGraphics graphics, int sx, int sy, int mx, int my, int slotIndex) {
            // Draw slider track (u=140, w=79, h=20)
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, sx, sy, 140, 20, 79, 20);
            sx += 4;
            int left = sx, right = sx + 71 - 4;
            float value = getSliderValue(sy, left, right, mx, my, slotIndex);
            int kx = (int) Mth.lerp(value, left, right);
            int kU = isHovering(mx, my, kx, sy, kx+4, sy+20) ? 223 : 219;
            graphics.blit(NpcCustomizeScreen.CUSTOMIZE_TEX, kx, sy, kU, 20, 4, 20);
            // Notify screen of current slider value
            screen.onSliderChanged(slotIndex, (int)(value * 100.0f));
        }

        private float getSliderValue(int trackY, int left, int right, int mx, int my, int slotIndex) {
            if (!screen.isDragging()) return screen.getSliderValue(slotIndex) / 100.0f;
            if (my > screen.width / 3.0f) return screen.getSliderValue(slotIndex) / 100.0f;
            if (my < trackY || my > trackY + 20) return screen.getSliderValue(slotIndex) / 100.0f;
            if (mx < left)  return 0.0f;
            if (mx > right) return 1.0f;
            return (float)(mx - left) / (right - left);
        }

        private void renderText(GuiGraphics graphics, String text, int x, int y) {
            graphics.drawString(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
            RenderSystem.setShaderColor(1,1,1,1);
        }

        private String getCurrentName() {
            if (names == null || names.isEmpty()) return "";
            int idx = Math.max(0, Math.min(selectedIndex, names.size() - 1));
            return names.get(idx);
        }

        private String getModifierLabel() {
            return screen.getModifierLabel(slot, selectedIndex);
        }

        private boolean isHovering(int mx, int my, int x1, int y1, int x2, int y2) {
            return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        }

        // -- Click -------------------------------------------------------------

        void onClickLocal(int mx, int rowY, int button, int rowIndex) {
            if (button != 0) return;
            if (rowY < 5 || rowY > 25) return;

            if (isFooter) {
                handleFooterClick(mx, rowY);
                return;
            }
            if (slot == ClothingSlot.GIRL_SPECIFIC) {
                handleGirlSpecificClick(mx, selectedIndex);
            } else {
                handleArrowClick(mx, selectedIndex);
            }
        }

        private void handleFooterClick(int mx, int ry) {
            int bx = 30;
            if (mx > bx && mx < bx + 20) {
                scrollToTop = true;
                screen.onAddCustomBone();
            }
            if (!canRemove) return;
            bx += 40;
            if (mx > bx && mx < bx + 20) {
                screen.onRemoveCustomBone();
            }
        }

        private void handleArrowClick(int mx, int slotIdx) {
            if (mx > 40 && mx < 60)  screen.cycleClothingSlot(slot, false, slotIdx);
            if (mx > 60 && mx < 80)  screen.cycleClothingSlot(slot, true,  slotIdx);
        }

        private void handleGirlSpecificClick(int mx, int slotIdx) {
            if (!screen.isGirlSpecificLocked(slotIdx)) handleArrowClick(mx, slotIdx);
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) { return false; }
    }
}
