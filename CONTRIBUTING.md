# 贡献指南 / Contributing

感谢你愿意接手这个项目。原始作者可能不再维护，本仓库以 **GPL-3.0** 开源，
欢迎任何人 fork、迁移到更新的 Minecraft 版本并继续开发。

## 许可

- 本项目采用 **GNU GPL v3.0**（见 [`LICENSE`](LICENSE)），与 MineColonies / Structurize / BlockUI
  保持一致。作为 MineColonies 的衍生扩展，这是必须的：上游是 GPL-3.0，本项目也必须是 GPL-3.0。
- 每个 `.java` 源文件顶部都有 GPL 文件头。新增文件请照抄。
- 如果你 fork 并发布了修改版，**请保留这份许可与原作者署名**，并注明你做了哪些改动。

## 接手维护：迁移到更新的游戏版本

这个项目停留在 Minecraft 1.21.1 / NeoForge 21.1.259。迁移到新版时，按下面的顺序排查，
能省掉大量时间。

### 1. 先读源码，不要先反编译

三个主要依赖都是开源的，**直接读它们的仓库源码**（见 README「依赖源码」一节）。
建议把源码放在**仓库外**的目录（例如 `../refs/minecolonies`），不要提交进本仓库。

但**这些是分支 HEAD，可能比你实际安装的 jar 新**。涉及二进制兼容时，
以你本机 `cp.txt` 指向的 jar 为准。

上游仓库与分支：

| 依赖 | 仓库 | 分支 |
|---|---|---|
| MineColonies | `ldtteam/minecolonies` | `version/1.21`（新版本可能改成 `version/1.22` 之类） |
| Structurize | `ldtteam/structurize` | `version/1.21` |
| BlockUI | `ldtteam/blockui` | `version/main` |

### 2. 优先修这些「版本敏感点」

这些地方最容易在版本升级时静默失效，按优先级排查：

1. **结构包的 immutable 标记**（`CustomRoadPack`）
   Structurize 只把**可变**包发给客户端。传 `immutable=true` → 客户端永远等不到这个包 →
   `setFinishedLoading()` 永不调用 → **建房工具面板整个是空的**。
   另外 `pack.json` 的 `version` 必须随内容变化（`CustomRoadPack#contentVersion`，
   哈希时排除 `pack.json` 自身）。

2. **结构包只在进服时同步一次**
   会话中途生成的蓝图不会自动到客户端。所以蓝图 NBT + 包版本必须随 `OpenBuildToolPayload`
   **点对点**下发，客户端由 `ClientRoadPackWriter` 写入并重新 `discoverPackAtPath` 注册。
   如果新版 Structurize 改了这条机制，这里要重写。

3. **原生工具预选依赖反射**
   `ClientNativeBuildTracker` 用反射调用 Structurize 的
   `AbstractBlueprintManipulationWindow.setBlueprint`。版本变化会失效（失败时手动在 `generated` 里选）。
   建议改成上游有稳定 API 就用 API。

4. **BlockUI 的 `Window` 改名**
   新版 BlockUI 把 `Window` 改名为 `BOWindow`。升级时全局搜 `Window` 即可。

5. **`AbstractTextElement` 的颜色行为**
   XML 里只写 `color="black"` 时，enabled / disabled / hover 三色会被设成同一个值，
   于是 `setEnabled(false)` 视觉上看不出变化。相关按钮必须用
   `setColors(text, disabled, hover)` 显式给灰色（本项目用 `0x000000` / `0x9A9A9A` / `0x000000`）。

6. **原版权限判定**
   `Permissions#hasPermission(Player, Action)` 的绕过规则是「创造模式 **且** 权限等级 ≥ 2」，
   两个条件缺一不可。**不要自己加 OP 直通**——原版强制要求创造模式是有道理的。
   本项目统一走 `RoadPermissions.canManage`，语义见 README「权限」一节。

7. **控制器迁移的幂等判断**
   「是否已迁移」**只认首选目标格**。外侧旧控制器的位置恒等于第二候选格，
   一旦放宽成「任一候选格」就会短路，表现为「控制器原地不动、完全没有迁移」。

8. **完成判定必须全量比对**
   不能抽样。Complete / Pretty / Paste 与建筑工都是跨 tick 放置，
   抽样会提前把控制器塞进尚未执行的占位符格里。

### 3. 几条不可动摇的开发约定

1. **不引入 Mixin**。曾因 `specified mixin not found` 启动失败，改用登录/列表事件恢复。
2. **不删持久蓝图**。只有 Remove / Release / Cleaner 才删。
3. **控制器预留格必须是 `structurize:blocksubstitution`**，不能改回空气，
   否则建筑工会挖掉未来控制器格。
4. **迁移顺序：先放置并回读验证，再删旧**。先删后建会让任何一次放置失败都表现为「控制器消失」。
5. **完成判定全量校验**。
6. **迁移必须在全部四条建造路径上调用**：原生工具、Survival 工单、创意即时建造、Repair。
7. **不删持久蓝图 / 保留 UUID 内部索引**；外部可见标识一律用名称。
8. **不要加"玩家到控制器距离"门槛**——A/B 在路的两端，长路必然远离控制器。
9. **道路蓝图只能在"施工中"短暂写进服务端结构包**。两个方向都不能走极端：
   - **不能干脆不放**：原生建造工具的 Complete/Pretty/Paste 由服务端执行，
     `BuildToolPlacementMessage` 只发"包名 + 路径"，服务端
     `BlueprintPlacementHandling#handlePlacement` 自己从磁盘的 pack 里读
     （`StructurePacks.getBlueprintFuture(packId, blueprintPath, ...)`）。
     pack 里没有文件，放置就静默失败（`NoSuchFileException` → `Couldn't retrieve blueprint`），
     路铺不出来。**这个坑踩过一次。**
   - **不能一直放着**：Structurize 用 `zipPack` 把整个 pack 目录打包发给**每个**进服玩家，
     没有按文件过滤的入口。所以道路一变 `BUILT` 就必须删（`RoadBuildWatcher#markBuilt`
     和创意即时建造两处都要），服务端启动时再用 `RoadTemporaryPack#rebuildFromStore`
     按 `RoadBlueprintStore` 快照重建 `generated/`，只留非 `BUILT` 的。
   - 权威副本始终在 `RoadBlueprintStore`（`saves/<world>/mcreconstruction/blueprints/`），
     Repair / Remove / 完成判定都读它。
   详见 README「蓝图归属：谁能看到哪条路」。
10. **权限分两层：看和改**。`RoadPermissions#canView`（`ACCESS_HUTS`）= 读殖民地内部信息
    （道路列表、控制器面板、蓝图文件、工单列表）；`RoadPermissions#canManage`
    （`MANAGE_HUTS`）= 改变殖民地（建计划、生成蓝图、修复、拆除、取消工单）。
    改任一处都要想清楚另一处。
11. **不要用 `Permissions#getPlayers()` 当"殖民地成员"判定**。它是等级名册，不是成员表，
    里面同时装着 `NEUTRAL` 和 `HOSTILE`（攻击者会被防守方以 HOSTILE 写进去）。
    要判断玩家能做什么，只能用 `hasPermission(player, Action)` 这类能力位。
12. **不要试图新增 `Action` 枚举值**。`Action` 是外部 mod 的普通 enum，NeoForge 的
    `IExtensibleEnum` 要求目标自己实现该接口（MineColonies 没有）；而且权限界面是
    `Arrays.asList(Action.values())` 生成的，枚举外的位不会出现在界面上、无法配置。
    要加"新能力"只能用现有的位，或在 README 里写清楚为什么做不到。
13. **拒绝提示统一走 `RoadPermissions#notifyDenied`**：原版
    `com.minecolonies.coremod.permission.no` + `MessagePriority.NORMAL`（灰色），
    10 秒节流。不要自己写 `displayClientMessage` 拼中文。

### 4. 构建

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File build.ps1
```

`build.ps1` 依赖 `cp.txt`（已 gitignore，内容是本机 Minecraft libraries 的绝对路径）。
换机器需要自己生成一份：把实例 `libraries` 下的 jar 路径用 `;` 连接写进 `cp.txt`。

## 提交与分支

- 主分支 `main` 是当前开发分支。
- 建议为每个功能/修复开一个分支：`feat/xxx`、`fix/xxx`、`port/1.22`。
- 提交信息用英文或中文都可以，但请写清楚**改了什么**、**为什么**。

## 报告问题

开 issue 时请附上：

- Minecraft / NeoForge / MineColonies / Structurize / BlockUI 的**准确版本**；
- 客户端与服务端是否同一 jar（联机问题十有八九是这个）；
- `logs/latest.log` 中与 mod 相关的片段；
- 复现步骤。

## 游戏内验证的分工

原仓库约定：**代理环境不启动游戏**，游戏内功能验证由人工完成。
如果你接手并有条件跑游戏，请优先验证：

1. 四种建造方式（Complete / Pretty / Paste / Survival）都能建成并完成控制器迁移；
2. 权限不足时按钮正确置灰，且服务端仍然拒绝操作；
3. 联机时客户端的建房工具能看到 `Reconstruction Plan` 包并能选到蓝图；
4. Repair / Remove 后没有残留的控制器、锚点、蓝图文件。
