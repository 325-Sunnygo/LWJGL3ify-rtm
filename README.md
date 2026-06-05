# RTM LWJGL3ify Compat

Small external patch mod for Minecraft 1.7.10 that targets the rough edges between RealTrainMod model packs and `lwjgl3ify`.

By **325**. Repository: <https://github.com/325-Sunnygo/LWJGL3ify-rtm>

## Requirements

- Minecraft **1.7.10** with Minecraft Forge
- **lwjgl3ify** (with its prerequisites: GTNHLib, UniMixins)
- **Angelica** — **required**. RTM's renderer scripts use legacy immediate-mode OpenGL (`GL11.glBegin`/`glVertex`/display lists), which `lwjgl3ify` alone does not emulate. This mod routes those calls through Angelica's `GLStateManager`. Without Angelica you will get a `ClassNotFoundException: ...ScriptGL.glXxx` crash while RTM models render.
- **RealTrainMod** (works with stock RTM or a KaizPatchX-based RTM build)

## What it does

- Injects every `.zip` and `.jar` under `mods/modelpacks` into the Launch classpath very early, matching the approach used by KaizPatchX's `ModelPackLoader`.
- Coremods `jp.ngt.ngtlib.io.NGTFileLoader` so RTM/NGTLib modelpack file lookup uses a dedicated compat index instead of the fragile vanilla scan path.
- Coremods `jp.ngt.ngtlib.renderer.model.ModelLoader` so model loading resolves `.mqo`/`.obj` resources from modelpack archives through the compat loader.
- Coremods `jp.ngt.rtm.RTMConfig` so `ModelPack load speed` is clamped from `3` (Fast) to `2` (Default) at config-load time. The clamp runs inside `syncConfig`, before RTM's background `ModelPackLoadThread` reads the value, so it can't race the loader. Fast/work-stealing loading is noticeably less stable under `lwjgl3ify`.
- Routes RTM renderer scripts' `GL11` calls through Angelica's `GLStateManager` (via `ScriptGL`), and restores the `GuiSelectModel` model preview by doing the same for its projection/lighting setup instead of leaving it blank.
- On macOS, forces `java.awt.headless=true` early (matching what `lwjgl3ify`/RFB expect) so RTM never tries to open its Swing loading window on the GLFW main thread.
- Replaces that Swing window with an in-game OpenGL progress overlay (a bar + status text drawn over the title screen / HUD while model packs load), plus console logging. The overlay reads thread-safe progress state on the client render thread, so it works on macOS where RTM's Swing window cannot.

## What it does not do yet

- It is aimed at "modelpack loads and becomes usable" first, not the full breadth of KaizPatchX's extra fixes and optimizations.

## Why this exists

- Original RTM expects model pack resources to be classpath-visible in several places.
- `lwjgl3ify` setups are noticeably less stable when RTM model pack loading is pushed to the fastest parallel mode.

## Build

```bash
./gradlew build
```

Output jar:

```text
build/libs/rtm-lwjgl3ify-compat-0.1.0.jar
```

Building requires a Java 8 toolchain (ForgeGradle 1.2 does not run on newer JDKs).

## License

Licensed under the **GNU Lesser General Public License v3.0 (or later)**.
See [`COPYING.LESSER`](COPYING.LESSER) (LGPL-3.0) and [`COPYING`](COPYING) (GPL-3.0).

## Credits

- Author: **325**
- References KaizPatchX's modelpack zip classpath-injection approach.
