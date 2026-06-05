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

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RTMLwjgl3ifyCompatTransformer implements IClassTransformer {
    private static final String TARGET_FILE_LOADER = "jp.ngt.ngtlib.io.NGTFileLoader";
    private static final String TARGET_MODEL_LOADER = "jp.ngt.ngtlib.renderer.model.ModelLoader";
    private static final String TARGET_SCRIPT_UTIL = "jp.ngt.ngtlib.io.ScriptUtil";
    private static final String TARGET_MODEL_PACK_LOAD_THREAD = "jp.ngt.rtm.modelpack.ModelPackLoadThread";
    private static final String TARGET_RTM_CONFIG = "jp.ngt.rtm.RTMConfig";
    private static final String TARGET_GUI_SELECT_MODEL = "jp.ngt.rtm.gui.GuiSelectModel";
    private static final String TARGET_MINFO_FONT = "jp.kaiz.minfo.api.MinFoCustomFontRenderer";
    private static final String TARGET_ANGELICA_BATCHING_FONT = "com.gtnewhorizons.angelica.client.font.BatchingFontRenderer";
    private static final String HOOKS = "jp/kaiz/compat/rtmlwjgl3ify/patch/CompatHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        if (TARGET_FILE_LOADER.equals(transformedName)) {
            return patchFileLoader(basicClass);
        }

        if (TARGET_MODEL_LOADER.equals(transformedName)) {
            return patchModelLoader(basicClass);
        }

        if (TARGET_SCRIPT_UTIL.equals(transformedName)) {
            return patchScriptUtil(basicClass);
        }

        if (TARGET_MODEL_PACK_LOAD_THREAD.equals(transformedName)) {
            return patchModelPackLoadThread(basicClass);
        }

        if (TARGET_RTM_CONFIG.equals(transformedName)) {
            return patchRtmConfig(basicClass);
        }

        if (TARGET_GUI_SELECT_MODEL.equals(transformedName)) {
            return patchGuiSelectModel(basicClass);
        }

        if (TARGET_MINFO_FONT.equals(transformedName)) {
            return patchMinFoFont(basicClass);
        }

        if (TARGET_ANGELICA_BATCHING_FONT.equals(transformedName)) {
            return patchAngelicaBatchingFont(basicClass);
        }

        return basicClass;
    }

    private byte[] patchFileLoader(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if ("getModsDir".equals(method.name) && "()Ljava/util/List;".equals(method.desc)) {
                method.instructions = new InsnList();
                method.tryCatchBlocks.clear();
                method.localVariables = null;
                method.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "getModsDir",
                        "()Ljava/util/List;",
                        false));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                method.maxStack = 1;
                method.maxLocals = 0;
            } else if ("getInputStream".equals(method.name)
                    && "(Lnet/minecraft/util/ResourceLocation;)Ljava/io/InputStream;".equals(method.desc)) {
                method.instructions = new InsnList();
                method.tryCatchBlocks.clear();
                method.localVariables = null;
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "getInputStream",
                        "(Lnet/minecraft/util/ResourceLocation;)Ljava/io/InputStream;",
                        false));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                method.maxStack = 1;
                method.maxLocals = 1;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchModelLoader(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if ("loadModel".equals(method.name)
                    && "(Lnet/minecraft/util/ResourceLocation;Ljp/ngt/ngtlib/renderer/model/VecAccuracy;[Ljava/lang/Object;)Ljp/ngt/ngtlib/renderer/model/PolygonModel;"
                    .equals(method.desc)) {
                method.instructions = new InsnList();
                method.tryCatchBlocks.clear();
                method.localVariables = null;
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                method.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "loadModel",
                        "(Lnet/minecraft/util/ResourceLocation;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                        false));
                method.instructions.add(new TypeInsnNode(
                        Opcodes.CHECKCAST,
                        "jp/ngt/ngtlib/renderer/model/PolygonModel"));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                method.maxStack = 3;
                method.maxLocals = 3;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchScriptUtil(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!"doScript".equals(method.name) || !"(Ljava/lang/String;)Ljavax/script/ScriptEngine;".equals(method.desc)) {
                continue;
            }

            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HOOKS,
                    "rewriteScript",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    false));
            hook.add(new VarInsnNode(Opcodes.ASTORE, 0));
            method.instructions.insert(hook);
            break;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchModelPackLoadThread(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if ("run".equals(method.name) && "()V".equals(method.desc)) {
                InsnList hook = new InsnList();
                hook.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "startModelPackProgress",
                        "()V",
                        false));
                method.instructions.insert(hook);
                continue;
            }

            if ("finish".equals(method.name) && "()V".equals(method.desc)) {
                InsnList hook = new InsnList();
                hook.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "finishModelPackProgress",
                        "()V",
                        false));
                method.instructions.insert(hook);
                continue;
            }

            if ("setMaxValue".equals(method.name) && "(IILjava/lang/String;)V".equals(method.desc)) {
                method.instructions.insert(progressHookThreeArgs("setModelPackMaxValue", "(IILjava/lang/String;)V"));
                continue;
            }

            if ("addMaxValue".equals(method.name) && "(II)V".equals(method.desc)) {
                method.instructions.insert(progressHookTwoInts("addModelPackMaxValue", "(II)V"));
                continue;
            }

            if ("setValue".equals(method.name) && "(IILjava/lang/String;)V".equals(method.desc)) {
                method.instructions.insert(progressHookThreeArgs("setModelPackValue", "(IILjava/lang/String;)V"));
                continue;
            }

            if ("addValue".equals(method.name) && "(ILjava/lang/String;)V".equals(method.desc)) {
                method.instructions.insert(progressHookIntString("addModelPackValue", "(ILjava/lang/String;)V"));
                continue;
            }

            if ("setText".equals(method.name) && "(ILjava/lang/String;)V".equals(method.desc)) {
                method.instructions.insert(progressHookIntString("setModelPackText", "(ILjava/lang/String;)V"));
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private InsnList progressHookThreeArgs(String name, String desc) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ILOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 3));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                name,
                desc,
                false));
        return hook;
    }

    private InsnList progressHookTwoInts(String name, String desc) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ILOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                name,
                desc,
                false));
        return hook;
    }

    private InsnList progressHookIntString(String name, String desc) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ILOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                name,
                desc,
                false));
        return hook;
    }

    /**
     * Clamp RTM's parallel model-pack {@code loadSpeed} down from 3 (Fast / work-stealing pool) to 2
     * (Default) when it is set to Fast. The fastest mode is noticeably less stable under lwjgl3ify.
     * We hook the tail of {@code syncConfig()} (which runs synchronously in RTM's preInit, before the
     * background {@code ModelPackLoadThread} is started) so the value is already clamped by the time
     * the loader reads it — a mod-side reflective tweak would race that thread.
     */
    private byte[] patchRtmConfig(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!"syncConfig".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }
            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    method.instructions.insertBefore(insn, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOKS,
                            "clampModelPackLoadSpeed",
                            "()V",
                            false));
                }
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * RTM's {@code GuiSelectModel.renderModel} drives the model preview with raw {@code GL11}/{@code GL12}
     * and {@code GLU.gluPerspective}. Raw immediate-mode GL desyncs Angelica's {@code GLStateManager}
     * under lwjgl3ify, which is why this was previously stubbed to a no-op (blank preview). Route it
     * through our hook instead, which performs the same setup via {@code GLStateManager} and renders
     * the model, degrading to a blank preview only if something actually throws.
     */
    private byte[] patchGuiSelectModel(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!"renderModel".equals(method.name)
                    || !"(Ljp/ngt/rtm/modelpack/modelset/IModelSetClient;Lnet/minecraft/client/Minecraft;)V".equals(method.desc)) {
                continue;
            }

            method.instructions = new InsnList();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HOOKS,
                    "renderSelectModel",
                    "(Ljava/lang/Object;Lnet/minecraft/client/Minecraft;)V",
                    false));
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = 2;
            method.maxLocals = 2;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchMinFoFont(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() != Opcodes.INVOKESPECIAL) {
                    continue;
                }
                if (!"net/minecraft/client/gui/FontRenderer".equals(call.owner) || !"<init>".equals(call.name)) {
                    continue;
                }

                org.objectweb.asm.tree.AbstractInsnNode textureNull = call.getPrevious() != null ? call.getPrevious().getPrevious() : null;
                org.objectweb.asm.tree.AbstractInsnNode resourceNull = textureNull != null ? textureNull.getPrevious() : null;
                if (resourceNull != null && resourceNull.getOpcode() == Opcodes.ACONST_NULL) {
                    method.instructions.set(resourceNull, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOKS,
                            "safeFontTexture",
                            "()Lnet/minecraft/util/ResourceLocation;",
                            false));
                }
                break;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchAngelicaBatchingFont(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)
                    || !"(Lnet/minecraft/client/gui/FontRenderer;[I[ILnet/minecraft/util/ResourceLocation;)V".equals(method.desc)) {
                continue;
            }

            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }

                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() != Opcodes.INVOKESPECIAL
                        || !"java/lang/Object".equals(call.owner)
                        || !"<init>".equals(call.name)) {
                    continue;
                }

                InsnList guard = new InsnList();
                org.objectweb.asm.tree.LabelNode continueLabel = new org.objectweb.asm.tree.LabelNode();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
                guard.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "skipAngelicaBatching",
                        "(Ljava/lang/Object;)Z",
                        false));
                guard.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, continueLabel));
                guard.add(new InsnNode(Opcodes.RETURN));
                guard.add(continueLabel);
                method.instructions.insert(insn, guard);
                break;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}