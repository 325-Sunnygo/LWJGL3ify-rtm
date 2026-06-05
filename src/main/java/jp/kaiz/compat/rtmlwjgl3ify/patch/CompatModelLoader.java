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

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class CompatModelLoader {
    private CompatModelLoader() {
    }

    public static Object loadModel(ResourceLocation resource, Object vecAccuracy, Object[] args) {
        // RTM's model parsers expect an asset-relative path like "models/foo.mqo".
        // Passing "minecraft:models/foo.mqo" can break path-based resolution.
        String fileName = resource.getResourcePath();
        try {
            InputStream mainStream = CompatHooks.getInputStream(resource);
            InputStream[] streams = createStreams(resource, mainStream);
            Method method = findLoaderMethod();
            return method.invoke(null, streams, fileName, vecAccuracy, args);
        } catch (IOException e) {
            throw new ModelFormatException("Failed to load model : " + fileName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access RTM model loader internals", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Failed to load model : " + fileName, cause);
        }
    }

    private static InputStream[] createStreams(ResourceLocation resource, InputStream mainStream) throws IOException {
        if (resource.getResourcePath().endsWith(".obj")) {
            String mtlName = resource.getResourcePath().replace(".obj", ".mtl");
            ResourceLocation mtl = new ResourceLocation(resource.getResourceDomain(), mtlName);
            InputStream mtlStream;
            try {
                mtlStream = CompatHooks.getInputStream(mtl);
            } catch (IOException ignored) {
                mtlStream = null;
            }
            return new InputStream[] { mainStream, mtlStream };
        }
        return new InputStream[] { mainStream };
    }

    private static Method findLoaderMethod() {
        try {
            Class<?> loaderClass = Class.forName("jp.ngt.ngtlib.renderer.model.ModelLoader");
            Method[] methods = loaderClass.getMethods();
            for (Method method : methods) {
                if ("loadModel".equals(method.getName()) && method.getParameterTypes().length == 4) {
                    Class<?>[] params = method.getParameterTypes();
                    if (InputStream[].class.equals(params[0]) && String.class.equals(params[1]) && Object[].class.equals(params[3])) {
                        return method;
                    }
                }
            }
            throw new NoSuchMethodException("loadModel(InputStream[], String, VecAccuracy, Object...)");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("RTM model loader class was not found", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("RTM model loader entrypoint was not found", e);
        }
    }
}