# webhtv
自编译 webhtv（默影视）电视版 —— 首页 UI 定制：分类导航常显、首行跟随滚动、源名不可选中、站源入口在首页、胶囊按钮


基于 **webhtv 源码**（包名 `com.silent.android.webhtv`，versionCode `560`，versionName `5.6.0`）改造的电视端影视 App。
目标设备：海信/VIDAA MTK9652，Android 9，`abilist = armeabi-v7a,armeabi` → **只能装 v7a 包**。

> 与 `OK影视-改造版`（`com.fongmi.android.tv` 5.1.6，apktool 改 smali）是**两条完全不同的线**，别混。
> 本仓库只放可复现改造的源码文件，APK 走 Release 附件 / 夸克网盘。

## 版本锚点

| 项 | 值 |
|---|---|
| 基线 | 上游 webhtv 5.6.0 (560) + 2026-10-23 首页顶栏改造 |
| 本版 | build1 —— 追加 2026-11-03 倍速上键 + 回看跳末尾两项修复 |
| 编译变体 | `:app:assembleLeanbackArmeabi_v7aRelease`（leanback = 电视桌面，v7a = 电视 ABI） |
| 产物 | `app/build/outputs/apk/leanbackArmeabi_v7a/release/leanback-armeabi_v7a.apk`（约 144 MB） |

## 改造清单

| # | 需求 | 实现位置 | 做法 |
|---|---|---|---|
| 1 | 播放界面「倍速」处**上翻加速**（原来上翻无反应、下翻能减速） | `VideoActivity.dispatchOpeningEndingAdjust()` + 新增 `dispatchSpeedAdjust()` | 在 Activity 分发阶段先拦截：焦点落在 `mBinding.control.action.speed` 时，`KeyUtil.isUpKey → onSpeedAdd()`、`isDownKey → onSpeedSub()`，`return true` |
| 2 | 看过的剧集**回到前一集总跳到末尾**，无法回看 | `VideoActivity.updateHistory()` / `resolveInitialPlaybackPosition()` / `checkEnded()`、`History.isNearEnding()`、`EpisodePositionCache` | ① 换集时旧集进度写进 `EpisodePositionCache`，新集无缓存则显式清空位置，避免继承旧集「播完」进度；② 续播点落在片尾附近（`isNearEnding()`）则从头播；③ 开播 5 秒内收到 ENDED 判为「回看已看完的前一集」，改为从 0 重播 |
| 3 | 首页顶栏改造（2026-10-23） | `HomeButton` / `CustomTitleView` / `FuncPresenter` / `TypeAdapter` / `HomeActivity` + 布局资源 | 顶栏常显、按钮行按内容算宽贴右、去掉 `FuncPresenter` 自带左右循环让焦点能走出、分类导航常显 |

### 第 1 条为什么必须放在 Activity 层

按键分发顺序是：

```
Activity.dispatchKeyEvent
  └─ dispatchOpeningEndingAdjust()          ← 只处理片头/片尾
  └─ PlayerControlFocusHelper.handleKey()   ← 这里把上键吞了
  └─ …才轮到 CustomUpDownView（倍速控件）
```

`PlayerControlFocusHelper.handleKey()` 先用 `focusSearch` 判断焦点能否移走，**移不出去就消费掉事件**。
倍速按钮上方没有可聚焦控件 → 上键被吞；下方有进度条 → 下键放行。所以在控件层改无效，必须按片头/片尾同样的模式在 Activity 层先拦。

## patches/ 内容

相对上游改动过的**完整文件**（不是 diff），按原路径摆放，见 `patches/FILES.txt`（15 个文件，约 736 KB）：

- `2026-10-23`：`HomeButton.java`、`CustomTitleView.java`、`FuncPresenter.java`、`TypeAdapter.java`、`HomeActivity.java`、`adapter_func.xml`、`activity_home.xml`、`shape_*_pill.xml`、三套 `strings.xml`
- `2026-11-03`：`VideoActivity.java`、`History.java`、`CustomUpDownView.java`

## 构建

```bash
export JAVA_HOME=~/build/jdk21
cd <webhtv-src>
nice -n 19 ionice -c3 ./gradlew --no-daemon -PfastRelease=true \
  :app:assembleLeanbackArmeabi_v7aRelease
```

- 本机（8G 内存 / 39 容器）必须限堆：`gradle.properties` → `-Xmx1536m -XX:MaxMetaspaceSize=448m -XX:+UseSerialGC`、`parallel=false`、`workers.max=1`、`kotlin.compiler.execution.strategy=in-process`。
- `settings.gradle` 里阿里云镜像（`repository/google`、`repository/public`）必须排在 `google()`/`mavenCentral()` **前面**，否则会命中 mihomo fake-ip（198.18.x.x）超时并把整个仓库标 disabled。
- 一次只编一个变体，别一把 `assemble`。
- 装机：同源包 `adb install -r` 直接 Success，配置不丢；跨签名才需要卸载（会清空内容源）。

## 踩过的坑

- **电视只认 v7a**：arm64 包报 `INSTALL_FAILED_NO_MATCHING_ABIS (res=-113)`。
- **截图带前缀**：`adb exec-out screencap -p` 前 44 字节是 `Init wrapper sys mutex successful...`，按 `\x89PNG\r\n\x1a\n` 偏移切掉再喂视觉模型。
- **别用控制条亮度判焦点**：控制条是半透明的，背景画面在变，按钮区平均亮度无法稳定指示焦点位置。
- **守护脚本见 `BUILD SUCCESSFUL/FAILED` 就停采样**：Gradle JVM 退场一次性释放 2~3G，`pswpout` 冲到 3000+ 页/秒会被误判为抖动 → 编成功却 exit 2。产物用 `unzip -t` + `md5sum` 核验。
- **别用工具后台任务跑编译**：Hermes 终端 300s 生命周期会 SIGTERM（exit 143，日志无任何错误）。用 `systemd-run --user` 或守护脚本。
