# CHANGELOG.md 核查报告

- 核查对象：`/Users/kangll13/aiot/java-code/self/gggggggood/kdla/CHANGELOG.md`（247 行）
- 核查时间：2026-11-17
- 核查方式：git log / git rev-list / git show / git diff + 源码 grep 对照 + `/tmp/kdla-full-test.log`（未重跑 mvn）
- 结论：**FAIL 1 条，WARN 6 条**。事实主体高度可靠（抽查的 8 条 BREAKING、8 个 matchIfMissing、测试数算术全部与仓库一致），主要问题在票数统计自相矛盾、装配矩阵越位复制、以及指向未入库 `.scratch/` 的死链。

---

## FAIL（事实错误）

### F1. "13 张实现票" 与本仓票目录矛盾（实为 14 张实现票）

- 原行（L15）：`13 张实现票 + 收口验证，16 个 commit（fe53887..票14 修复）...`
- 问题：`.scratch/kdla-backport/issues/` 共 17 张票，其中票 17（`17-common-crypto.md`，Sm4/AES/Sm2 加密回合）是 2026-11-14 补立的**实现票**（Status: done，独立 commit `07ffd02`，且该 commit 在声明的 16 个之内）；实现票 = 01-13 + 17 = 14 张。L17 自己也写 "17 张票"，与 "13 张实现票 + 收口验证"（=14 张）对不上。
- 修法：改为 "14 张实现票（01-13、17）+ 3 张收口票（14-16）"。
- 验证方式：`ls /Users/kangll13/aiot/java-code/self/gggggggood/kdla/.scratch/kdla-backport/issues/`（17 个文件）；`head -20 .../issues/17-common-crypto.md`（"拆票时遗漏，2026-11-14 补立"、Status: done、"独立 commit"）；`git log --oneline` 确认 `07ffd02` 在 fe53887..8f6af69 区间内。

---

## WARN（表达 / 结构 / 职责问题）

### W1. "16 个 commit（fe53887..票14 修复）" 区间记法差一，且未覆盖后补的 P1 commit

- 原行（L15）：`16 个 commit（fe53887..票14 修复）`
- 问题：git 语义 `fe53887..8f6af69` = **15** 个（左端开区间）；含 fe53887 才是 16。verify-14.md 自己用的是正确记法 `fe53887^..HEAD`。另外该块收口后又追加了 P1 修复 commit `30490b8`（L73 有记载），本块最终态实为 17 个代码 commit，概览行未更新。
- 修法：改记法为 `fe53887^..8f6af69（含端点 16 个）`，并在 L73 处或概览注明 "另 +1 commit（P1 修复，30490b8）"。
- 验证方式：`git rev-list --count fe53887..8f6af69` → 15；`git rev-list --count fe53887~1..8f6af69` → 16；`git log --oneline fe53887~1..30490b8 | wc -l` → 18（含 docs commit 23a793a）；`.scratch/kdla-backport/verify-14.md:29`（"fe53887^..HEAD 共 14 个 commit"）与 §5.4（票 14 修复 2 个 commit：fb4cff4、8f6af69）。

### W2. 装配契约表越位：CHANGELOG 承载了 canonical 装配矩阵

- 原行（L45-56）：`### 装配契约（T 与 R 偏离，矩阵详见 .scratch/kdla-backport/investigation.md 第 11 节）` + 完整"默认开/关"两行大表。
- 问题：`docs/architecture.md` 已有 "### 装配矩阵" 章节（L114 起，含 T/R 语义相反警告 L147），CHANGELOG 复制整表形成第二真源，后续改矩阵必然漂移；时间线文档应只记 "本次改了什么" 并链接 architecture.md。
- 修法：表压缩为一句 "T 侧 9 类默认开 + 3 类无开关默认开 + 6 类默认关，矩阵真源见 docs/architecture.md#装配矩阵"，删表。
- 验证方式：`grep -n "装配矩阵" /Users/kangll13/aiot/java-code/self/gggggggood/kdla/docs/architecture.md` → L114；对照 CHANGELOG L49-52 表格内容重叠。

### W3. 两处引用 `.scratch/kdla-backport/`，该目录未入 git，冷读者死链

- 原行（L17）：`明细单一真源：.scratch/kdla-backport/（spec、17 张票、装配矩阵、评审登记 P1-P20）`；（L45）：`矩阵详见 .scratch/kdla-backport/investigation.md 第 11 节`。
- 问题：`git ls-files .scratch` 输出为空——`.scratch/` 不在版本控制内，clone 本仓的下游开发者无法访问所谓"单一真源"；且 L72 已把 nit-inventory 真源改指 `docs/nit-inventory.md`（已入库），L17/L45 仍指 scratch，自相矛盾。
- 修法：入库引用一律改指 `docs/architecture.md`、`docs/nit-inventory.md`；`.scratch` 引用删除或标注 "本地工作目录，不随仓分发"。
- 验证方式：`git -C ... ls-files .scratch | head -3` → 空；`ls docs/` → architecture.md、nit-inventory.md、project-map.md 均在；`git show --stat 23a793a` 确认 docs/ 四文件已入库。

### W4. L72 "只登记不修" 与 L73 "P1 已修" 相邻矛盾

- 原行（L72）：`R 上游同源遗留 20 项登记 docs/nit-inventory.md P1-P20（只登记不修，保两仓 diff 可核验）。`
- 问题：下一行 L73 即 "P1 已修（2026-11-16 用户裁定最小修复）"，"只登记不修" 被紧邻条目证伪，且 P1 修复恰恰制造了两仓 diff（与 "保两仓 diff 可核验" 的口径相反）。
- 修法：L72 改为 "20 项登记 docs/nit-inventory.md（默认只登记不修；P1 例外，见下）"。
- 验证方式：文本对照 CHANGELOG L72-73；`git show 30490b8 -- CHANGELOG.md` 确认 L73 为后补条目、L72 同 commit 内改写但未消除口径冲突。

### W5. "票14" 中英文空格缺失且与全文不一致

- 原行（L15）：`16 个 commit（fe53887..票14 修复）`
- 问题：全文其余处均写 "票 14"（如 L66 "票 14 评审发现"），此处 "票14" 缺空格，perl 全文扫描（汉字紧邻字母数字）唯一命中点即此处。
- 修法：改 "票 14"。
- 验证方式：`perl -CSD -ne 'while (/([0-9A-Za-z])(\p{Han})|(\p{Han})([0-9A-Za-z])/g){...}' CHANGELOG.md` → 实质命中仅 L15 "票1"（其余为标点邻接，符合排版惯例）。

### W6. 已提交的 2026-11-16 块被后一 commit 原位改写（轻微违反"已发布条目不重写"）

- 原行（L72，改写前）：`R 上游同源遗留 20 项登记 .scratch/kdla-backport/nit-inventory.md P1-P20（含 P1 ...待裁定）。`
- 问题：`23a793a` 已提交该块后，`30490b8` 直接改写既有条目（路径与措辞全换）而非追加带日期更正；P1 修复本身用了带日期条目（"2026-11-16 用户裁定"）是对的，但同 commit 顺手重写了上一行。因两块同日且均为未发布 SNAPSHOT，降级为 WARN。
- 修法：保留原条目、以 "更正（2026-11-16）：" 前缀追加新行；或接受现状但在贡献约定中明示 "同日发布前允许原位改"。
- 验证方式：`git show 30490b8 -- CHANGELOG.md`（-1/+2 diff，被删行即原 L72）。

---

## 核实通过的事实（抽查明细）

| # | 文中声明 | 验证方式 | 结果 |
|---|---|---|---|
| 1 | BaseAssert `isBlank`×3 重命名 `notBlank`、`isNull` 删除（L21/L131） | grep `kdla-component-validator/.../BaseAssert.java` → L158/164/170 三个 `notBlank` 重载 + L155 重命名注释；`public static void isNull` 零命中 | ✓ |
| 2 | Sm4Util 输出 `v1:` 前缀、CBC+随机 IV（L23/L98） | `kdla-component-common/.../Sm4Util.java:50` `V1_PREFIX = "v1:"`、L94 注释 "CBC+随机IV，输出 v1:Base64(IV+密文)" | ✓ |
| 3 | VertxHttpConfigure `kdla.http.ssl.trust-all` 默认 false + 开启 log.warn MITM；`kdla.http.request-timeout` 默认 30s（L26/L134-135） | `kdla-component-supplement/.../VertxHttpConfigure.java:48` `${kdla.http.ssl.trust-all:false}`、`:52` `${kdla.http.request-timeout:30000}`、`:80` log.warn MITM | ✓ |
| 4 | SqlFilter 白名单正则 `^[a-zA-Z0-9_]+$`、≤64 字符（L31/L125） | `kdla-component-dal/.../SqlFilter.java:19` Pattern、`:24` `MAX_COLUMN_LENGTH = 64` | ✓ |
| 5 | IPageQuery limit 封顶 500 静默钳制（L32/L126） | `kdla-component-dal/.../IPageQuery.java:22` `MAX_LIMIT = 500`、`:32-33` 超限直接钳制不抛错 | ✓ |
| 6 | MachineIdHelp init SETNX 注册成功才发布静态字段；失败走随机回退+ERROR；destroy 改 del（L30/L73/L139） | `kdla-component-supplement-cache/.../MachineIdHelp.java:92-98`（SETNX 成功分支内才赋值 machineId/dataCenterId）、`:116`（getGenerator 判空）、`:142-145`（随机回退 + log.error 撞号告警）、`:122`（del 注释） | ✓ |
| 7 | EnumValid null→TRUE、IEnumValid checkForNull 默认 false（L22/L132-133） | `EnumValid.java:61-62` `return Boolean.TRUE` + JSR-303 注释；`IEnumValid.java:43` `boolean checkForNull() default false` | ✓ |
| 8 | 装配契约表 matchIfMissing 抽查 8 键（L51-52） | Mdc `kdla.config.mdc.enabled`=true（MdcAutoConfigure.java:20-21）、StopWatch=true（:26-27）、CatchLog=true（:27）、exception.handler=true（UnifiedExceptionControllerAdvice.java:57-58）、redisson=true（RedissonAutoConfigure.java:38）、vertx-http=true（VertxHttpConfigure.java:26）→ 表中"开" ✓；request-log=false（RequestLogAutoConfigure.java:21）、cache.enabled=false（CacheAutoConfigure.java:41）→ 表中"关" ✓ | ✓ 8/8 |
| 9 | SequenceNoConfigure 三键 OR（L55） | `SequenceNoConfigure.java:27-30` `@ConditionalOnExpression` 三个 `${...:false} or` 拼接，键名逐一匹配 | ✓ |
| 10 | B1 否决：T 保留 `success` 字段（L43/L130） | `kdla-component-model/.../Response.java:20` `private boolean success` | ✓ |
| 11 | DistributeLockedAspect SpEL 改 `SimpleEvaluationContext.forReadOnlyDataBinding()`（L28/L226） | `DistributeLockedAspect.java:77` | ✓ |
| 12 | Sm2Util 新增 `generateSecureKeyPair`、旧 API @Deprecated（L24/L99） | `Sm2Util.java:68` 方法存在、`:82` Javadoc "仅为兼容既有数据保留，新场景使用 generateSecureKeyPair()" | ✓ |
| 13 | MultiResponse 新增 `totalCount` 并回填（L35/L105） | `MultiResponse.java:23-25` 字段+注释 "旧版buildSuccess(items,totalCount)曾静默丢弃该参数" | ✓ |
| 14 | 20 个 IDE 噪音文件从索引移除 + .gitignore 加固（L71） | `git show --name-status 6b7bdb9` → 20 个 `^D`（.classpath/.project/.settings/*.prefs）+ 1 个 `M .gitignore` | ✓ |
| 15 | reactor、supplement-job 零改动（L16） | `git diff --stat fe53887~1..30490b8 -- kdla-component-reactor kdla-component-supplement-job` → 空输出 | ✓ |
| 16 | "13 模块全面版"（L5） | 根 pom `<module>` 计数 = 13 | ✓ |
| 17 | nit-inventory P1-P20 共 20 项（L72） | `docs/nit-inventory.md` P 条目行计数 = 20 | ✓ |
| 18 | 测试数自洽：L15 "171 用例（64 测试类）" 为收口时点，L73 "+2 回归测试" 后现状 173/65 | `/tmp/kdla-full-test.log`：9 个模块汇总行 6+56+10+8+16+28+7+17+25 = **173**；`-- in` 行计数 = **65**；`MachineIdHelpFailPathTest` Tests run: **2**。173−2=171、65−1=64，两个数字在各自语境（收口时点 vs P1 修复后）算术自洽 | ✓ |
| 19 | 版本块时间倒序（L11/77/109/143/171/218） | 2026-11-16 → 11-13 → 10-29 → 09-04 → 08-28 → 07-10 | ✓ |
| 20 | SpringRedisHelp 23 处 printStackTrace 改 log.error（L39/L245） | `grep -c printStackTrace SpringRedisHelp.java` → 0（已全部清除，与"改 log.error"一致） | ✓ |
| 21 | advice 认证 handler 绑 spring-security AuthenticationException、DuplicateKeyException 绑 Spring DAO（L33/L91/L232） | `UnifiedExceptionControllerAdvice.java:91-92` `org.springframework.security.core.AuthenticationException`、`:26` `import org.springframework.dao.DuplicateKeyException`、`:72-73` 注释记载原误绑 SASL | ✓ |

表达面抽查：全文无 "赋能/彰显/开创性" 类营销语、无三段式排比、无模糊归因；除 W5 外中英文空格规范（数字/代码与汉字间均有空格）。

---

## 无法验证的声明（R 仓不可达）

以下声明依赖参考仓 R，本机无 R 仓 checkout，T 仓 git 对象库中对应 commit 全部 `cat-file` missing，**无法核实、不判对错**：

1. L6 "R（裁剪版 9 模块）的 **104 项修复** + 64 测试类回合至本仓"——104 这个数字与 R 模块数无本地证据。
2. L81/L89/L96/L103/L113/L123/L145/L173/L220 的 R commit 哈希：`0209b43`、`89fc933`、`b9695b2`、`e3a4acd`、`1fa3289`、`d7d7bfe`、`7d86361`、`9e4d29c`、`32ba8d2`、`9cdbe79`、`bb8022f`（验证方式：逐一 `git cat-file -t` → 全部 missing）。
3. L47 "R 侧 15 个配置类全部默认关"、L54 "同名键在 R `matchIfMissing=false`"、L130 "R 已删除 success 字段"、L73 "R 侧未修"——均为 R 仓现状断言。
4. L15 "171 用例全绿" 的**当时运行本身**（禁止重跑 mvn，只能做 W18 的算术间接印证；64 测试类与 verify-14.md:49 "9 模块 64 测试类" 记载一致）。

建议：R 侧声明在文中统一加 "（R 仓 git 为据，本仓不可达）" 标注，或补 R 仓访问路径。

---

## 汇总

- **FAIL：1**（F1 实现票数 13 vs 实际 14，与本仓票目录及自身 L17 矛盾）
- **WARN：6**（W1 区间记法差一+概览未覆盖 P1 commit；W2 装配矩阵越位复制；W3 `.scratch` 死链引用；W4 "只登记不修" 与 "P1 已修" 相邻矛盾；W5 "票14" 空格；W6 已提交条目被原位改写）
- 核实通过 21 项，无法验证 4 类（均涉 R 仓）。
