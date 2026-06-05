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

package jp.kaiz.compat.rtmlwjgl3ify.patch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects RTM model-pack loading progress and exposes it to the log and to an in-game overlay.
 *
 * RTM's own progress UI is a Swing {@code JFrame}, which is skipped whenever the JVM is headless —
 * and lwjgl3ify forces {@code java.awt.headless=true} on macOS because GLFW owns the main thread.
 * That leaves the user with no feedback during a load that can take many seconds. Touching the Forge
 * splash/window from RTM's background load thread is unsafe (it can freeze the client), so this class
 * never renders anything itself: it only records thread-safe state (and writes to the thread-safe log).
 * {@link jp.kaiz.compat.rtmlwjgl3ify.client.ModelPackLoadingOverlay} reads that state on the client
 * render thread, where the GL context is fully up, and draws the progress bar.
 *
 * Per-item ticks ({@link #addValue}) are not logged individually to avoid flooding the log with
 * thousands of lines; they are summarized at {@link #finish()}. Phase text changes are logged,
 * de-duplicated so repeats are dropped.
 */
public final class ModelPackProgressDisplay {
    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompatProgress");

    /** RTM's per-model registration bar; its fraction is what we surface to the user. */
    private static final int PRIMARY_BAR = 1;

    private static final AtomicInteger ITEM_COUNT = new AtomicInteger();

    private static volatile boolean active;
    private static volatile int primaryMax;
    private static volatile String statusText = "";
    private static volatile String lastLoggedText;

    private ModelPackProgressDisplay() {
    }

    public static void start() {
        ITEM_COUNT.set(0);
        primaryMax = 0;
        statusText = "Starting";
        lastLoggedText = null;
        active = true;
        LOGGER.info("RTM model-pack loading started.");
    }

    public static void finish() {
        active = false;
        LOGGER.info("RTM model-pack loading finished ({} entries processed).", ITEM_COUNT.get());
    }

    public static void setMaxValue(int index, int maxValue, String text) {
        if (index == PRIMARY_BAR) {
            primaryMax = maxValue;
        }
        logPhase(text);
    }

    public static void addMaxValue(int index, int delta) {
        if (index == PRIMARY_BAR && delta > 0) {
            primaryMax += delta;
        }
    }

    public static void setValue(int index, int value, String text) {
        logPhase(text);
    }

    public static void addValue(int index, String text) {
        ITEM_COUNT.incrementAndGet();
        logPhase(text);
    }

    public static void setText(int index, String text) {
        logPhase(text);
    }

    // --- Read side, consumed by the client overlay (all volatile/atomic, no locks) ---

    public static boolean isActive() {
        return active;
    }

    /** Progress in [0,1], or a negative value when the total is not yet known (indeterminate). */
    public static float getProgress() {
        int max = primaryMax;
        if (max <= 0) {
            return -1.0F;
        }
        int count = ITEM_COUNT.get();
        if (count >= max) {
            return 1.0F;
        }
        return (float) count / (float) max;
    }

    public static int getCount() {
        return ITEM_COUNT.get();
    }

    public static int getMax() {
        return primaryMax;
    }

    public static String getStatusText() {
        return statusText;
    }

    private static void logPhase(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        statusText = text;
        if (text.equals(lastLoggedText)) {
            return;
        }
        lastLoggedText = text;
        LOGGER.info("RTM model-pack: {}", text);
    }
}