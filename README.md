# RTM LWJGL3ify Compat

RealTrainMod（RTM）のモデルパックを `lwjgl3ify` 環境で動かすための、Minecraft 1.7.10 向けの小さな外部パッチ Mod です。

作者: **325** / リポジトリ: <https://github.com/325-Sunnygo/LWJGL3ify-rtm>

## 必要なもの

- **Minecraft 1.7.10** + Minecraft Forge
- **lwjgl3ify**（前提として GTNHLib・UniMixins が必要）
- **Angelica** — **必須**。RTM のレンダースクリプトはレガシーな即時モード OpenGL（`GL11.glBegin` / `glVertex` / ディスプレイリスト）を使いますが、`lwjgl3ify` 単体ではこれをエミュレートしません。本 Mod はこれらの呼び出しを Angelica の `GLStateManager` 経由に転送します。Angelica が無いと、RTM のモデル描画時に `ClassNotFoundException: ...ScriptGL.glXxx` でクラッシュします。
- **RealTrainMod**（素の RTM でも、KaizPatchX ベースの RTM でも動作します）

## できること

- `mods/modelpacks` 内のすべての `.zip` / `.jar` を起動の非常に早い段階で Launch クラスパスへ注入します（KaizPatchX の `ModelPackLoader` と同じ方式）。
- `jp.ngt.ngtlib.io.NGTFileLoader` をコアMod改変し、RTM/NGTLib のモデルパックのファイル探索を、壊れやすいバニラの走査経路ではなく専用の互換インデックス経由にします。
- `jp.ngt.ngtlib.renderer.model.ModelLoader` をコアMod改変し、`.mqo` / `.obj` リソースをモデルパックのアーカイブから互換ローダー経由で解決します。
- `jp.ngt.rtm.RTMConfig` をコアMod改変し、`ModelPack load speed` を設定読み込み時に `3`（Fast）から `2`（Default）へクランプします。クランプは `syncConfig` 内（RTM のバックグラウンド `ModelPackLoadThread` が値を読む前）で実行されるため、ローダーとの競合（レース）が起きません。Fast（work-stealing）モードは `lwjgl3ify` 環境では明らかに不安定です。
- RTM レンダースクリプトの `GL11` 呼び出しを（`ScriptGL` 経由で）Angelica の `GLStateManager` に転送します。また `GuiSelectModel` のモデルプレビューを、同じくGL状態管理経由で投影・ライティングを組み立て直すことで、空白表示にせず復活させます。
- macOS では早い段階で `java.awt.headless=true` を設定し（`lwjgl3ify`/RFB の前提に合わせる）、RTM が GLFW のメインスレッド上で Swing のロードウィンドウを開こうとしないようにします。
- その Swing ウィンドウの代わりに、ゲーム内 OpenGL のプログレスオーバーレイ（モデルパック読み込み中にタイトル画面／HUD 上へバー＋状態テキストを描画）とコンソールログを表示します。オーバーレイはスレッドセーフな進捗状態をクライアント描画スレッドで読むため、RTM の Swing ウィンドウが使えない macOS でも動作します。

## まだやっていないこと

- まずは「モデルパックが読み込まれて使えるようになる」ことを目標にしており、KaizPatchX が持つ各種の追加修正・最適化の全範囲はカバーしていません。

## なぜ存在するか

- 素の RTM は、モデルパックのリソースが複数の箇所でクラスパスから見えることを前提にしています。
- `lwjgl3ify` 環境では、RTM のモデルパック読み込みを最速の並列モードにすると明らかに不安定になります。

## ビルド

```bash
./gradlew build
```

出力 jar:

```text
build/libs/rtm-lwjgl3ify-compat-1.0.0.jar
```

ビルドには Java 8 のツールチェーンが必要です（ForgeGradle 1.2 は新しい JDK では動きません）。

## ライセンス

**GNU 劣等一般公衆利用許諾書 バージョン 3 以降（GNU Lesser General Public License v3.0 or later）** で配布しています。
[`LICENSE`](LICENSE) / [`COPYING.LESSER`](COPYING.LESSER)（LGPL-3.0）および [`COPYING`](COPYING)（GPL-3.0）を参照してください。

## クレジット

- 作者: **325**
- KaizPatchX のモデルパック zip クラスパス注入の手法を参考にしています。
