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

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CompatModelPackIndex {
    private static final Logger LOGGER = LogManager.getLogger("RTMLwjgl3ifyCompatIndex");
    private static volatile CompatModelPackIndex INSTANCE;

    private final Map<String, List<ModelPackSource>> packsByDomain;
    private final List<File> modsDir;

    private CompatModelPackIndex() {
        Map<String, List<ModelPackSource>> domainMap = new HashMap<String, List<ModelPackSource>>();
        List<File> dirs = new ArrayList<File>();

        for (File file : getModsOrJarsInternal()) {
            if (file.isDirectory()) {
                dirs.add(file);
                registerPack(domainMap, new DirectoryPackSource(file));
            } else if (isArchive(file.getName())) {
                File parent = file.getParentFile();
                if (parent != null) {
                    dirs.add(parent);
                }
                try {
                    registerPack(domainMap, new ArchivePackSource(file));
                } catch (IllegalArgumentException e) {
                    try {
                        registerPack(domainMap, new ArchivePackSource(file, Charset.forName("MS932")));
                    } catch (IOException inner) {
                        LOGGER.warn("Skipping unreadable archive {}", file.getAbsolutePath(), inner);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Skipping unreadable archive {}", file.getAbsolutePath(), e);
                }
            }
        }

        this.packsByDomain = domainMap;
        this.modsDir = Collections.unmodifiableList(deduplicateFiles(dirs));
        LOGGER.info("Indexed {} model-pack domains from {} search roots.", this.packsByDomain.size(), this.modsDir.size());
    }

    public static List<File> getModsDir() {
        return getInstance().modsDir;
    }

    public static InputStream getInputStream(ResourceLocation location) throws IOException {
        ModelPackSource source = getInstance().findSource(location);
        if (source == null) {
            throw new FileNotFoundException(location.toString());
        }
        return source.open(location);
    }

    private static CompatModelPackIndex getInstance() {
        CompatModelPackIndex current = INSTANCE;
        if (current == null) {
            synchronized (CompatModelPackIndex.class) {
                current = INSTANCE;
                if (current == null) {
                    current = new CompatModelPackIndex();
                    INSTANCE = current;
                }
            }
        }
        return current;
    }

    private static void registerPack(Map<String, List<ModelPackSource>> domainMap, ModelPackSource source) {
        for (String domain : source.getDomains()) {
            List<ModelPackSource> list = domainMap.get(domain);
            if (list == null) {
                list = new ArrayList<ModelPackSource>();
                domainMap.put(domain, list);
            }
            list.add(source);
        }
    }

    private ModelPackSource findSource(ResourceLocation location) {
        List<ModelPackSource> sources = this.packsByDomain.get(location.getResourceDomain());
        if (sources == null) {
            return null;
        }
        for (ModelPackSource source : sources) {
            if (source.has(location)) {
                return source;
            }
        }
        return null;
    }

    private static List<File> getModsOrJarsInternal() {
        List<File> files = new ArrayList<File>();

        File gameDir = Launch.minecraftHome;
        if (gameDir != null) {
            files.add(new File(gameDir, "mods"));
            files.add(new File(gameDir, "jar-mods-cache/v1/mods"));
            files.add(new File(gameDir, "mods/modelpacks"));
        }

        return flattenExisting(files);
    }

    private static List<File> flattenExisting(List<File> roots) {
        List<File> flattened = new ArrayList<File>();
        for (File root : roots) {
            if (!root.exists()) {
                continue;
            }
            if (root.isDirectory()) {
                File[] children = root.listFiles();
                if (children == null || children.length == 0) {
                    flattened.add(root);
                    continue;
                }
                Arrays.sort(children);
                for (File child : children) {
                    flattened.add(child);
                }
                flattened.add(root);
            } else {
                flattened.add(root);
            }
        }
        return deduplicateFiles(flattened);
    }

    private static List<File> deduplicateFiles(List<File> files) {
        List<File> result = new ArrayList<File>();
        Set<String> seen = new HashSet<String>();
        for (File file : files) {
            try {
                String path = file.getCanonicalPath();
                if (seen.add(path)) {
                    result.add(file);
                }
            } catch (IOException e) {
                String path = file.getAbsolutePath();
                if (seen.add(path)) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private static boolean isArchive(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        return lowered.endsWith(".jar") || lowered.endsWith(".zip");
    }

    private interface ModelPackSource {
        Set<String> getDomains();

        boolean has(ResourceLocation location);

        InputStream open(ResourceLocation location) throws IOException;
    }

    private static final class DirectoryPackSource implements ModelPackSource {
        private final File root;
        private final Set<String> domains;

        private DirectoryPackSource(File root) {
            this.root = root;
            this.domains = scanDomains(root);
        }

        @Override
        public Set<String> getDomains() {
            return this.domains;
        }

        @Override
        public boolean has(ResourceLocation location) {
            return resolve(location).isFile();
        }

        @Override
        public InputStream open(ResourceLocation location) throws IOException {
            File file = resolve(location);
            if (!file.isFile()) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            return new java.io.BufferedInputStream(new java.io.FileInputStream(file));
        }

        private File resolve(ResourceLocation location) {
            return new File(this.root, "assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
        }
    }

    private static final class ArchivePackSource implements ModelPackSource {
        private final File file;
        private final Charset charset;
        private final Set<String> domains;
        private final Map<String, String> lowerCaseEntries;

        private ArchivePackSource(File file) throws IOException {
            this(file, Charset.forName("UTF-8"));
        }

        private ArchivePackSource(File file, Charset charset) throws IOException {
            this.file = file;
            this.charset = charset;

            Set<String> scannedDomains = new HashSet<String>();
            Map<String, String> entryMap = new HashMap<String, String>();
            ZipFile zip = openZip();
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    String[] parts = name.split("/");
                    if (parts.length >= 2 && "assets".equals(parts[0]) && parts[1].length() > 0) {
                        scannedDomains.add(parts[1]);
                    }
                    entryMap.put(name.toLowerCase(Locale.ROOT), name);
                }
            } finally {
                zip.close();
            }
            this.domains = scannedDomains;
            this.lowerCaseEntries = entryMap;
        }

        @Override
        public Set<String> getDomains() {
            return this.domains;
        }

        @Override
        public boolean has(ResourceLocation location) {
            // The entry name set was scanned once at construction; consult that cached index instead
            // of reopening the archive on every lookup (RTM probes many resources during loading).
            String path = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
            return this.lowerCaseEntries.containsKey(path.toLowerCase(Locale.ROOT));
        }

        @Override
        public InputStream open(ResourceLocation location) throws IOException {
            ZipFile zip = openZip();
            ZipEntry entry = zip.getEntry(resolveEntryName(location));
            if (entry == null) {
                closeQuietly(zip);
                throw new FileNotFoundException(location.toString());
            }
            return new ZipBackedInputStream(zip, zip.getInputStream(entry));
        }

        private String resolveEntryName(ResourceLocation location) {
            String path = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
            String mapped = this.lowerCaseEntries.get(path.toLowerCase(Locale.ROOT));
            return mapped != null ? mapped : path;
        }

        private ZipFile openZip() throws IOException {
            if (this.file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return new JarFile(this.file);
            }
            return new ZipFile(this.file, this.charset);
        }
    }

    private static Set<String> scanDomains(File root) {
        File assets = new File(root, "assets");
        if (!assets.isDirectory()) {
            return Collections.emptySet();
        }
        Set<String> domains = new HashSet<String>();
        File[] children = assets.listFiles();
        if (children == null) {
            return domains;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                domains.add(child.getName());
            }
        }
        return domains;
    }

    private static void closeQuietly(ZipFile zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static final class ZipBackedInputStream extends InputStream {
        private final ZipFile zipFile;
        private final InputStream delegate;

        private ZipBackedInputStream(ZipFile zipFile, InputStream delegate) {
            this.zipFile = zipFile;
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return this.delegate.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.delegate.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return this.delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return this.delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            return this.delegate.available();
        }

        @Override
        public void close() throws IOException {
            try {
                this.delegate.close();
            } finally {
                this.zipFile.close();
            }
        }
    }
}