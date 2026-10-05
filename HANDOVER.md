# 交接日志 — 旅行者背包：额外升级（Traveler's Backpack: Extra Upgrades）

> 面向下一个对话的自己。读完这一页即可继续开发，不需要重新摸索。

## 一、项目基本信息

- 项目目录：`E:\Minecraft\MODS\TravelersBackpackExtras`（jar 输出 `build\libs\travelersbackpackextras-1.21.11-<版本>.jar`）
- 目标：**Minecraft 1.21.11 / Fabric**，硬依赖旅行者背包（`travelersbackpack`）与 Advanced Netherite
- 版本号在 `gradle.properties` 的 `mod_version`；**不保留任何构建号**（用户要求过：去掉 `BUILD` 常量与进游戏聊天提示）
- 素材来源（用户提供）：`E:\Minecraft\MODS\旅行者背包附属`
- **用户实际测试实例：`E:\Minecraft\.minecraft\versions\1.21.11test`**（71 个模组，Fabric Loader **0.19.5**、Fabric API **0.141.6** —— 都比开发环境新）。交接文档里以前写的 `1.21.11Fabric`（163 模组）是他另一个实例，**别再往那边找**
  - 命名规则：`1.21.11Fabric` = 完整模组包；`1.21.11test` = 精简测试包，用户改功能时用的是这个
  - **我能直接读他的日志**：`E:\Minecraft\.minecraft\versions\1.21.11test\logs\latest.log`（游戏在跑时文件被锁，要用 `FileShare.ReadWrite` 打开）。旧会话是 `logs\2026-10-05-N.log.gz`，用 `GZipStream` 解。
    - **这比让用户截图/描述强太多**：登录坐标、登录/登出时刻、加载的模组版本号全在里面。这轮就是靠日志确认了「他确实装上并跑了修复版」，排除了"测了旧包"的可能
  - 日志是 GBK/UTF-8 混杂，`read` 工具会报 invalid UTF-8，用 `[System.IO.File]::ReadAllText(..., UTF8)` 或 `StreamReader` 读
  - 写他实例里的文件会被沙箱拦（workspace 只覆盖 `E:\Minecraft\MODS`）→ 要装 jar 得用 `sandbox_permissions: danger-full-access` 提权重试

### 构建与验证命令（每次都照这个来）

```powershell
$env:JAVA_HOME="D:\JavaStudy\JDK\jdk-21"; $env:GRADLE_USER_HOME="E:\Minecraft\MODS\.gradle-home"
# 在项目目录下：
E:\Minecraft\MODS\.tools\gradle-9.5.1\bin\gradle.bat build --console=plain
E:\Minecraft\MODS\.tools\gradle-9.5.1\bin\gradle.bat runServer --console=plain   # 自检，看 RESULT: PASS
E:\Minecraft\MODS\.tools\gradle-9.5.1\bin\gradle.bat runClient --console=plain   # 改了客户端混入必须跑一次
```

- 自检：`SelfTest.java`，仅在 `-Dtravelersbackpackextras.selfTest=true`（runServer 已配）时运行，当前 **112 条全过**；输出 `RESULT: PASS`
- 混入目标校验：`MixinTargetVerifier`（服务端，15 个 common 目标）/ `ClientMixinTargetVerifier`（客户端 `-Dtravelersbackpackextras.verifyMixins=true`，**9 个 client 目标**）
- **改动客户端混入后必须跑 `runClient`**：选择器写错会当场崩客户端，服务端测不出来。看日志里的 `verified 9 client mixin targets` 和有没有 `Exception`
- 构建失败不会污染已有 jar（**不要**在这条流程里用 `clean`）

### 反编译/查 API 的手段（这轮新摸索出来的，很好用）

没有现成反编译器，但**有整套编译期映射好的 jar**，`javap` 直接能看命名后的签名和字节码：

```powershell
$jdk = "D:\JavaStudy\JDK\jdk-21\bin\javap.exe"
# 原版（named）：
$mc  = "E:\Minecraft\MODS\.gradle-home\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2\minecraft-merged-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2.jar"
# 旅行者背包：
$tb  = "E:\Minecraft\.minecraft\versions\1.21.11Fabric\mods\[旅行者背包] travelersbackpack-fabric-1.21.11-10.11.11.jar"
& $jdk -p -classpath $mc net.minecraft.client.gui.Gui                 # 只看签名
& $jdk -p -c -classpath $tb com.tiviacz.travelersbackpack.common.ServerActions   # 看完整字节码
& $jdk -p -constants -classpath $tb com.tiviacz.travelersbackpack.common.ServerActions  # 看常量数值
```

- 找某个类在哪个包：把 jar 当 zip 遍历条目名（`[System.IO.Compression.ZipFile]::OpenRead`）
- 找「哪些类实现了某接口」：把所有 `.class` 解出来，逐个 `javap -p` 看有没有出现接口名
- Fabric API 的各模块 **有 sources jar**，在 `E:\Minecraft\MODS\.gradle-home\caches\modules-2\files-2.1\net.fabricmc.fabric-api\<模块>\...` 里挑 `-sources.jar`，解出来就能读真源码
- 字节码丢到 `E:\Minecraft\MODS\.analysis\`（分析用，不属于项目，不要提交）

## 二、当前状态

- 已发布过：**1.0.4**
- 本轮做完了 **1.1.0**（欧米伽创造飞行全套：飞行值 + 右侧开关 + Shift+F + 经验条 HUD + 等级数字倒数 + **飞行值存档** + **重进保留空中状态（欧米伽与金苹果两条都算）**），已 build 通过、自检 112/112、runClient 通过（9 个客户端混入目标），**用户已实机确认两条都可用**，临时诊断已删干净，**准备发布**
  - **版本号最终定在 1.1.0**（用户明确要求不用升号）。中间我一度升到 1.1.1 / 1.1.2，都只是给用户看的，**没发布**，对应的 jar 已删掉，`build\libs` 里现在只有 `1.0.4`（上个正式版）和 `1.1.0`（本次）
  - 教训补充：**先问清楚上一版发没发，再决定升不升号**。我这轮连着升了两次号都是白升，用户最后说「版本号还是 1.1.0 不用改」
- **还没提交**：见最后一节的推送指令
- **还没提交**：见最后一节的推送指令

## 三、欧米伽创造飞行（1.1.0，已完成）

用户确认的设计与实际实现：

| 项目 | 决定 | 落在哪 |
|---|---|---|
| 飞行值 | 600，飞行时 1/秒消耗，不飞行时 0.8/秒回复 | `OmegaFlight.tick` |
| 归零 | 失去创造飞行 → 转鞘翅滑翔 | `OmegaFlight.fallIntoGlide` |
| 滑翔判定顺序 | ① 先问 `canGlide()`（天然兼容饰品栏模组）② 否则从背包取鞘翅换到**空的**胸甲栏 ③ 都没有则直接下落 | 同上 |
| HUD | 占用经验条槽位，**仅飞行时**接管，定位条的圆点继续叠加在上面 | `OmegaFlightHud` + 两个渲染器混入 |
| 等级数字 | 原来那个等级数字改成**飞行值倒数**（600 → 0） | `GuiMixin` + `ContextualBarRenderer.renderExperienceLevel` |
| 开关 | 欧米伽升级**右侧**开关（TB 的 `IEnable`）+ **Shift+F**（键位可改，Shift 固定） | `CapacityUpgrades.Omega implements IEnable`、`OmegaFlightTogglePayload`、`KeyboardHandlerMixin` |
| 关开关 | 飞行值**保留** | 不受开关影响 |
| 存储 | **按玩家 UUID 存在玩家的存档里**（换背包不丢、退出游戏也不丢） | Fabric `AttachmentRegistry` 持久化附件，见下面第 10 条 |
| 重进 | 退出时在空中飞着 → 重进**还在空中飞着**（和原版创造模式一致），飞行值接着扣 | `abilities.flying` 原版就会存；由 JOIN 时的归属判断放行，见下面第 11 条 |

**关键实现事实（都是这轮查字节码确认的，别再猜）**：

1. **TB 的 `IEnable` 是纯标记接口**，两个方法全是 default：
   - `isEnabled(UpgradeBase)` → 读 `dataHolderStack.getOrDefault(ModDataComponents.UPGRADE_ENABLED, true)`
   - `setEnabled(boolean)` → **空实现**
   - 所以「让欧米伽升级带右侧开关」= 只让它的 upgrade 类 `implements IEnable`，不用写任何 widget / 点击 / 存储代码。状态天然存在升级物品自己的 `UPGRADE_ENABLED` 组件里（跟着物品走，两端都看得到）。
2. **右侧开关的位置是 TB 算的**：`UpgradeWidgetBase.enableElement = (tabSize.x + 6, 6)`，尺寸 `(4, 13)`，绘制/命中测试都以 widget 的 `pos` 为原点 → 挂在 24×24 升级图标**右边 6 像素**处。
3. **开关点击链路**：`enableButtonMouseClicked` → `ServerboundActionTagPacket.create(dataHolderSlot, !enabled, 1)` → 服务端 `tableswitch tag 0` → `ServerActions.modifyUpgradeTab(player, slot, value, 1, Arg3默认true)` → `fromScreen=true` 分支要求 `player.containerMenu instanceof BackpackBaseMenu`（所以只有开着背包界面才生效，正好）→ 写组件 + 对活的 upgrade 对象调 `setEnabled`。
4. **`mayfly` 是所有人共用的一个标志**。原来的 `OmegaFlight` 在「不激活」时**无条件**收回 `mayfly`，会把金苹果（`FlightHandler` + `CREATIVE_FLIGHT` 效果）给的创造飞行每 tick 打掉。现在两边都按「只收回自己发出去的」来写（各自一个 `GRANTED` 集合），并且欧米伽在玩家身上有苹果效果时**完全让位**（不消耗也不收回）。
5. **1.21.11 没有 `Gui.renderExperienceBar` 了**。经验条改成了「情境条」`ContextualBarRenderer`，经验条 / 定位条 / 载具跳跃条**共用同一条 182×5 的槽位**，同一时刻只显示一个：
   - `Gui.nextContextualInfoState()` 决定选谁：有路径点且刚获得经验(100 tick 内) → EXPERIENCE；有路径点 → LOCATOR；**单人档没路径点 → 永远 EXPERIENCE**（所以单人档看不到定位条）
   - 每个渲染器分成两半：`renderBackground()` 画底条，`render()` 画内容（**定位条的圆点和箭头全在 `render()` 里，不带背景**）
   - 等级数字由 `Gui` 自己在两次调用**之间**画（`ContextualBarRenderer.renderExperienceLevel`），后面第 6 条讲怎么抢它
   - 因此 HUD 的做法：**两个渲染器的 `renderBackground` 都注入 + cancel**，改成画飞行值；**不碰 `render`**，定位条的圆点就自动叠在飞行条上面 —— 这就是「占用经验条但不遮挡玩家位置显示」
   - **等级数字要一起抢**（用户第二轮要求）：`Gui.renderHotbarAndDecorations` 的顺序是 `renderBackground()` → 画等级数字 → `render()`，所以不处理的话原版等级数字会直接压在飞行条上。做法是：`OmegaFlightHud.render()` 里用**原版自己的** `ContextualBarRenderer.renderExperienceLevel(graphics, font, 剩余飞行值)` 画（位置、描边、字体全都跟原版一模一样），同时 `GuiMixin` 把 `Gui.renderHotbarAndDecorations` 里那句 `MultiPlayerGameMode.hasExperience()` 拦掉返回 false。
     - 为什么拦 `hasExperience()` 而不是拦 `renderExperienceLevel` 那一句：等级数字外面套了 `hasExperience() && experienceLevel > 0` 两层判断，只换绘制调用的话**玩家等级为 0 时整个数字根本不会画**，而飞行时等级为 0 恰恰是最常见的情况。拦 `hasExperience()` 一次就把两层都跳过，而且它在 `renderHotbarAndDecorations` 里**只被调用这一次**（可直接 `@Redirect`）。
6. 原版经验条两张贴图：`hud/experience_bar_background`、`hud/experience_bar_progress`（`Identifier.withDefaultNamespace`）。底条用 `blitSprite(RenderPipelines.GUI_TEXTURED, 贴图, left, top, WIDTH, HEIGHT)`，填充用 `graphics.fill(left+1, top+1, left+1+w, top+HEIGHT-1, colour)` 画在贴图的 1 像素边框里面。**`RenderPipelines` 在 `net.minecraft.client.renderer`**，不在 `com.mojang.blaze3d.pipeline`（编译报错过）。
   - `ContextualBarRenderer`：`WIDTH = 182`、`HEIGHT = 5`、`MARGIN_BOTTOM = 24`；`left(窗口) = (guiWidth-182)/2`，`top(窗口) = guiHeight-24-5`
   - `renderExperienceLevel(guiGraphics, font, int)` 是**公开静态**的，内部是 `Component.translatable("gui.experience.level", 数字)` + 居中 + 描边，想画同款数字直接调它
   - 数字画在**条的上方**（`guiHeight-35`），定位条圆点在条上，两者不会打架
7. **`ClientPlayNetworking.send` 不检查通道是否注册**，直接发包。在没装本模组的服务器上按 Shift+F 会被原版以「Unregistered custom payload channel」踢下线 → 必须先 `ClientPlayNetworking.canSend(TYPE)`。
8. 飞行值同步：服务端每 tick 算完，只在 `(charge, max, flying)` 变化超过阈值（`SYNC_EPSILON = 0.25`，远小于一个像素）才发 `OmegaFlightStatePayload`。开关翻转和起飞/降落改的是 `flying`，所以立即到达。
9. `KeyMapping.Category.register(Identifier)` 自己建分类；lang 键是 `key.category.<namespace>.<path>`。
10. **飞行值存档用 Fabric 的 data attachment**（用户报的 bug：退出游戏飞行值重置回 600）。原来是个 `HashMap<UUID, Double>` 并在 `DISCONNECT` 里清掉，等于每次退出都白送一箱油。
    - 写法：`AttachmentRegistry.<Double>builder().persistent(Codec.DOUBLE).copyOnDeath().buildAndRegister(标识符)`，读用 `player.getAttachedOrElse(CHARGE, 默认值)`，写用 `player.setAttached(CHARGE, 值)`
    - **为什么它能存下来**（查过字节码，不是猜的）：Fabric 的 `EntityMixin` 往 `Entity.saveWithoutId` 里注入 `fabric_writeAttachmentsToNbt`、往 `Entity.load` 里注入读取；而 `PlayerDataStorage.save(Player)` 正是调 `Player.saveWithoutId(...)`，`ServerPlayer`/`Player` 都没有覆盖这两个方法 → 玩家存档天然走这条路
    - `copyOnDeath()`：不加的话死亡重生时附件会丢（重生/传送/末地返回会走 `AttachmentTargetImpl.transfer`，只有标了 `copyOnDeath` 的才带过去）
    - entity 的 `fabric_markChanged` 是空实现，所以**每 tick `setAttached` 很便宜**，不用担心频繁标脏
    - 自检里做了**真存档往返**：造一个 armor stand → `setAttached` → `saveWithoutId(TagValueOutput)` → 重新 `load(TagValueInput)` → 读回来。只断言 `isPersistent()` 是没用的，那只证明声明对了，证明不了真能写进存档
11. **「退出时人在空中飞着，重进还在空中飞着」是原版行为，别再以为做不到**（第二轮的坑，我一开始答错了）。
    - 事实：`Abilities$Packed` 这个 record 里**有 `flying`**，`Player.addAdditionalSaveData` 用 `Abilities$Packed.CODEC` + `abilities.pack()` 整个存、`readAdditionalSaveData` 用 `abilities.apply(...)` 整个读；`ServerPlayer` 里**没有任何一处**写 `abilities.flying` / `mayfly`。所以创造模式退出重进确实还在空中 —— 这是原版给的，不是创造模式特判。
    - 那我们为什么不行：**是自己代码打掉的**。`FlightHandler` 的 JOIN 监听里无条件 `setFlight(player, false)`（清 `mayfly` + `flying`），在第一个 tick 之前就把刚读回来的 `flying` 抹了。
    - 修法：JOIN 时**先问一圈谁在提供飞行**再决定清不清。
      - `FlightHandler`：`if (!hasEffect && !OmegaFlight.grantsFlight(player) && canFlyOnlyByUs(player)) setFlight(false)`；`update()` 里苹果效果结束那一支也要加 `!OmegaFlight.grantsFlight(player)`，否则苹果到期瞬间会把已经接管过来的欧米伽飞行踢掉。
      - `OmegaFlight`：`grantsFlight(ServerPlayer)` 抽成 public（`配置开着 && 没有苹果效果 && 背包里有开着的欧米伽升级`）。JOIN 时如果 `grantsFlight(player) && abilities.flying`，要把玩家**收编进 `GRANTED`** —— 不然这个"我可以收回的飞行"集合是空的，之后关开关时 `release()` 不认账，玩家会挂着 0 飞行值一直悬停。
      - （**注意：这一版的"JOIN 时判断"后来被第 14 条推翻了，最终形态是"JOIN 只排队、第一个 tick 才判断"**）
    - 自检里加了一条 `vanilla saves whether the player was flying`（`Abilities.pack()`/`apply()` 往返），把"原版会存 flying"这个前提钉住，免得将来升版本时悄悄失效
12. **重进恢复飞行：不要依赖登录那一刻的状态**（第一版修法失败了，用户回「不行，还是会丢失飞行状态」）。
    - 第一版只在 `JOIN` 事件里判断「有欧米伽升级 && `abilities.flying`」，两个前提都可能不成立：JOIN 触发时背包附件**未必已经读回来**（`grantsFlight()` 要读背包，读不到就返回 false → 直接被 `FlightHandler` 当脏数据清掉）。所以登录时机依赖不得。
    - 改成**两段式、顺序无关**：
      1. 自己存一个 `WAS_FLYING` 布尔附件（`persistent` + `copyOnDeath`），每 tick 写「此刻是不是靠欧米伽飞着」。不依赖原版 `abilities` 的存档格式。
      2. `JOIN` 时只**排队**（`PENDING_RESTORE`，带 100 tick 宽限期），真正的恢复放在 tick 里：**第一个 `grantsFlight(player)` 为真的 tick** 才动手 —— 那一刻背包一定已经在了。恢复动作是 `mayfly = true` + `flying = true` + **`onUpdateAbilities()`**（这句会把能力包发给客户端，否则服务端认为在飞、客户端自己往下掉）。
    - 宽限期结束还没等到升级就放弃（防止"进游戏十分钟后才装上背包却被凭空塞回空中"）。
13. **改完这类时序相关的东西，先加临时诊断再改**。这轮在 `OmegaFlight` 里加了标着 `TEMPORARY` 的 `[flight-diag]` 日志（`diag()` / `diagTick()`）——**用户确认修好之后已经删干净了**，现在的代码里没有诊断残留。
    - 用户复现步骤固定为：进世界 → 飞起来 → 退到标题 → 再进世界
    - 删诊断后**要重新 build + runServer 自检**（这轮就是 112/112 复验过的）
14. **`ServerPlayConnectionEvents.JOIN` 触发时，玩家自己的存档数据还没读回来** —— 这条是实锤，日志里写得明明白白：
    ```
    [flight-diag] join flying=false mayfly=false ours=false ... backpack=false omega=false enabled=false pending=true
    [flight-diag] tick flying=false mayfly=false ours=true charge=565.19 ... backpack=true omega=true enabled=true pending=true
    [flight-diag] restored flying=true mayfly=true ours=true ... pending=false
    ```
    - 登录那一刻 `backpack=false`（背包没读回来）→ `grantsFlight()` 返回 false → `FlightHandler` 把 `mayfly`/`flying` 当脏数据清掉。**玩家明明有背包、也明明在飞。**
    - 第一个 tick 就 `backpack=true` 了 → **要判断"谁在提供飞行"，最早只能等到第一个 tick**。
    - 所以 `FlightHandler` 的"脏数据清理"也从 JOIN 挪到了第一个 tick（`PENDING_CHECK` 集合：JOIN 只入队，`update()` 里第一个 tick 才真正判断；苹果效果在第一个 tick 已经读回来了，日志证实 `tick mayfly=true`）。
    - 通用教训：**任何"根据玩家状态做决定"的登录逻辑都不要写在 JOIN 里**，一律延到第一个 tick。
15. **怎么从 `[flight-diag]` 一眼看出"玩家身上有苹果效果"**：字段是 `ours=grantsFlight(player)`，而 `grantsFlight = 配置 && !有苹果效果 && 背包里有开着的欧米伽`。日志里出现 `ours=false` 但 `backpack=true omega=true enabled=true`，就只剩"有苹果效果"这一种解释。这次用户报的「钻石附魔金苹果的还不可以」就是这么定位的。
16. **两个飞行来源的"重进恢复"必须做成一模一样的动作**（苹果那条最后修好的关键，第三次才修对）。对比：
    - 欧米伽（先修好的）：登录第一个 tick **强制** `mayfly = true` + `flying = true` + **`onUpdateAbilities()`**
    - 苹果（原来只是"不去清它"，保持 `flying` 原样）→ **还是掉**
    - 差别就是那句 `onUpdateAbilities()`：它把能力包发给客户端。**服务端认为你在飞、客户端认为没在飞 → 客户端自己往下掉，再把坐标发回服务端，人就下来了。** 所以「原版存档里有 `flying`」≠「客户端知道你在飞」。
17. **`WAS_FLYING` 改名 `WAS_AIRBORNE`，语义从"靠欧米伽飞着"改成"不管谁在付账，你在空中"**。原来苹果飞行时它记的是 `false`，所以苹果那条路拿不到"我是在空中退出的"这个信息。现在写的是 `player.getAbilities().flying`（原版能力本身），两个来源共用一份，并新增 `OmegaFlight.wasAirborne(ServerPlayer)` 给 `FlightHandler` 用。
    - `FlightHandler` 的 `PENDING_CHECK` 也从"一次性标记"改成**带截止 tick 的宽限**（`CHECK_GRACE_TICKS = 20`），因为效果有可能晚一两个 tick 才读回来。

## 四、已实现并验证的功能（勿回退）

- **9 种容量升级**：木×2 → 欧米伽×33554432；相乘叠加、低级叠加封顶 4096、单格上限 2147483648
- **超大堆叠**：`virtual_count` 组件承载逻辑数量（组件存在时物理数量恒为 1）、k/m/b 缩写、整理自动合并、按数量/名称排序、受单格上限约束
- **鼠标操作**：左键一组 / Shift+右键 1 个 / Shift+左键转入 / Alt+左键铺满空位 / Ctrl 丢弃 / Ctrl+Shift 丢 1024 / 中键取方块（创造）/ **Ctrl+左键两下交换槽位** / **Shift+点「提取到物品栏」只搬第一个超堆叠**
- **升级管理**：拆升级需「剩余倍率仍够用」（`CapacityHelper.canRemoveUpgrade`）、最多 3 个、可重复安装
- **欧米伽创造飞行**：见上一节（飞行值随玩家存档保存，换背包、退游戏、死亡都不重置）
- **金苹果创造飞行也会「退出时在空中→重进还在空中」**：和欧米伽走同一套恢复动作（强制 `flying` + `onUpdateAbilities()`），两个来源共用 `WAS_AIRBORNE`
- **喂食升级**：吃背包里的食物会正确扣减（`FeedingUpgradeMixin` 的 `copy()` 重定向）
- **无限流体**：下界合金及以上，水>2桶 / 岩浆>10000桶变无限；水管可吸取回收；槽位清除
- **内容**：龙鳞（复活末影龙掉落，每级抢夺独立 30% 多掉 1）、海洋之心配方、鞘翅配方（**模板不消耗**）
- **兼容**：IPN 整理按钮在背包界面不初始化；Alt+点击潜影盒放行给 Item Scroller；空潜影盒可堆叠（读 `getMaxStackSize()` 与组件较大值）
- **ModMenu 配置界面**：无限水/岩浆、金苹果创造飞行（未装 ModMenu 时改 `config/travelersbackpackextras.json`）

## 五、血泪教训（每一条都是踩过的坑，务必遵守）

### 探路方式
1. **先确认事实再动手**。鞘翅那条连错 7 轮，根因是没读 "材料消耗发生在哪一行" 就开始猜"补回的时机"。定位手段优先级：**用户对照实验 > 一行诊断日志 > 反编译字节码 > 推理**。
2. **加一行临时诊断日志是最高效的手段**。曾靠一行日志直接定位「上限读成 1」；用完即删。
3. **多模组环境要请用户做对照实验**（单独禁用某模组），比自己读十遍字节码有效。
4. 往第三方模组身上找原因之前，**先怀疑自己的代码**。

### 注入与 API
5. `@Inject` 处理器必须写**完整参数表**（不能只写前缀 + CallbackInfo），否则 `InvalidInjectionException`。
6. `@Shadow` / `@Accessor` **够不到父类的私有/受保护成员**（`CraftingMenu.craftSlots` 在 `AbstractCraftingMenu` 里 → 改用公开的 `getInputGridSlots()`）。
7. `protected` 方法要调 → 写 `@Invoker`（如 `Player.canGlide()` → `PlayerAccessor`）。
8. 可选的第三方模组混入 → 单独的**非必需混入配置**（`"required": false`）+ 注入器 `require = 0`；找不到目标就静默跳过，不要崩客户端。
9. `require = 0` 会**掩盖"钩子没挂上"**和"条件没命中"的区别 → 加一行一次性日志区分。
10. 注入点的选择要看**调用栈**：`ContinuousCraftingHandler` 的 `onTickInGame` 只是跟踪界面、`handle` 才是干活的；同类的要**两个都拦**。
11. **同一个方法可以挂多个 `@Inject`**，只要处理器方法名不同（`KeyboardHandlerMixin` 就往 `keyPress` 挂了两个）。
12. 要拿到的字段/方法在原版里是 `private` 时，**先看它有没有藏在别处的公开等价物**：`Gui` 里已经没有经验条了，别死磕。

### 旅行者背包 API（已确认可用的写法）
13. 拆升级入口：`ServerActions.removeBackpackUpgrade(ServerPlayer, int)`（`mayPickup` 那条路是死的 —— 安装后的升级槽被 TB 锁住）。
14. 取背包 wrapper：`AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.UPGRADES_ONLY.get())`。
15. 按钮命中测试：`SortingButtons.isButtonHovered(buttons.getPos(), x, y, SortingButtons.Buttons.TRANSFER_TO_PLAYER)`（**必须传组件位置**，另一个重载的 xy 是组件局部坐标）。
16. 背包升级标签是**单行字符串绘制**，`\n` 会渲染成缺字方框；TB 把收到的字符串当翻译键处理，未知键原样显示。
17. 合成材料消耗在 **`ResultSlot.onTake`** 的循环里调用 `CraftingContainer.removeItem` —— 「模板不消耗」是在这里 `@Redirect` 跳过扣除（不是事后补回）。
18. 中键被 TB 占成整理（`SORT_BACKPACK.matchesMouse`）→ 创造模式下让 `KeyMapping.matchesMouse` 返回 false。
19. 升级开关一律走 `IEnable` + `ModDataComponents.UPGRADE_ENABLED` 组件，别自己造组件、别自己写点击。
20. **改升级物品的组件要 `copy()` 之后再 `setStackInSlot`**（TB 自己也是这么干的），原地改活对象不会被同步。

### 工具与脚本（务必遵守，否则会写出坏文件）
21. **写中文/JSON 一律用 `[System.IO.File]::WriteAllText($p, $t, (New-Object System.Text.UTF8Encoding($false)))`**。PowerShell 5.1 的 `Set-Content` 默认 ANSI，会把中文写成 `?` 或乱码。
22. **改语言文件用「JSON 解析 → 改键 → 写回」**（`ConvertFrom-Json` / `Add-Member` / `ConvertTo-Json`），**当场 `ConvertFrom-Json` 校验**。
    - **补充（这轮真踩了）**：`ConvertFrom-Json` 失败是**非终止错误**，后面的 `WriteAllText` 照样会执行，于是把两个语言文件写坏了一半。**必须包在 `try/catch` 里，校验放在写入之前，并且只有校验通过才写**。
    - 追加条目时 `-join` 用 `"," + "`r`n"`，不要只 join 换行 —— 少一个逗号就是这次的事故。
    - 修坏文件：按行切开，找到锚点行的下标，切掉后面所有内容再补 `}`，先 `ConvertFrom-Json` 过一遍再往下走。
23. **批量替换前先确认换行符**：本项目源码是 **LF**，资源 JSON 是 **CRLF**，用 `.Replace("...\r\n")` 会静默失败 → 用 `[regex]::Replace` 配 `\r?\n`，或者直接按文件实际换行符来。
24. **PowerShell 双引号里的 `$` 会被当变量展开**：`"tbx$canGlide()"` 会变成 `"tbx()"` → 用单引号或反引号转义。
25. 删除代码时**别用宽泛正则**（「删掉含 `screen.travelersbackpackextras` 的行」误删了 `capacity_upgrade` 键，导致背包界面显示键名）。

## 六、用户偏好（务必遵守）

- **全程中文回复**。
- **每个新版本后面必须附上 `git` 推送指令 + Release 说明草稿**。
- 不要发布「半成品版本号」：同一个版本号只发一次，改完升号（曾因同名 jar 分不清导致用户测试了旧包）。
  - **但升号前先确认上一版到底发没发**。这轮我连着升到 1.1.1、1.1.2，用户最后说「版本号还是 1.1.0 不用改」——因为 1.1.0 压根没发布过，白升两次还多出两个容易拿错的 jar。拿不准就直接问一句。
- 不要保留调试残留（构建号、聊天提示、诊断日志）——除非明确说是临时的。
- **不要猜**：不确定就加诊断或请用户做对照实验。**涉及设计取舍（做成什么样）时，先把查到的客观事实摆出来再问**——这轮就是这么定下 HUD 方案的，很顺。
- 用户偏好：按住 Shift 比 Ctrl 方便（转移超堆叠用 Shift）。

## 七、发布流程（用户自己执行，我没有他的凭据）

```powershell
cd E:\Minecraft\MODS\TravelersBackpackExtras
git add -A
git commit -m "<版本说明>"
git push
```

- SSL 报错 → `git config --global http.sslBackend schannel`
- `dubious ownership` → `git config --global --add safe.directory E:/Minecraft/MODS/TravelersBackpackExtras`
- `fetch first` / `stale info` → `git pull --rebase origin main` 后再 push
- Release：**必须点 `Choose a tag` 创建 tag**（如 `v1.1.0`），否则报 `tag name can't be blank`；附件手动上传 `build\libs\*.jar`；`.gitignore` 已排除 `build/ run/ .gradle/ tools/`
- 仓库：https://github.com/Byx070531/travelersbackpack-extras
- 介绍页需注明**本模组由 AI 编写**（README 顶部已有该声明）

## 八、已知限制（对外说明用）

1. 拖拽分配、双击聚拢会跳过超堆叠格；不同物品无法与超堆叠格直接交换（用 Ctrl+左键交换）
2. 超大堆叠只在背包内部存在，单次取出最多 64
3. 旧版本遗留的「无升级但格子里 >64」不会被自动清理（插回任意升级即可恢复）
4. 鞘翅配方每次产出 1 个（模板不消耗，故可连续合成）
5. 飞行 HUD：多人服上飞行期间定位条的**底条**会被飞行条替换，圆点仍然叠加在上层；条上方那个数字在飞行期间变成飞行值倒数（600 → 0），降落后自动变回经验等级
6. Shift+F 只在**没开界面**时生效（开着背包时用右侧开关）；键位可在「控制」里改，但 Shift 一直要按
7. 飞行中（`abilities.flying` 为真）就算悬停不动也按 1/秒扣，落地停飞才回复
8. 飞行值数字用的是原版 `gui.experience.level` 这个翻译键（值就是 `%s`），所以数字样式/位置跟原版等级完全一致；材质包改了那个键会一起变
9. 「重进还在空中」对**欧米伽飞行**和**附魔钻石金苹果的创造飞行**都生效；两者都必须真的在退出那一刻处于飞行状态（站在地上退出、或者飞行模式开着但人在地上，重进就是站在地上）
