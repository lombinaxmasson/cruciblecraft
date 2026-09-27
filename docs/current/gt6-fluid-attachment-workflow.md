# GT6 流体附件移植与验收流程

这份流程记录 Funnel / Faucet 波次暴露的问题：运行时行为可以通过
GameTest，但玩家仍然可能搜不到物品、看不到中文、缺少配方、模型使用
错误贴图，或破坏粒子显示为紫黑块。因此“代码能运行”不等于“内容已完成”。

## 权威来源

按以下顺序查资料，不凭文件名或印象补齐：

1. **GT6 行为**：钉住的 GregTech 6，见 [代码树 · 参考源](code-tree.md#参考源)。
   记录相态、容量、耐酸/耐魔、点击方向、容器替换和创造模式行为。
2. **GT6 美术**：同一节钉住的 gregtech6_w。
   记录实际纹理路径、共用底图、overlay、透明度和材质染色方式。
3. **CrucibleCraft 身份目录**：
   `src/main/resources/data/cruciblecraft/mte_inplace_catalog.json`。
   记录 `(meta, kind, registry_path, runtime_id)`，不得用运行时生成物反推
   GT6 分母。
4. **波次 player-surface contract**：
   `tools/waves/content/gt6-mte-fluid-attachments-runtime/player_surface_contract.json`。
   这是审查过的数量、种类、模型几何和验收测试分母；新增种类或材料时
   必须显式更新它。
5. **美术清单**：
   `tools/waves/content/gt6-mte-fluid-attachments-runtime/art_manifest.json`。
   每条记录必须有 `source`、`gt6_source`、`destination`、`sha256`。

对照时停在参考源表里的 revision。不要把 CC 现有方块贴图当替身，
也不要为了凑资源数复制同一张 PNG。

## 标准实施顺序

### 1. 先做 GT6 census

从 Loader / MTE 类和 GT6 资源树得到完整分母，按 kind 和材料列出每一行。
至少确认：

- Faucet、Tap、Funnel、Nozzle、Cap Nozzle 的总数和 meta；
- 哪些是独立 BlockItem，哪些属于已有 host；
- 每种物品的配方入口和 raw Ceramic 前置；
- 每种模型的 render pass 数量、AABB、旋转和材质层；
- 每种附件允许的流体相态与容器交互。

缺资料时记录 `blocked`，不能拿别的材料、别的形态或单个电路物品顶位。

### 2. 先锁定身份，再写资源

每一行必须同时落到：

- MTE catalog；
- runtime overlay / 注册表；
- BlockItem 注册；
- blockstate；
- block model 和 Item model；
- 创造物品栏；
- EMI / `/give`；
- 配方输出。

“目录里有一行”不能证明物品已经注册；“能通过 EMI 找到”也不能证明
创造标签和普通玩家路径完成。

### 3. 美术必须从 GT6 源解析

对每个 kind：

1. 先读 GT6 Java 的 render pass 和坐标宏，明确 `PX_N[n]` 等坐标含义；
2. 再读 `gregtech6_w` 中源码实际引用的 PNG；
3. 按真实复用粒度复制到 `textures/**/gt6_import/`；
4. 在 art manifest 记录来源、目标和 hash；
5. 模型的每个 `textures` 引用都必须能解析到实际 PNG；
6. GT6 没有 overlay 时，CC 不得自行添加 overlay；
7. 有 tint 的层必须有 `tintindex`，粒子必须显式指向有效纹理。

Hash 检查只能证明复制的文件没有变，不能证明 AABB 或方向正确。模型仍需
做 geometry contract，最终再进行一次客户端放置/破坏截图检查。

### 4. 配方和语言从身份目录生成

配方检查的是输出物品和完整语义，不只是“存在一个 JSON”：

- 每个 catalog row 都有对应配方；
- `result.id` 必须等于该行的 `runtime_id`；
- 输入、数量、流体、电路、档位和时长对齐 GT6；
- 缺真实配料时保持 blocked，不写 stand-in。

语言检查的是实际生成的 key：

- `block.cruciblecraft.<registry_path>`；
- `item.cruciblecraft.<registry_path>`；
- `en_us` 有稳定英文名；
- `zh_cn` 必须有真实中文值，不能回退到 registry id 或英文原文。

目录中的 `chinese_name` 可以保留 GT6 继承名，但必须有明确的生成器 fallback
规则；最终 `zh_cn` 才是玩家可见验收对象。

### 5. 运行时和玩家表面分开验收

服务端行为测试覆盖相态、传输、耐酸、容器替换和创造模式消耗；它不覆盖
创造物品栏搜索、模型渲染和破坏粒子。因此必须分成两道门：

```powershell
python tools/gt6_fluid_attachment_contract.py --check
python tools/gt6_mte_inplace_runtime.py --check
python -m unittest tools.tests.test_gt6_fluid_attachment_contract
.\gradlew.bat runGameTestServer -PwaveRecipes=content/gt6-mte-fluid-attachments-runtime
.\gradlew.bat runClient
```

`gt6_fluid_attachment_contract.py` 是只读的玩家表面检查器，当前会检查：

- catalog 分母、kind 数量、meta 和 registry 唯一性；
- runtime overlay 与 catalog 对齐；
- blockstate、BlockItem model、共享模型、AABB、particle 和纹理；
- art manifest 的来源存在性、复制字节和 SHA-256；
- 47 条配方输出；
- 中英文语言 key；
- Machines 创造标签的 MTE 路由；
- 8 个流体附件 GameTest 方法。

GameTest 全绿后仍要在客户端人工确认至少一个石制、一个中阶金属和一个高阶
材料实例：创造搜索、放置方向、材质颜色、破坏粒子、EMI 配方和容器交互。
客户端截图是视觉签收，不把 headless GameTest 当成视觉证明。

## 完成闸门

只有下面五项全部通过，才可以把波次标记为完成：

1. **分母完整**：GT6 census、catalog、overlay 和 contract 数量一致；
2. **来源一致**：行为、配料、模型几何和 GT6 纹理来源有证据；
3. **资源完整**：语言、配方、blockstate、模型、Item model、粒子和纹理可解析；
4. **运行正确**：Python contract、JUnit、GameTest、重载和 EMI 检查通过；
5. **玩家可见**：客户端创造搜索、放置、破坏和截图验收通过。

任意一项失败都应报告具体 blocker，而不是把“行为没问题”扩大解释成
“这一族已经完成”。

## 维护规则

- 增加材料或 kind 时，先更新 GT6 census 和 contract，再写实现。
- 改模型时必须同时更新 art manifest、geometry contract 和对应测试。
- 改 `ModLanguageProvider`、目录或生成资源后重新跑 datagen，并检查生成
  diff；不要直接只补生成后的 JSON。
- 新增测试文件必须登记到 `tools/python_test_policy.json`。
- 不要把运行目录、崩溃日志或临时提取脚本当作验收证据提交。

