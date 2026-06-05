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

package jp.kaiz.compat.rtmlwjgl3ify.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

@IFMLLoadingPlugin.Name("RTMLwjgl3ifyCompatCoremod")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class RTMLwjgl3ifyCompatCoremod implements IFMLLoadingPlugin {
    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompatCoremod");

    @Override
    public void injectData(Map<String, Object> data) {
        forceHeadlessOnMac();
        addNashornExclusions();

        File mcDir = (File) data.get("mcLocation");
        if (mcDir == null) {
            LOGGER.warn("mcLocation was not provided by FML; skipping modelpack zip injection.");
            return;
        }

        File modelpackDir = new File(mcDir, "mods/modelpacks");
        if (!modelpackDir.isDirectory()) {
            LOGGER.info("No mods/modelpacks directory found at {}", modelpackDir.getAbsolutePath());
            return;
        }

        File[] files = modelpackDir.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            LOGGER.info("No modelpack archives found in {}", modelpackDir.getAbsolutePath());
            return;
        }

        Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        for (File archive : files) {
            try {
                Launch.classLoader.addURL(archive.toURI().toURL());
                LOGGER.info("Injected modelpack archive into classpath: {}", archive.getName());
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to inject modelpack archive: {}", archive.getAbsolutePath(), e);
            }
        }
    }

    /**
     * lwjgl3ify runs GLFW on the main thread on macOS and mandates {@code java.awt.headless=true}
     * (its {@code org.lwjglx.Sys} throws otherwise). When headless, RTM's {@code ModelPackLoadThread}
     * skips its Swing progress window entirely, which is exactly what we want — a Swing JFrame cannot
     * be shown on the GLFW main thread. RFB normally sets this, but set it defensively here (the
     * earliest point we run) so non-RFB launch paths don't crash. Only macOS, never override the user.
     */
    private static void forceHeadlessOnMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac")) {
            return;
        }
        if (System.getProperty("java.awt.headless") != null) {
            return;
        }
        System.setProperty("java.awt.headless", "true");
        LOGGER.info("Set java.awt.headless=true for macOS lwjgl3ify compatibility.");
    }

    /**
     * RTM's renderer scripts run through Nashorn, which generates synthetic script classes at runtime.
     * Letting LaunchWrapper try to load/transform those generated classes is both pointless and a
     * frequent source of crashes under lwjgl3ify's relocated Nashorn. Exclude them. This must happen
     * before any script engine is initialized, which is why it lives in the coremod rather than the
     * mod's preInit (RTM starts its model-load/script work during its own preInit).
     */
    private static void addNashornExclusions() {
        String[] prefixes = {
                "org.openjdk.nashorn.internal.scripts.",
                "jdk.nashorn.internal.scripts."
        };
        try {
            for (String prefix : prefixes) {
                Launch.classLoader.addClassLoaderExclusion(prefix);
                Launch.classLoader.addTransformerExclusion(prefix);
            }
            LOGGER.info("Added Nashorn generated-script exclusions for lwjgl3ify compatibility.");
        } catch (Throwable t) {
            LOGGER.warn("Failed to add Nashorn generated-script exclusions.", t);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "jp.kaiz.compat.rtmlwjgl3ify.core.RTMLwjgl3ifyCompatTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}