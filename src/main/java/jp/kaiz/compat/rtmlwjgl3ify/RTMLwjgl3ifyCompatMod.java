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

package jp.kaiz.compat.rtmlwjgl3ify;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RTMLwjgl3ifyCompatMod.MOD_ID,
        name = "RTM LWJGL3ify Compat",
        version = RTMLwjgl3ifyCompatMod.VERSION,
        acceptableRemoteVersions = "*")
public class RTMLwjgl3ifyCompatMod {
    public static final String MOD_ID = "rtmlwjgl3ifycompat";
    public static final String VERSION = "1.0.0";

    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompat");

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // The functional compatibility work happens earlier and more reliably elsewhere:
        //  - java.awt.headless + Nashorn exclusions: set in the coremod's injectData (runs first).
        //  - ModelPack load speed clamp: applied via ASM on RTMConfig.syncConfig, so it lands before
        //    RTM's ModelPackLoadThread (started during RTM's own preInit) ever reads the value.
        // Doing the load-speed tweak here via reflection would race that background thread, so we don't.
        if (isLwjgl3ifyPresent()) {
            LOGGER.info("lwjgl3ify detected; RTM model-pack compatibility patches are active.");
        } else {
            LOGGER.info("lwjgl3ify was not detected; RTM model-pack compatibility patches are still applied harmlessly.");
        }

        // Register the in-game model-pack loading overlay on the client only, so the client-only
        // class is never loaded on a dedicated server. This replaces RTM's Swing loading window,
        // which can't be shown on macOS under lwjgl3ify (GLFW owns the main thread).
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new jp.kaiz.compat.rtmlwjgl3ify.client.ModelPackLoadingOverlay());
            LOGGER.info("Registered RTM model-pack loading overlay.");
        }
    }

    private static boolean isLwjgl3ifyPresent() {
        return Loader.isModLoaded("lwjgl3ify") || Launch.blackboard.get("lwjgl3ify:major-version") != null;
    }
}