/*
 * RTM LWJGL3ify Compat — compatibility patch mod for RealTrainMod on lwjgl3ify.
 * Copyright (C) 2026 325
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version. It is distributed WITHOUT ANY WARRANTY; see the GNU LGPL v3 for
 * details. You should have received a copy of the license (COPYING.LESSER and
 * COPYING) with this program.
 */

package jp.kaiz.compat.rtmlwjgl3ify.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import jp.kaiz.compat.rtmlwjgl3ify.patch.ModelPackProgressDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draws RTM's model-pack load progress as an in-game overlay.
 *
 * This is the visible replacement for RTM's Swing loading window, which cannot exist on macOS under
 * lwjgl3ify (GLFW owns the main thread, so AWT is forced headless). We render here, on the client
 * render thread, where the GL context and Angelica are fully initialized — RTM loads model packs on a
 * background thread that overlaps the title screen, so by the time anything is drawn the screen is up.
 *
 * Drawing only happens while a load is {@link ModelPackProgressDisplay#isActive() active}. We cover
 * both surfaces: {@link GuiScreenEvent.DrawScreenEvent.Post} (title screen / any open GUI) and
 * {@link RenderGameOverlayEvent.Post} (in-world HUD) — only one fires per frame, so there is no
 * double draw. Everything is wrapped so a render hiccup can never crash or spam.
 */
public final class ModelPackLoadingOverlay extends Gui {
    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompatOverlay");

    private static final int BAR_MAX_WIDTH = 200;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_BOTTOM_MARGIN = 24;

    private boolean warned;

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        render();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            render();
        }
    }

    private void render() {
        if (!ModelPackProgressDisplay.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) {
            return;
        }

        try {
            ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            int barWidth = Math.min(BAR_MAX_WIDTH, screenWidth - 40);
            if (barWidth <= 0) {
                return;
            }
            int x = (screenWidth - barWidth) / 2;
            int y = screenHeight - BAR_BOTTOM_MARGIN;

            // Backing plate behind the bar so it stays readable over any background.
            drawRect(x - 2, y - 2, x + barWidth + 2, y + BAR_HEIGHT + 2, 0xAA000000);

            float progress = ModelPackProgressDisplay.getProgress();
            drawRect(x, y, x + barWidth, y + BAR_HEIGHT, 0x44FFFFFF);
            if (progress < 0.0F) {
                // Total not known yet: show a faint full bar as an indeterminate state.
                drawRect(x, y, x + barWidth, y + BAR_HEIGHT, 0x55FFFFFF);
            } else {
                int filled = (int) (barWidth * progress);
                drawRect(x, y, x + filled, y + BAR_HEIGHT, 0xFF55FF55);
            }

            FontRenderer font = mc.fontRenderer;
            String label = buildLabel();
            int textX = (screenWidth - font.getStringWidth(label)) / 2;
            font.drawStringWithShadow(label, textX, y - 12, 0xFFFFFF);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Failed to draw RTM model-pack loading overlay (further occurrences suppressed).", t);
            }
        }
    }

    private static String buildLabel() {
        String phase = ModelPackProgressDisplay.getStatusText();
        int max = ModelPackProgressDisplay.getMax();
        StringBuilder sb = new StringBuilder("RTM ModelPacks");
        if (max > 0) {
            sb.append(" ").append(ModelPackProgressDisplay.getCount()).append('/').append(max);
        }
        if (phase != null && !phase.isEmpty()) {
            sb.append(" - ").append(phase);
        }
        return sb.toString();
    }
}