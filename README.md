# 模拟殖民地：重构计划 / MineColonies: Reconstruction Plan

> [!WARNING]
> **本项目仍在开发中（in development），尚未完成测试，请勿下载安装到正式存档或正式服务器。**
>
> 当前版本 `0.1.0-beta.1` 只在开发者的单人测试环境里验证过部分流程，**已知存在未修复的问题**，
> 存档格式与内部数据结构**仍可能变动**。如果你不是打算参与开发或帮忙测试，请等正式发布。
>
> 仓库以 GPL-3.0 开源，**原作者可能不再维护**。愿意接手迁移到更新版本的开发者，
> 请看 [`CONTRIBUTING.md`](CONTRIBUTING.md)。

MineColonies 的功能扩展 mod：用 Road Wand 划出**轴向直路**，生成**严格按道路宽度**的蓝图，
交给**原生建筑工具**建造，建成后把装饰控制器迁移到道路内侧，并持久化为可修复、可拆除的道路实体。

| 项 | 值 |
|---|---|
| Mod ID | `mcreconstruction` |
| 包名 | `com.YLFXM.mcreconstruction` |
| 版本 | `0.1.0-beta.1`（开发中） |
| 定位 | MineColonies 附属扩展，**不是** Pause Research 的更新，不复用其 mod id / 包名 / 配置路径 / 合成 path 前缀 |

## 它解决什么问题

MineColonies 擅长造房子，但不擅长造**路**——而路恰恰是殖民地一切布局的骨架。这个 mod 补上这一块，
并且**不替换** MineColonies 已有的任何东西。

用 **Road Wand** 选两个点 A、B，mod 算出两点之间严格宽度的道路，生成一份真正的 Structurize 蓝图，
然后打开**原版**建造工具（Complete / Pretty / Survival / Paste）。等道路真的在世界里立起来之后，
mod 把装饰控制器搬到车行道内侧，把道路绑定到殖民地，并从此持续看管它。

玩家接触到的每一个环节都是 MineColonies 的居民、MineColonies 的工单、原生的 Structurize 蓝图。
没有第二套建造系统，没有平行经济，没有假装自己是道路的自定义方块。

## 功能一览

| 物品 | ID | 用途 |
|---|---|---|
| Road Wand | `mcreconstruction:road_wand` | 选点 A/B、实时预览、生成蓝图、打开原生建造工具 |
| Road Surveyor | `mcreconstruction:road_surveyor` | 绑定市政厅后查看该殖民地道路，逐行权限判定，多选批量释放/拆除 |
| Work Order Scroll | `mcreconstruction:work_order_scroll` | 绑定市政厅后查看该殖民地工单，多选取消；道路工单显示道路名 |

**四种原生建造方式全部可用**：Complete / Pretty / Paste 由 Structurize 执行；**Survival** 会把道路
变成殖民地工作管理器里一张**真正的工单**，由你自己的建筑工承接，用的是和原版同一套
`WorkOrderDecoration` API 与同一套 `MANAGE_HUTS` 权限校验。

创造模式物品栏里有独立分页 **「模拟殖民地：重构计划」**（`itemGroup.mcreconstruction`），
方便测试时直接取用。

## 设计理念

**扩展，而不是替换。** 这个 mod 触碰的每一套机制都属于别人：建造工具是 Structurize 的，工单是
MineColonies 的，权限系统是 MineColonies 的，控制器就是原版装饰控制器。本 mod 贡献的是**道路本身**
——几何、蓝图、持久化与生命周期。基座 mod 改了行为，本 mod 会跟着改，而不是 fork 一份。

**原生，而不是另造一套。** MineColonies 玩家不该学第二套界面语言。建造流程是原版建造工具，
卷轴用起来像原版资源卷轴，控制器就是原版控制器，权限就是原版权限。

**权限要正确，也要安静。** 殖民地里的权限关系很敏感。本 mod 原样复用原版规则、不放宽，
每次操作服务端都复核，拒绝时用**置灰控件**表达，而不是对玩家大声嚷嚷。

**数据要经得起折腾。** 修一条路代价很高。蓝图保留到玩家明确删除为止；重新规划是原地覆盖而不是
到处堆文件；拆除是一次完整的操作，而不是留下一地半删状态。

**幂等性是硬约束。** 控制器迁移永远是**先放置并回读验证、再删旧的**；"是否已迁移"的判断
只认**首选**目标格。这两条任何一条放松，都会导致经典的"控制器原地不动"或"控制器凭空消失"。

## 许可与开源

本项目以 **GNU GPL v3.0** 开源，见 [`LICENSE`](LICENSE)。

选择 GPL-3.0 而不是 MIT，是因为上游依赖全部是 GPL-3.0（MineColonies / Structurize / BlockUI），
本项目是它们的衍生扩展，必须保持同一许可。所有 `.java` 源文件顶部都有 GPL 文件头。

修改版请保留本许可与原作者署名，并注明你的改动。

### 接手维护

本项目处于**开发中**，原作者可能不再维护。如果你愿意接手、或想把它迁移到更新的 Minecraft 版本，
请先读 [`CONTRIBUTING.md`](CONTRIBUTING.md)——里面列出了最容易在版本升级时静默失效的
几个点（结构包 immutable 标记、结构包只同步一次、原生工具预选的反射调用、
BlockUI 的 `Window` → `BOWindow` 改名、`AbstractTextElement` 的颜色行为、
原版权限的绕过规则、控制器迁移的幂等判断、全量完成判定），
以及十三条不可动摇的开发约定。

也可以直接在仓库开 issue，勾选 **「迁移 / 版本移植」** 模板说明你的目标版本。

### 致谢

- **MineColonies**（`ldtteam/minecolonies`）—— 本 mod 的基础，工单系统、权限系统、装饰控制器、
  居民与建筑工机制全部来自它。
- **Structurize**（`ldtteam/structurize`）—— 蓝图与建造工具，本 mod 生成的蓝图直接交给它处理。
- **BlockUI**（`ldtteam/blockui`）—— 四个窗口的 UI 框架。
- **Domum Ornamentum** —— 风格包中的方块。

本项目是站在这些项目肩膀上的扩展，没有复制它们的代码，但思路、约定与一部分踩坑经验来自它们。

## 环境要求

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.259 |
| Java | 21 |
| MineColonies | 1.1.1403-1.21.1 |
| Structurize | 1.0.835 |
| BlockUI | 1.0.212 |
| Domum Ornamentum | 1.0.236 |

以上全部为必需依赖，本 mod 是附属扩展，**不能独立运行**。

`RoadManager` 与 `RoadPlan` 都按维度分档，多维度理论上可用，但只在主世界做过烟测。

## 玩家手册

### 合成

三件物品都在工作台上合成，中间都要放一个 **Structurize 的建筑工具**（`structurize:sceptergold`）。

**Road Wand** —— 两种镜像摆法等价：

```
铁锭 ·  ·         ·  ·  铁锭
 ·  权杖  ·   /   ·  权杖  ·
 ·   ·  木棍      木棍 ·  ·
```

**Road Surveyor** —— 同原版剪贴板布局，材料用纸：

```
木棍  权杖  木棍
木棍   纸   木棍
木棍  木棍  木棍
```

**Colony Work Order Scroll** —— 同原版资源卷轴布局，材料用纸：

```
木棍  权杖  木棍
木棍   纸   木棍
木棍   纸   木棍
```

### 建路流程

1. 放置原生 `minecolonies:decorationcontroller`，让它的 `FACING` 贴着作为锚点的方块（默认 `minecraft:stone_bricks`）。
2. 右键控制器（不潜行）→ 打开控制器面板；此时 `DecorationControllerHandler` 已经为该控制器建立了 `RoadPlan`，并按 `<殖民地名>-<道路名>` 生成唯一名称。
3. 手持 Road Wand：
   - **左键**选点 A；
   - **Shift + 左键**选点 B；
   - A/B 必须共享 x 或 z 之一，Y 取 A 的 Y（平路）。
4. 选点后立即出现**白色 AABB** 预览，范围就是严格道路宽度。
5. **右键空气** → 打开 Wand 面板：`Create Blueprint / Open Build Tool`、`Mirror`、`Clear`。宽度不可编辑（来自风格包），Mirror 翻转 `widthSide` 并实时重算预览。
6. 点 `Create Blueprint` 后服务端：校验 A/B → 构造 `RoadPlan` → 生成严格宽度蓝图 → 写入结构包 → 切包并打开原生 `WindowExtendedBuildTool`（自动预选该蓝图）。
7. 在原生工具里选 **Complete / Pretty / Survival**（或 **Paste**）。
8. `RoadBuildWatcher` 检测到全部道路方块就位后：迁移控制器到锚点内侧、状态置 `BUILT`、Wand 自动重绑、蓝图保留。

### 四种建造方式

| 方式 | 谁执行 | 材料 | 控制器迁移 |
|---|---|---|---|
| Complete | Structurize 即时放置 | 无 | 完成检测后迁移 |
| Pretty | Structurize 先清体积再放置 | 无 | 完成检测后迁移 |
| Paste | Structurize 从剪贴板粘贴 | 无 | 完成检测后迁移，另有 20 s 守护补回 |
| Survival | 殖民地建筑工按工单建造 | 需要 | 工单期间持续监视，建成后迁移 |

创意模式下，本 mod 的面板也有自己的即时建造分支（`Config.creativeInstantBuild`），完成后直接迁移。

### 拆除 / 修复 / 批量

- **控制器的 Remove**：`RoadDataCleaner` 全量清理 —— 删工单、删控制器与锚点、删 `RoadManager` 记录、删快照、删结构包里的生成蓝图、重新注册结构包。
- **控制器的 Repair**：`RoadRepair` 读快照（没有就重新生成）重放缺失方块，结束时再跑一次迁移把控制器补回。
- **Surveyor**：和工单卷轴一样**必须绑定市政厅**——`Shift + 右键市政厅` 绑定（位置写进物品 NBT），普通右键（方块或空气）打开该殖民地的道路列表；未绑定则打开空列表并提示绑定。列表显示道路名、状态、长度、控制器位置，可多选后 `Release Plans`（清计划/控制器/锚点，保留方块）或 `Remove Roads`（连方块一起清）。**逐行判定权限**：无权管理的道路仍然列出，但勾选框显示 `[-]` 且不可点、行文字变灰；`Select All` 也只选有权限的条目。
- **WorkOrder Scroll**：仿原版资源卷轴，**必须绑定市政厅**：
  - `Shift + 右键市政厅` 绑定（位置写进物品 NBT，`stacksTo(1)`）；
  - 绑定后普通右键（方块或空气）打开该殖民地的工单列表，需要该殖民地的 **`ACCESS_HUTS`**（默认领主/官员/好友）；无权限时灰字提示且不开窗；
  - 未绑定 → 打开空列表并在标题栏提示 `Not bound to a town hall - shift + right-click one to bind`；
  - 支持 `Select All` / `Clear` / `Cancel Selected Orders`，取消需要该殖民地的 **manage-huts** 权限；取消后服务端回推新列表，窗口原地刷新。
  - 工单状态列用 `order.getStage()` 拼装，**未认领或尚未进入建造阶段的单子 stage 为 null**，必须判空；单条工单读取失败只跳过该条，不能让整张列表失败。
- **控制器面板 Upgrade**：按钮存在但禁用，道路还没有等级/升级语义。

### 权限

所有「会改变殖民地」的操作都走 `RoadPermissions.canManage`，**只有两条规则，完全复用原版判定**：

1. **位置不被任何殖民地认领** → 放行。判定用 `getColonyByPosFromWorld`，它查的是**区块归属**（`ColonyUtils.getOwningColony(chunk)`），所以野外建路不受限制。
2. **殖民地 `Action.MANAGE_HUTS`** → 放行。

第二条直接调用 `Permissions#hasPermission(Player, Action)`，它自己就包含了原版的绕过：

```java
if (hasPermission(getRank(player), action)) return true;
else if (player.hasPermissions(permissioneventbypassminpermlevel /* 服务端配置，默认 2 */)
         && player.isCreative()) {
    return hasPermission(OP_RANK, action);
}
return false;
```

即**创造模式 + 权限等级 ≥ 2** 才绕过，两个条件缺一不可。

> **不要额外加"OP 直接放行"这类绕过。** 原版强制要求创造模式是有道理的：OP 也可能在正常玩生存，不该因为挂了个 OP 就能在别人的殖民地里动工。这一条此前试过、已回退。

**权限不足时按钮置灰，不弹自定义提示。** 殖民地权限系统本身会向玩家报告拒绝（原生提示已经足够），所以在我们的窗口里改成按权限禁用按钮：

| 窗口 | 按钮 | 启用条件 |
|---|---|---|
| 工单卷轴 | `Cancel Selected Orders` | 已绑定市政厅且对该殖民地有 manage-huts |
| 控制器面板 | `Repair` / `Remove` | 该道路所在殖民地的 manage-huts（`Upgrade` 始终禁用） |
| 道路勘测仪 | 每条道路的勾选框 | 该道路所在殖民地有 manage-huts 才可勾选（`[-]` 表示无权限，行文字变灰） |
| 道路勘测仪 | `Release Plans` / `Remove Roads` | 选中项里**至少有一条**可管理 |
| 道路魔杖 | `Create Blueprint` / `Mirror` | 绑定的道路可管理（`Clear` 不受限） |

判定依据是随数据一起下发的权限标记：`RoadSyncPayload` 里多一个 `manageable`（UUID 字符串列表），`WorkOrderListPayload` 多一个 `canManage`。**这些只用于界面**，服务端每次操作仍然重新校验。

右键控制器被拒时**不取消事件**，让 MineColonies 自己的处理器走原生流程和提示；其余入口只写 `LOGGER.debug`。

> **BlockUI 的坑**：XML 里只写 `color="black"` 时，`AbstractTextElement` 会把 enabled / disabled / hover 三种颜色**都设成同一个值**（`setColors(color)` → `setColors(color, color, color)`），于是 `setEnabled(false)` 的按钮看起来和启用状态一模一样。相关按钮必须在窗口构造时用 `setColors(text, disabled, hover)` 显式给灰色（我们用 `0x000000` / `0x9A9A9A` / `0x000000`）。

| 操作 | 是否需要授权 |
|---|---|
| 右键控制器创建新 `RoadPlan` | 是（已有计划可只读打开） |
| 规划 / 生成蓝图 / 打开原生建造工具 | 是 |
| 创意即时 Complete/Pretty | 是（创意只免消耗，不自动免权限；创造+OP 按原版规则放行） |
| 原生 Survival 建工单 | 是，且必须位于殖民地范围内 |
| 控制器面板 Repair / Remove | 是 |
| Surveyor 批量 Release / Remove | 是（无权限的条目跳过并计数） |
| 取消工单 | 是 |
| 放置道路锚点（`initial` 蓝图 Survival） | 是 |
| 查看工单列表 / 查看道路列表 / 打开控制器面板 | 是（`ACCESS_HUTS`） |

想在生存模式下管理别人殖民地，只能靠殖民地权限本身（把自己加进殖民地 / 提升 rank），或临时切创造模式（原版规则）。

## 核心机制

### 严格宽度与锚点

- `RoadBounds.ofRoadOnly`：只包含道路宽度与锚点，**不含**外侧控制器 —— 预览与蓝图都用它。
- `RoadBounds.ofBody`：纯铺装体，连锚点 padding 都没有 —— 只用于判断某个格子是否真的在路面内。
- 锚点是**蓝图放置原点**，同时也是 WorkOrder 的位置（`Blueprint.setCachePrimaryOffset` 指向它）。锚点方块由 `Config.roadAnchorBlock` 决定。

### 控制器与迁移

控制器是原生 `minecolonies:decorationcontroller`，本模组不再提供自定义控制器方块。

蓝图**不含控制器**，只在"锚点沿宽度轴、落在铺装体内的一格"写 `structurize:blocksubstitution` 占位符，建筑工不会挖掉这一格。

`RoadControllerMigration.migrate` 的规则：

1. 候选格 = 锚点沿宽度轴偏移一格、落在 `RoadBounds.ofBody` 内的一侧；另一侧作为放置失败的回退；
2. **先放置并回读验证**：目标格先无条件清空（占位符 / 路面方块 / 旧控制器都 `removeBlock`），再 `setBlock`，保证控制器一定占到该格；
3. 放置成功后才删除旧控制器；
4. 更新 `plan.controllerPos`，重绑在线玩家的 Wand，发 `BindRoadWandPayload`；
5. 幂等**只在首选格成立**：控制器已在首选格则直接返回，不搬运。

> **幂等判断不能放宽到"任一候选格"**。外侧旧控制器的位置恒等于 `锚点 + FACING 反向`，也就是第二候选格；一旦按"任一候选格"判定就会直接短路，表现为"旧控制器原地不动、完全没有迁移"，而沿道路方向的控制器反而正常 —— 症状看起来像是迁移方向反了。

**顺序不可颠倒** —— 先删后建会让任何一次放置失败都表现为"控制器消失"。

迁移必须在四条路径上都执行：原生工具（Complete/Pretty/Paste/Survival）、本 mod 面板的创意即时建造、Repair。

### 完成判定

`RoadBuildWatcher` 每 20 tick 检查一次：把蓝图里所有道路方块（跳过空气与 substitution 占位符、跳过锚点和控制器格）与世界上实际方块**全量比对**，比较的是方块类型而非完整 state。

不能用抽样：Complete/Pretty/Paste 与建筑工都是跨 tick 放置，提前判定会把控制器放进尚未执行完的占位符格里。

判定完成后进入 `GUARD` 守护窗口（400 tick），期间每 20 tick 确认控制器还在，被异步任务覆盖就补回。

**观察器是纯内存的，所以每次服务端启动都要重新挂接**（`RoadBuildWatcher#onServerStarted`）：
把所有处于 `PLANNED` / `BUILDING` 的道路重新 `watch()` 一遍。少了这一步，重启后只有**带殖民地工单**
的道路会被 `RoadWorkOrderRecovery` 找回来，而**用原生建造工具建的路根本没有工单**——没人盯，
完成判定永远不跑，状态停在 `PLANNED`，蓝图也永远留在 pack 里。

### 蓝图生命周期

持久生命周期（`RoadBlueprintStore` 快照）—— 建成、取消、超时**都不删**：

- 生成于 Plan 阶段；
- 只有 `Remove Road`、Surveyor 的 `Release Plan` / `Remove Roads`、`RoadDataCleaner` 会删。

这保证原生建筑工具随时可重开蓝图、Repair 有快照、拆除有完整依据。

**客户端本地副本走另一套规则**：`RoadSyncPayload.blueprints` 只列出**非 `BUILT`** 的道路，
客户端 `ClientRoadPackWriter#retainOnly` 会删掉不在名单里的文件——已建成的也会被删。
所以道路完工后，原生建造工具里不会再列出这条路的蓝图，客户端也不会一直留着一份没人用的副本。

#### 重新选取 A/B

对同一个控制器再次规划时（状态仍是 `DRAFT`/`PLANNED`）：

- `plan.setId(old.id())` 且沿用旧 `displayName`，所以 UUID 与名称都不变；
- 文件名由 `<短名>-<4位UUID哈希>` 派生 → `generated/<短名>-<hash>.blueprint` 路径不变，**原地覆盖**；
- 快照 `blueprints/road_<uuid>.blueprint` 同样按 UUID 命名，**原地覆盖**；
- 覆盖前先清掉这条路遗留的 WorkOrder：换了几何还留着旧订单，建筑工就会拿新蓝图在老锚点施工；
- 唯一会产生新文件名的情况是旧计划名称为空串（会现生成一个随机名），此时 `RoadTemporaryPack.replace` 会按旧计划先删旧文件，不留孤儿；
- 已建成（`BUILT`）的道路不允许重新选点，会提示 `already built; construction options are locked`，必须先 Remove 触发全量清理再重建。

Structurize 侧没有蓝图内容缓存（`StructurePacks.getBlueprint` 每次从磁盘读文件），所以覆盖后重新打开原生工具拿到的就是新几何。

### 名称与标识

- 内部索引始终是 `RoadPlan.id`（UUID），`RoadManager` 的 `SavedData` 名固定为 `mcreconstruction_roads`；
- 完整名称：`<殖民地名>-<随机路名>`（英文环境为 `<Colony> - Road <n>`），维度内唯一；
- 原生按钮显示短名：`<道路名>-<4 位 UUID 哈希>`，例如 `梧桐路-a1b2`；
- `Blueprint.fileName`、文件路径、WorkOrder 名称、工单卷轴显示保持一致；
- 旧 UUID path `mcreconstruction:road/<uuid>` 仍可读取，但不会自动改名。

## 工单创建（与原版对照）

原版（MineColonies）创建装饰/建筑工单的流程，见 `DecorationBuildRequestMessage#onExecute`：

1. 客户端 GUI 发消息到服务端；
2. 服务端用 `getColonyByPosFromDim(dimension, pos)` **按位置解析殖民地**；
3. 校验 `Action.MANAGE_HUTS`，没有权限直接 `return`；
4. 该位置已有同类型工单 → 删除（按钮是 toggle 语义）并 return；
5. 否则 `ServerFutureProcessor.queueBlueprint(...)` **异步**取蓝图；
6. 回调里 `WorkOrderDecoration.create(type, pack, path, name, pos, rotMir, 0)`；
7. `order.setBlueprint(blueprint, world)`；
8. `colony.getWorkManager().addWorkOrder(order, false)`。

`RoadWorkOrderService.createAndAdd` 走的是同一组 API：

- 同样的 `WorkOrderDecoration.create(...)` 工厂，同样的 `setBlueprint` + `addWorkOrder(order, false)`；
- 唯一差别是**我们不同步等蓝图**——`RoadBlueprintGenerator` 已经在内存里生成了 `Blueprint`，直接 `setBlueprint`，省掉 `ServerFutureProcessor` 那一步；
- 权限、toggle、位置解析现在也按原版补齐了。

`stage` 的语义（`AbstractWorkOrder`）：

```java
private BuildingProgressStage stage = null;   // 默认 null
public BuildingProgressStage getStage() { return stage; }   // 只由 setStage 写入
public boolean isClaimed() { return !BlockPos.ZERO.equals(claimedBy); }
```

所以 **“已认领 + stage 为 null” 是一个合法的持久状态**：某个建筑工 hut 已经认领这张单，但还没走到任何建造阶段（材料不够、路径不可达、蓝图异常都会长期停在这里）。这与创建者是谁无关，也不是创建流程与原版不一致导致的。列表 UI 必须容纳这个状态。

## 代码结构

```text
src/main/java/com/YLFXM/mcreconstruction/
  MCReconstruction.java            @Mod 入口：注册表、网络、配置、SurvivalHandler
  ModRegistry.java                 block/item/block entity 注册
  Config.java                      common 配置
  CustomRoadPack.java              每存档结构包 + initial/road.blueprint
  DecorationControllerHandler.java 原生控制器右键：建计划、绑定、开面板
  ItemRoadWand.java                Wand 物品
  ItemRoadSurveyor.java            Surveyor 物品
  ItemWorkOrderScroll.java         工单卷轴
  RoadWandHandler.java             取消 Wand 左键破坏方块
  RoadAnchorSurvivalHandler.java   initial 锚点蓝图的 ISurvivalBlueprintHandler
  RoadWorkOrderRecovery.java       登录/请求列表时从 RoadManager 恢复
  ColonyInfoAccessHandler.java     原版资源卷轴的读取权限检查
  RoadPermissions.java             权限判定（canView / canManage）
  ClientRoadRenderer.java          AABB 渲染
  network/
    Network.java                   全部 payload 的注册与处理
    ConfigureRoadPayload / OpenRoadPlanPayload / RequestRoadsPayload / RoadSyncPayload
    RoadBulkActionPayload / RoadControllerActionPayload / BindRoadWandPayload
    OpenBuildToolPayload / NativeBuildClosedPayload / WorkOrderListPayload / ClientRoadCache
  road/
    RoadPlan.java                  核心计划（NBT 持久化）
    RoadSegment.java               轴向直段 / 台阶
    RoadState.java                 DRAFT/PLANNED/BUILDING/BUILT/REMOVING/REMOVED
    SegmentKind.java               ROAD / STAIR_UP / STAIR_DOWN
    RoadManager.java               每维度 SavedData 索引
    RoadPlanBuilder.java           端点校验、长度、交叉检测
    RoadBounds.java                AABB（严格宽度 / 纯铺装体）
    RoadUnit.java / UnitTemplate.java / RoadUnitExtractor.java  风格包单元切片
    RoadBlueprintGenerator.java    严格宽度蓝图生成
    RoadTemporaryPack.java         蓝图在 pack 中的逻辑路径 + 清理旧 generated/
    RoadBlueprintStore.java        每计划蓝图快照（存档内）
    RoadBuildWatcher.java          完成检测、状态推进、迁移触发、守护
    RoadControllerMigration.java   控制器迁移
    RoadQuickBuilder.java          创意即时建造
    RoadRemover.java               按快照删方块
    RoadRepair.java                即时修复
    RoadDataCleaner.java           全量清理
    RoadNames.java                 命名、短名、唯一性
    RoadWandBinding.java           Wand 绑定（物品 NBT）
    RoadWorkOrderService.java      A 管线工单创建
    RoadPlacementMode.java         PLAN/COMPLETE/PRETTY/SURVIVAL
    RoadWorkOrderService / RoadManager ...
  client/
    WindowRoadWand.java            Wand 面板（Plan/Mirror/Clear）
    WindowRoadController.java      控制器面板（Upgrade/Repair/Remove）
    WindowRoadSurveyor.java        Surveyor 多选列表
    WindowWorkOrderList.java       工单卷轴列表
    ClientNativeBuildTracker.java  原生工具预选（反射）与关闭上报
    ClientRoadPreview.java / ClientSelectionEvents.java / ClientDecorationControllerEvents.java
    RoadSelectionClient.java
```

资源：

```text
assets/mcreconstruction/
  gui/windowroadwand.xml · windowroadcontroller.xml · windowroadsurveyor.xml · windowworkorderlist.xml
  lang/en_us.json · lang/zh_cn.json
  models/item/*.json
data/mcreconstruction/recipe/work_order_scroll.json
```

## 数据模型

`RoadPlan` 持久化字段：`id`(UUID)、`dimension`、`anchorPos`、`controllerPos`、`controllerFacing`、`mirror`、`pack`、`variant`、`displayName`、`width`、`walkWidth`、`widthSide`、`lampSpacing`、`segments`、`state`、`workOrderId`、`creator`(UUID，可空)。

状态机：

```text
DRAFT -> PLANNED -> BUILDING -> BUILT
                         \-> REMOVING -> REMOVED
```

`RoadSegment(start, length, axis, rise, kind)`：轴只能是 X 或 Z，`length >= 1`，台阶必须 1 格长。

道路长度没有最小限制（`length = |A轴-B轴| + 1`），上限是 `Config.maxRoadLength`（默认 256）。

## 蓝图管线

1. **源单元**：默认 `RoadUnit` = 风格包 `Minecolonies Original` 的 `infrastructure/roads/streets/longroad1.blueprint`，取第 0 片，宽 4、walk 宽 2、`surfaceY` 1。
2. **切片**：`RoadUnitExtractor` 从源蓝图切出一格厚度的竖向切片，跳过 `minecolonies:decorationcontroller`（控制器由道路系统自己放），保留方块实体数据。
3. **生成**：`RoadBlueprintGenerator` 按 `RoadBounds.ofRoadOnly` 决定尺寸；道路上方显式写空气（让建筑工清空），路面下方写 `blocksolidsubstitution` 作支撑填充，锚点写入蓝图，控制器格写 `blocksubstitution` 占位符。
4. **两处落盘**：
   - **结构包**（给原生工具/建筑工看）
     ```text
     saves/<world>/mcreconstruction/structurepack/
       pack.json
       initial/road.blueprint
       generated/<短名>-<4位哈希>.blueprint
     ```
     包名固定为 `Reconstruction Plan`，由 `CustomRoadPack` 每次重建 `pack.json` 并注册。
   - **快照**（给 Repair / Remove / 完成检测用）
     ```text
     saves/<world>/mcreconstruction/blueprints/road_<uuid>.blueprint
     ```

删除方块时（`RoadRemover`）只删蓝图里明确指定的非空气方块，并跳过全部 `structurize:*substitution*`。

### 结构包同步（联机必读）

客户端建房工具里的包来自三处：mod jar（immutable）、客户端 `blueprints/` 目录、**服务端同步**。联机时 Structurize 的规则是：

- 服务端 `ServerStructurePackLoader.handleClientUpdate` 只会把 **`!pack.isImmutable()`** 的包发给客户端；
- 客户端 `ClientStructurePackLoader#checkPackDifferences` 收到服务端的包列表后，如果发现「服务端有、本地没有」的包，会把 `needsChanges = true`，于是 **不会调用 `StructurePacks.setFinishedLoading()`**；
- `StructurePacks.waitUntilFinishedLoading()` 返回 false 时，`getBlueprints` / `getCategories` / `getBlueprint` 全部返回空 —— **建房工具面板里一个包都看不到**。

所以本模组每存档的结构包在 `StructurePacks.discoverPackAtPath(...)` 时的 `immutable` 参数**必须传 `false`**，否则客户端会永远等一个服务端永远不会发送的包，表现为“建筑包搜不到”。

`pack.json` 的 `version` 也必须随内容变化（见 `CustomRoadPack#contentVersion`：对 `initial/` 和 `generated/` 下文件的**名字 + 内容**做哈希，且**排除 pack.json 自身**以免自反馈）。版本不变则客户端拿到旧包后永远不会更新；版本乱变则每次加入都重下整个包。注意这里**不能**用时间戳：`install()` 每次都会重写 `initial/road.blueprint`，时间戳一变版本就跟着变，客户端会无休止重下。

### 蓝图归属：谁能看到哪条路

**这是本模组最重要的安全边界，改动前务必读完。**

Structurize 同步结构包时是 `zipPack(pack.getPath())` —— 它**逐个文件遍历整个 pack 目录并打包**，没有按文件过滤的入口，也不提供 hook（除非上 Mixin，而本项目禁用 Mixin）。所以**只要文件在服务端 pack 目录里，它就会被发给每一个进服玩家**。

道路蓝图恰恰不能公开：它暴露别人殖民地的布局，而野外道路是创建者的私事。因此：

1. **服务端 pack 里的 `generated/` 只包含"施工中"的道路**。道路一旦变成 `BUILT`，它的蓝图立刻从 pack 里删掉（`RoadBuildWatcher#markBuilt` / 创意即时建造两处）。所以进服泄漏面 = 此刻正在建造的那几条路，不是全部历史。
2. **权威副本存在 `RoadBlueprintStore`**，路径 `saves/<world>/mcreconstruction/blueprints/road_<uuid>.blueprint`，在 pack 目录**之外**，永不被 zip。Repair / Remove / 完成判定都读它，不读 pack。
3. **每次服务端启动按快照重建 `generated/`**（`RoadTemporaryPack#rebuildFromStore`）：清空目录，再把所有非 `BUILT` 道路的蓝图从快照写回去。这样即使"建成"和"删文件"之间崩过一次，已完工的蓝图也不会永远留在 pack 里。
4. **客户端按需点对点接收**：有权限的玩家点 `Create Blueprint` 时，`OpenBuildToolPayload` 携带蓝图 NBT 与包版本；客户端 `ClientRoadPackWriter` 写进**自己本地**的 `blueprints/Reconstruction Plan/generated/` 并重新注册包。
5. **客户端会清理多余的本地蓝图**：`RoadSyncPayload` 额外带一个 `blueprints` 列表（该玩家在所有维度上**有权看到的蓝图文件名**），客户端 `ClientRoadPackWriter.retainOnly` 把 `generated/` 下不在列表里的文件删掉。

> **为什么不能干脆不放进 pack**（这条踩过坑，别再试）：原生建造工具的 Complete / Pretty / Paste 由**服务端**执行，
> `BuildToolPlacementMessage` 只把"包名 + 路径"发上去，服务端 `BlueprintPlacementHandling#handlePlacement`
> 自己调 `StructurePacks.getBlueprintFuture(packId, blueprintPath, ...)` 从磁盘读。
> 客户端有没有蓝图都不影响这一步。所以文件**必须**存在于服务端已注册的 pack 里，
> 否则每次放置都静默失败（日志：`NoSuchFileException` → `Couldn't retrieve blueprint`），路就是铺不出来。
> 曾经的错误判断是"服务端只读 `RoadBlueprintStore` 快照就够了"——只覆盖了 Repair/Remove/工单恢复，
> 漏掉了原生工具这条路。

**可见性规则**（`Network#isVisibleTo` / `#isEntitledToBlueprint`）：

| 道路位置 | 列表里能看到 | 能拿到蓝图文件 |
|---|---|---|
| 殖民地内 | **`ACCESS_HUTS`**（默认 = 领主 + 官员 + 好友） | 创建者本人，或该殖民地有 **`MANAGE_HUTS`** 的人（= 领主 + 官员） |
| 野外·有主 | **仅创建者** | **仅创建者** |
| 野外·无主（`creator == null`） | 所有人 | 所有人 |

### 为什么用 `ACCESS_HUTS` 而不是自造"成员"或"新能力值"

**不要引入"殖民地成员"这个布尔量**——MineColonies 没有，而它看上去最像的那个东西是**错的**：

`IColony#getPermissions().getPlayers()` 返回的是一张**等级名册**（`Map<UUID, ColonyPlayer>`，每个条目带一个 `Rank`），不是成员表。它里面装着从 OWNER 到 **HOSTILE** 的全部玩家——玩家攻击别人殖民地时，防守方会把攻击者以 `HOSTILE_RANK_ID` 写进自己的名册（`EntityCitizen.java:1308`），权限界面加的玩家则会以 `NEUTRAL_RANK_ID` 进去（`PermissionsMessage:180/283`）。用「在名册里」当"成员"判定，会把**正在打这个殖民地的敌人**当成成员放行。这个错误本模组犯过并已修正。

**也不要试图新增一个 `Action`**——它做不成，三个硬障碍：

1. `Action` 是普通 `enum`，NeoForge 的 `IExtensibleEnum` 扩展机制要求目标枚举**自己实现该接口**，MineColonies 没有；
2. 权限界面用 `Arrays.asList(Action.values())` 生成复选框（`WindowPermissionsPage:110`），枚举之外的位**不会出现在界面上**，玩家无法授予/撤销；
3. `Rank` 只暴露 `addPermission(Action)` / `getPermissions()`，没有按位写入的 API。

于是只能用 `ACCESS_HUTS` —— 原版自己的"可以进入并使用殖民地建筑"能力位。`loadRanks()` 里它经 OWNER → OFFICER → FRIEND 的 fallthrough 授予，NEUTRAL / HOSTILE 没有，正好是"领主 + 官员 + 好友"这一档，并且可以在权限 GUI 里按 rank 调整。

**原版已经用它拦住了大部分"看殖民地内部"的入口**，我们不需要重复实现：

| 入口 | 原版拦在哪 |
|---|---|
| 右键任何小屋（含市政厅、建筑工小屋） | `ColonyPermissionEventHandler` 要求 `ACCESS_HUTS` |
| 打开市政厅 GUI | `BlockHutTownHall` 要求 `ACCESS_HUTS` |
| 请求清单 | 位于市政厅 GUI 内部，连带被拦 |
| 本模组工单卷轴绑定市政厅 | 走"右键小屋"路径，连带被拦 |

### 本模组补的三处缺口

| 位置 | 做法 |
|---|---|
| 道路列表（`sendRoads`） | 殖民地道路要求 `ACCESS_HUTS`；绑定视图同样过滤，否则绑一个市政厅就绕过了 |
| 工单卷轴列表（`sendWorkOrders`） | 要求 `ACCESS_HUTS`；拒绝时**不下发任何包**，客户端就不会开窗 |
| 原版资源卷轴（`ColonyInfoAccessHandler`） | `ItemClipboard` 的 `use`/`useOn` 原本**零检查**；在其 `PlayerInteractEvent` 上按手持物品拦截 |
| 控制器绑定与面板 | 要求 `ACCESS_HUTS`（`DecorationControllerHandler`） |

> 资源卷轴的拦截必须在**客户端和服务端都取消**：窗口是客户端自己的 `Item#use` 打开的，只拦服务端挡不住正常客户端。
>
> **限度**：MineColonies 会把工单推给 `closeSubscribers`（走近殖民地就自动加入的玩家），所以这份数据本来就在客户端内存里。我们只能做到"正常客户端打不开界面"，改过的客户端仍能读到。要真正堵住得改 `ColonyPackageManager` 的推送对象 —— 那是 Mixin，本项目不做。

### 拒绝提示

统一走 `RoadPermissions#notifyDenied`：`MessageUtils.format(TranslationConstants.PERMISSION_DENIED)`，即原版 `com.minecolonies.coremod.permission.no`，默认 `MessagePriority.NORMAL` = **灰色**。节流照抄 `ColonyPermissionEventHandler#cancelEvent`：同一玩家 10 秒内只提示一次（**不含**它那段"连续 10 次就弹漂浮"的惩罚，那个是反刷屏手段，不该因为点了个被拒的界面就把玩家浮起来）。

### 为什么野外必须留"创建者"

野外没有殖民地，就没有任何权限位可以问——这是唯一无法复用原版规则的地方。`RoadPlan.creator` 是持久化字段（NBT `creator`），在建计划时写入，重新规划 A/B 时**沿用旧值**（不能因为别人重选 A/B 就转移归属）。老存档没有这个字段，读出来是 `null`，这种**无主**道路按公开处理（列表与蓝图对所有人放开），并且被重新规划 A/B 时会自动认领给当前玩家。

### 为什么蓝图比列表严

蓝图是**逐方块的完整数据**，包含站在路上看不见的部分（地下的 `blocksolidsubstitution` 支撑填充、预留的路灯高度），而且能被放进原生建造工具复制到任何地方。列表只是元数据（名字、状态、长度、控制器坐标）。所以列表可以不设卡，蓝图不行。

> 网络层只负责「发给谁」，服务端**每次操作仍然重新校验权限**（`RoadPermissions.canManage`），不依赖客户端传来的任何标记。

**结构包只在进服时同步一次**（`ClientStructurePackLoader` 的 `onWorldTick` 在 `FINISHED_LOADING → SYNCING` 只走一遍）。所以会话中途生成的蓝图**不会**自动出现在客户端，原生工具会报找不到蓝图。这就是上面第 3 条点对点下发存在的原因。`pack.json` 同步写成服务端当时的版本号，避免下次进服时因版本不一致被整包删除重下。


## 网络协议

| Payload | 方向 | 作用 |
|---|---|---|
| `ConfigureRoadPayload` | C→S | A/B、模式、镜像 |
| `OpenRoadPlanPayload` | S→C | 打开控制器面板 |
| `RequestRoadsPayload` / `RoadSyncPayload` | C→S / S→C | 道路列表同步（`RoadSyncPayload` 里带 `bound` / `colony` / `manageable` / `blueprints`） |
| `RoadBulkActionPayload` | C→S | 批量 Release / Remove |
| `RoadControllerActionPayload` | C→S | Upgrade / Repair / Remove |
| `BindRoadWandPayload` | S→C | 迁移后通知客户端重绑 |
| `OpenBuildToolPayload` | S→C | 切包 + 打开原生工具 + 预选路径，**并携带蓝图 NBT 与包版本** |
| `NativeBuildClosedPayload` | C→S | 原生工具关闭 |
| `WorkOrderListPayload` | S→C | 工单卷轴数据（含 `bound` 标记，未绑定时客户端显示提示而不是空白） |
| `CancelWorkOrdersPayload` | C→S | 取消选中的工单（按 colonyId + orderIds） |

## 配置

`config/mcreconstruction-common.toml`：

| 项 | 默认 | 说明 |
|---|---|---|
| `roadFillBlock` | `minecraft:stone` | 支撑填充的兜底方块 |
| `roadAnchorBlock` | `minecraft:stone_bricks` | 锚点方块，也是控制器 `FACING` 指向的目标 |
| `lampSpacing` | 4 | 路灯间距（当前只存储、不生成） |
| `defaultLampReserveHeight` | 6 | 预览与蓝图预留的路灯高度 |
| `maxRoadLength` | 256 | 单段道路最大长度 |
| `creativeInstantBuild` | true | 创意模式允许面板即时建造 |
| `debugLog` | false | 打开后完成检测会打印第一处不匹配的坐标 |

## 构建

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File build.ps1
```

输出 `dist/mcreconstruction-<版本>.jar`，版本取自 `neoforge.mods.toml`。

`build.ps1` 行为：

- classpath 来自 `cp.txt`（**未入库**，需要自己生成：把 Minecraft 实例 `libraries` 下的 jar 路径用 `;` 连接写进去）；
- Java 运行时按 `tools\jre` → `%APPDATA%\.minecraft\runtime\java-runtime-delta` → `JAVA_HOME` 顺序探测，取第一个能跑 `java -version` 的；
- 镜像里没有 `javac.exe` / `jar.exe` 时分别走 `java -m jdk.compiler/...` 与 `java -m jdk.jartool/...`。

安装：把 jar 复制进实例 `mods/`。

```powershell
Copy-Item -LiteralPath 'dist\mcreconstruction-0.1.0-beta.1.jar' `
  -Destination '<你的实例>\mods\mcreconstruction-0.1.0-beta.1.jar' -Force
```

## 本地测试服务器

仓库外另有一个 NeoForge 21.1.259 / Minecraft 1.21.1 专用服务器用于联机测试。

- **安装方式**：`maven.neoforged.net` 直连不通时，installer 可从 BMCLAPI 镜像取
  `https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge/21.1.259/neoforge-21.1.259-installer.jar`；
  先把客户端 `.minecraft\libraries` 整个预置到 `server\libraries`，再 `--installServer`，这样它只需要从 Mojang 补服务端 jar 和官方库。
- **mods/**：与客户端实例保持一致，**两端必须同一个 jar**。
- **server.properties**：`server-port=25565`、`max-tick-time=-1`、`view-distance=8`、`spawn-protection=0`。
- **eula.txt**：需自行写 `eula=true`（代表已接受 Mojang EULA）。
- **OP**：控制台执行 `op <玩家名>`。离线模式（`online-mode=false`）下 UUID 由名字推导，换名字等于换人。
- **已知无害日志**：`BOWindow for invalid dist DEDICATED_SERVER`（某个 mod 的 mixin 指向 BlockUI 的客户端类）、`SSLHandshakeException`（NeoForge 版本检查访问 GitHub 失败），都不影响启动。

## 依赖源码

**查依赖 API 先读上游源码，不要一上来就反编译 jar。** 三个主要依赖都是开源的，直接看它们的仓库：

| 依赖 | 仓库 | 分支 |
|---|---|---|
| MineColonies | [`ldtteam/minecolonies`](https://github.com/ldtteam/minecolonies) | `version/1.21`（更新版本可能改为 `version/1.22` 等） |
| Structurize | [`ldtteam/structurize`](https://github.com/ldtteam/structurize) | `version/1.21` |
| BlockUI | [`ldtteam/blockui`](https://github.com/ldtteam/blockui) | `version/main` |

下载源码（zip 方式）：

```bash
curl -L -o minecolonies.zip \
  "https://github.com/ldtteam/minecolonies/archive/refs/heads/version/1.21.zip"
unzip -q minecolonies.zip -d minecolonies
```

也可以用 GitHub 的 Code → Download ZIP，或 `git clone --depth 1 -b version/1.21`。

- 建议把源码放在**仓库外**的目录（不要提交进来），例如 `../refs/minecolonies`。
- 分支是 **HEAD**，可能比你实际安装的版本略新（例：minecolonies `version/1.21` 的 `gradle.properties` 里 blockui 依赖写的是 1.0.199，而实际装的是 1.0.212）。查行为、查签名够用，**涉及二进制兼容时以你本机 `cp.txt` 指向的 jar 为准**。
- BlockUI 在新版本里把 `Window` 改名为 `BOWindow`。
- Domum Ornamentum 不必下载：本 mod 只用到它的方块（通过风格包蓝图），不引用其类。


## 已知限制

**整体状态：开发中。** 以下功能未完成或仅有部分实现：

- 路灯只预留高度，不生成；`lampSpacing` 不参与生成。
- 台阶 `SegmentKind.STAIR_UP/STAIR_DOWN` 已定义，但生成器只处理 `ROAD`。
- 交叉口只做几何检测与覆盖，没有专门铺装和路灯协调。
- Repair 是服务端即时修复，不是建筑工任务；Upgrade 按钮禁用。
- 名称唯一性是**维度内**全局，不是严格"同殖民地内"。
- 宽度 1 的道路几何上无法把控制器放进铺装体，会退化为相邻格。
- 专用服务器：结构包本体在服务端存档目录，客户端不下载整包；蓝图改为随 `OpenBuildToolPayload` 点对点下发（见「结构包同步」）。
- 自动预选依赖反射调用 Structurize 的 `AbstractBlueprintManipulationWindow.setBlueprint`，Structurize 版本变化可能失效（失败时手动在 `generated` 里选）。
- README 之外没有正式 wiki；英文文案仍有中英混杂。
- 游戏内验证由人工完成，**尚未做完整的回归测试**；存档格式仍可能变动。

## 开发状态

**当前版本 `0.1.0-beta.1` 是开发中版本，不适合正式使用。**

已实现并验证过的：

- Road Wand 选点 → 严格宽度蓝图 → 原生建造工具（Complete / Pretty / Paste）→ 控制器迁移；
- Survival 工单路径：道路成为殖民地建筑工的真工单，建成后迁移；
- 控制器面板的 Repair / Remove，Surveyor 的批量释放/拆除，工单卷轴的多选取消；
- 权限：完全复用 MineColonies 原版判定，界面按权限逐行置灰；
- 联机：结构包注册 + 蓝图点对点下发。

未完成或未充分验证的：

- 路灯、台阶、交叉口（见「已知限制」）；
- 完整回归测试，存档格式稳定性；
- 英文本地化（`en_us.json` 内容仍是中文）；
- **蓝图归属模型（`RoadPlan.creator` + 客户端蓝图清理）刚落地，尚未做多人联机实测**。
  已验证的是编译通过与服务端逻辑，未验证的是：两名玩家在同一服务器上互相看不到对方
  的野外道路、失权后客户端蓝图确实被清掉、旧存档的 `generated/` 确实被清除。

## 路线图

**P1 严格原生流程收口**（进行中）：Survival 建单验证、WorkOrder 显示名统一、控制器迁移边界、预选回退提示。

**P2 路灯/台阶/交叉口**：`lampunit` 提取与放置、`lampSpacing` 参与生成、建筑工把锚点替换为路灯、`stairunit` 的 rise/walkWidth、交叉口连续铺装与路灯避让、预览按高度更新。

**P3 地形/装饰/寻路/联机**：custom dig/fill、装饰控制扩展、Pathfinding Edition 联动、专用服务器结构包同步、Surveyor 过滤/分页/统计、自定义模型、完整本地化、旧代码清理。

## 开发约定

1. **不引入 Mixin**。曾因 `specified mixin not found` 启动失败，改为登录/列表事件恢复；要恢复必须先解决启动兼容。
2. **不删持久蓝图**，只有 Remove / Release / Cleaner 才删。
3. **控制器预留格必须是 `structurize:blocksubstitution`**，不能改回空气，否则建筑工会挖掉未来控制器格。
4. **原生按钮名用短名 + 4 位哈希**，不用完整殖民地名，也不退回 UUID。
5. **UUID 保留为内部索引**，外部可见标识一律用名称。
6. **迁移先放置验证、再删旧**，不允许先删后建。
7. **完成判定必须全量校验**，不能抽样。
8. **迁移必须在全部四条建造路径上调用**：原生工具、Survival 工单、创意即时建造、Repair。
9. 构建后必须复制 jar 到实例 `mods/`；代理环境不启动游戏，游戏内验证由人工完成。
10. 查 MineColonies / Structurize / BlockUI 的 API 先读上游仓库的源码（见「依赖源码」一节），不要一上来就反编译 jar。
