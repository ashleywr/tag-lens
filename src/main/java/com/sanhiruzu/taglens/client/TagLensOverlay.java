package com.sanhiruzu.taglens.client;

import com.sanhiruzu.taglens.TagLens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TagLens.MOD_ID, value = Dist.CLIENT)
public final class TagLensOverlay {
    private static final int PANEL_WIDTH = 194;
    private static final int PANEL_MARGIN = 6;
    private static final int PANEL_PADDING = 7;
    private static final int HEADER_HEIGHT = 29;
    private static final int ROW_HEIGHT = 12;
    private static final int FOOTER_HEIGHT = 17;
    private static final int BACKGROUND = 0xE6101216;
    private static final int BORDER = 0xFF4B5563;
    private static final int ROW_HOVER = 0xFF34445A;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFF9CA3AF;
    private static final int TAG_COLOR = 0xFFE5E7EB;
    private static final int SHARED_TAG_COLOR = 0xFF86EFAC;

    private static Screen activeScreen;
    private static int scrollOffset;
    private static PanelState lastPanel;
    private static ResourceLocation hoveredTag;

    private TagLensOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRender(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> containerScreen = event.getContainerScreen();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastPanel = null;
            return;
        }

        if (activeScreen != containerScreen) {
            activeScreen = containerScreen;
            scrollOffset = 0;
        }

        InventoryTagAnalyzer.Analysis analysis = InventoryTagAnalyzer.analyze(
                containerScreen, minecraft.player.getInventory());
        PanelLayout layout = layout(containerScreen, analysis.tags().size());
        int maxOffset = Math.max(0, analysis.tags().size() - layout.visibleRows());
        scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(-containerScreen.getGuiLeft(), -containerScreen.getGuiTop(), 0);
        renderPanel(graphics, minecraft.font, analysis, layout, event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderTooltip(ScreenEvent.Render.Post event) {
        if (event.getScreen() == activeScreen && hoveredTag != null) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                    Component.literal(hoveredTag.toString()), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || lastPanel == null
                || event.getScreen() != activeScreen) {
            return;
        }

        RowState clicked = lastPanel.rowAt(event.getMouseX(), event.getMouseY());
        if (clicked == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String tag = clicked.tag().toString();
        minecraft.keyboardHandler.setClipboard(tag);
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("taglens.overlay.copied", tag), true);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (lastPanel == null || event.getScreen() != activeScreen
                || !lastPanel.contains(event.getMouseX(), event.getMouseY())
                || lastPanel.totalRows() <= lastPanel.visibleRows()) {
            return;
        }

        int direction = (int) Math.signum(event.getScrollDeltaY());
        if (direction != 0) {
            int maxOffset = lastPanel.totalRows() - lastPanel.visibleRows();
            scrollOffset = Math.clamp(scrollOffset - direction, 0, maxOffset);
            event.setCanceled(true);
        }
    }

    private static PanelLayout layout(AbstractContainerScreen<?> screen, int tagCount) {
        int guiLeft = screen.getGuiLeft();
        int guiRight = guiLeft + screen.getXSize();
        int roomOnRight = screen.width - guiRight - PANEL_MARGIN * 2;
        int roomOnLeft = guiLeft - PANEL_MARGIN * 2;
        int sideRoom = Math.max(roomOnLeft, roomOnRight);
        int width = Math.min(PANEL_WIDTH, Math.max(1, sideRoom));
        int x;
        if (roomOnRight >= roomOnLeft && roomOnRight > 0) {
            x = guiRight + PANEL_MARGIN;
        } else if (roomOnLeft > 0) {
            x = guiLeft - width - PANEL_MARGIN;
        } else {
            width = Math.min(PANEL_WIDTH, screen.width - PANEL_MARGIN * 2);
            x = Math.max(PANEL_MARGIN, screen.width - width - PANEL_MARGIN);
        }

        int y = Math.max(PANEL_MARGIN, screen.getGuiTop());
        int availableHeight = Math.max(HEADER_HEIGHT + FOOTER_HEIGHT + ROW_HEIGHT,
                screen.height - y - PANEL_MARGIN);
        int visibleRows = Math.max(1,
                (availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
        visibleRows = Math.min(Math.max(1, tagCount), visibleRows);
        int height = HEADER_HEIGHT + visibleRows * ROW_HEIGHT + FOOTER_HEIGHT;
        return new PanelLayout(x, y, width, height, visibleRows);
    }

    private static void renderPanel(GuiGraphics graphics, Font font,
                                    InventoryTagAnalyzer.Analysis analysis,
                                    PanelLayout layout, int mouseX, int mouseY) {
        int x = layout.x();
        int y = layout.y();
        int right = x + layout.width();
        int bottom = y + layout.height();
        hoveredTag = null;
        graphics.fill(x, y, right, bottom, BACKGROUND);
        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);

        graphics.drawString(font, Component.translatable("taglens.overlay.title"),
                x + PANEL_PADDING, y + 6, TITLE_COLOR, false);
        graphics.drawString(font,
                Component.translatable("taglens.overlay.occupied", analysis.occupiedSlots()),
                x + PANEL_PADDING, y + 17, MUTED_COLOR, false);

        List<RowState> rows = new ArrayList<>();
        if (analysis.occupiedSlots() == 0) {
            drawCenteredMessage(graphics, font, "taglens.overlay.no_items", layout);
        } else if (analysis.tags().isEmpty()) {
            drawCenteredMessage(graphics, font, "taglens.overlay.no_tags", layout);
        } else {
            int end = Math.min(analysis.tags().size(), scrollOffset + layout.visibleRows());
            for (int index = scrollOffset; index < end; index++) {
                InventoryTagAnalyzer.TagCount tag = analysis.tags().get(index);
                int visibleIndex = index - scrollOffset;
                int rowY = y + HEADER_HEIGHT + visibleIndex * ROW_HEIGHT;
                boolean hovered = mouseX >= x + 2 && mouseX < right - 2
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (hovered) {
                    graphics.fill(x + 2, rowY, right - 2, rowY + ROW_HEIGHT, ROW_HOVER);
                    hoveredTag = tag.id();
                }

                String count = tag.matches() + "/" + analysis.occupiedSlots();
                int countWidth = font.width(count);
                int tagWidth = layout.width() - PANEL_PADDING * 2 - countWidth - 6;
                String tagText = font.plainSubstrByWidth(tag.id().toString(), tagWidth);
                int color = tag.matches() == analysis.occupiedSlots()
                        ? SHARED_TAG_COLOR : TAG_COLOR;
                graphics.drawString(font, tagText, x + PANEL_PADDING, rowY + 2, color, false);
                graphics.drawString(font, count, right - PANEL_PADDING - countWidth,
                        rowY + 2, MUTED_COLOR, false);
                rows.add(new RowState(x + 2, rowY, right - 2, rowY + ROW_HEIGHT, tag.id()));
            }
        }

        int footerY = bottom - FOOTER_HEIGHT + 4;
        Component footer;
        if (analysis.tags().size() > layout.visibleRows()) {
            int first = scrollOffset + 1;
            int last = Math.min(analysis.tags().size(), scrollOffset + layout.visibleRows());
            footer = Component.translatable("taglens.overlay.range", first, last, analysis.tags().size());
        } else {
            footer = Component.translatable("taglens.overlay.copy_hint");
        }
        graphics.drawCenteredString(font, footer, x + layout.width() / 2, footerY, MUTED_COLOR);

        lastPanel = new PanelState(x, y, right, bottom, layout.visibleRows(),
                analysis.tags().size(), List.copyOf(rows));
    }

    private static void drawCenteredMessage(GuiGraphics graphics, Font font, String key,
                                            PanelLayout layout) {
        graphics.drawCenteredString(font, Component.translatable(key),
                layout.x() + layout.width() / 2,
                layout.y() + HEADER_HEIGHT + 2, MUTED_COLOR);
    }

    private record PanelLayout(int x, int y, int width, int height, int visibleRows) {
    }

    private record RowState(int left, int top, int right, int bottom, ResourceLocation tag) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        }
    }

    private record PanelState(int left, int top, int right, int bottom,
                              int visibleRows, int totalRows, List<RowState> rows) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        }

        RowState rowAt(double mouseX, double mouseY) {
            for (RowState row : rows) {
                if (row.contains(mouseX, mouseY)) {
                    return row;
                }
            }
            return null;
        }
    }
}
