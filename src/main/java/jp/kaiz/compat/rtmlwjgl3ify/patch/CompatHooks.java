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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CompatHooks {
    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompatHooks");
    private static final String LWJGL_GL11_PREFIX = "Packages.org.lwjgl.opengl.GL11.";
    private static final String SCRIPT_GL_PREFIX = "Packages.jp.kaiz.compat.rtmlwjgl3ify.patch.ScriptGL.";

    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<String>());

    private CompatHooks() {
    }

    public static List getModsDir() {
        List delegated = getModsDirFromFixFileLoader();
        return delegated != null ? delegated : CompatModelPackIndex.getModsDir();
    }

    public static InputStream getInputStream(ResourceLocation location) throws IOException {
        IOException deferred = null;

        try {
            InputStream stream = getInputStreamFromFixFileLoader(location);
            if (stream != null) {
                return stream;
            }
        } catch (IOException e) {
            deferred = e;
        }

        try {
            return CompatModelPackIndex.getInputStream(location);
        } catch (IOException e) {
            if (deferred != null) {
                e.addSuppressed(deferred);
            }
            throw e;
        }
    }

    public static Object loadModel(ResourceLocation resource, Object vecAccuracy, Object[] args) {
        return CompatModelLoader.loadModel(resource, vecAccuracy, args);
    }

    public static void startModelPackProgress() {
        ModelPackProgressDisplay.start();
    }

    public static void finishModelPackProgress() {
        ModelPackProgressDisplay.finish();
    }

    public static void setModelPackMaxValue(int index, int maxValue, String text) {
        ModelPackProgressDisplay.setMaxValue(index, maxValue, text);
    }

    public static void addModelPackMaxValue(int index, int delta) {
        ModelPackProgressDisplay.addMaxValue(index, delta);
    }

    public static void setModelPackValue(int index, int value, String text) {
        ModelPackProgressDisplay.setValue(index, value, text);
    }

    public static void addModelPackValue(int index, String text) {
        ModelPackProgressDisplay.addValue(index, text);
    }

    public static void setModelPackText(int index, String text) {
        ModelPackProgressDisplay.setText(index, text);
    }

    public static ResourceLocation safeFontTexture() {
        return new ResourceLocation("textures/font/ascii.png");
    }

    public static boolean skipAngelicaBatching(Object underlying) {
        return isMinFoFontRenderer(underlying) || !hasCurrentOpenGlContext();
    }

    /**
     * Invoked from the tail of {@code RTMConfig.syncConfig}. Clamps the parallel model-pack load speed
     * down from 3 (Fast) to 2 (Default); Fast is noticeably less stable under lwjgl3ify. Reflection is
     * used so this class carries no compile-time dependency on RTM, and missing fields degrade quietly.
     */
    public static void clampModelPackLoadSpeed() {
        try {
            Class<?> cfg = Class.forName("jp.ngt.rtm.RTMConfig");
            Field loadSpeed = cfg.getField("loadSpeed");
            int current = loadSpeed.getInt(null);
            if (current >= 3) {
                loadSpeed.setInt(null, 2);
                LOGGER.info("Clamped RTM ModelPack load speed from {} (Fast) to 2 (Default) for lwjgl3ify stability.", current);
            }
        } catch (Throwable t) {
            warnOnce("clampModelPackLoadSpeed", "Could not clamp RTM ModelPack load speed", t);
        }
    }

    /**
     * Replacement body for {@code GuiSelectModel.renderModel}. Mirrors RTM's original model-preview
     * setup but routes every GL call through Angelica's {@code GLStateManager} (via {@link ScriptGL})
     * so the projection/lighting state stays in sync. Any failure degrades to a blank preview rather
     * than crashing the GUI, and the GL matrix stack is always rebalanced.
     */
    public static void renderSelectModel(Object modelSetClient, Minecraft mc) {
        if (modelSetClient == null || mc == null) {
            return;
        }
        try {
            ScriptGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            ScriptGL.glPushMatrix();
            ScriptGL.glMatrixMode(ScriptGL.GL_PROJECTION);
            ScriptGL.glPushMatrix();
            try {
                ScriptGL.glLoadIdentity();
                gluPerspective(80.0F, 1.0F, 5.0F, 1000.0F);
                ScriptGL.glMatrixMode(ScriptGL.GL_MODELVIEW);
                ScriptGL.glLoadIdentity();
                RenderHelper.enableStandardItemLighting();
                ScriptGL.glEnable(ScriptGL.GL_DEPTH_TEST);
                ScriptGL.glEnable(ScriptGL.GL_RESCALE_NORMAL);
                renderModelInGui(modelSetClient, mc);
            } finally {
                ScriptGL.glDisable(ScriptGL.GL_RESCALE_NORMAL);
                ScriptGL.glDisable(ScriptGL.GL_DEPTH_TEST);
                RenderHelper.disableStandardItemLighting();
                ScriptGL.glMatrixMode(ScriptGL.GL_PROJECTION);
                ScriptGL.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
                ScriptGL.glPopMatrix();
                ScriptGL.glMatrixMode(ScriptGL.GL_MODELVIEW);
                ScriptGL.glPopMatrix();
                ScriptGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } catch (Throwable t) {
            warnOnce("renderSelectModel", "Model preview rendering failed; showing a blank preview", t);
        }
    }

    private static void renderModelInGui(Object modelSetClient, Minecraft mc) throws ReflectiveOperationException {
        Method method = modelSetClient.getClass().getMethod("renderModelInGui", Minecraft.class);
        method.invoke(modelSetClient, mc);
    }

    private static void gluPerspective(float fovy, float aspect, float zNear, float zFar) throws ReflectiveOperationException {
        Class<?> project = Class.forName("org.lwjgl.util.glu.Project");
        Method method = project.getMethod("gluPerspective", float.class, float.class, float.class, float.class);
        method.invoke(null, Float.valueOf(fovy), Float.valueOf(aspect), Float.valueOf(zNear), Float.valueOf(zFar));
    }

    private static void warnOnce(String key, String message, Throwable t) {
        if (WARNED.add(key)) {
            LOGGER.warn("{} (further occurrences suppressed).", message, t);
        }
    }

    public static String rewriteScript(String source) {
        if (source == null || source.indexOf("GL11") < 0) {
            return source;
        }

        String rewritten = source.replace(LWJGL_GL11_PREFIX, SCRIPT_GL_PREFIX);
        rewritten = rewritten.replace("GL11.", SCRIPT_GL_PREFIX);
        return rewritten;
    }

    private static boolean isMinFoFontRenderer(Object underlying) {
        return underlying != null
                && "jp.kaiz.minfo.api.MinFoCustomFontRenderer".equals(underlying.getClass().getName());
    }

    private static List getModsDirFromFixFileLoader() {
        try {
            Object loader = getFixFileLoaderInstance();
            Method method = loader.getClass().getMethod("getModsOrJars");
            Object result = method.invoke(loader);
            if (!(result instanceof List)) {
                return null;
            }

            List files = (List) result;
            List dirs = new ArrayList(files.size());
            for (Object entry : files) {
                if (!(entry instanceof File)) {
                    continue;
                }
                File file = (File) entry;
                dirs.add(file.isDirectory() ? file : file.getParentFile());
            }
            return dirs;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static InputStream getInputStreamFromFixFileLoader(ResourceLocation location) throws IOException {
        try {
            Object loader = getFixFileLoaderInstance();
            Method method = loader.getClass().getMethod("getInputStream", ResourceLocation.class);
            Object result = method.invoke(loader, location);
            return result instanceof InputStream ? (InputStream) result : null;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getFixFileLoaderInstance() throws ReflectiveOperationException {
        Class<?> loaderClass = Class.forName("jp.kaiz.kaizpatch.fixrtm.modelpack.FIXFileLoader");
        Field instanceField = loaderClass.getField("INSTANCE");
        return instanceField.get(null);
    }

    private static boolean hasCurrentOpenGlContext() {
        try {
            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            Object value = glfwClass.getMethod("glfwGetCurrentContext").invoke(null);
            if (value instanceof Long) {
                return ((Long) value).longValue() != 0L;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}