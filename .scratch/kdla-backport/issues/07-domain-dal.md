# 07: domain + dal 模块回合

**What to build:** domain：ApplicationContextHelp 空 catch 记日志+requireContext 明确异常；Page sameValueAs 按字段比较+pageSize<=0 防溢出。dal：IPageQuery try-parse+limit 封顶 500（防超大分页拖垮 DB）；PlusPageQuery 基本类型判 >0 替代恒真 nonNull+params 判空；SqlFilter 黑名单子串→白名单正则 ^[a-zA-Z0-9_]+$+长度上限（修 update_time 误杀与 union 漏防，SQL 注入防线关键）；EsSourceData/EsBaseData @SuperBuilder 修父字段丢失（伴生 @NoArgsConstructor）；新增 MpBaseDo（@TableLogic Integer deleted，适配 MyBatis-Plus 逻辑删除，纯增量）。

**Blocked by:** 04

**Status:** done

依据：investigation.md 第 3 节 35/49/50/54/78/81 项（9e4d29c/d7d7bfe/b3e1abf）。

- [x] domain 两项、dal 五项修复/新增落地
- [x] domain/dal pom 补 test 依赖（spring-boot-starter-test provided + mockito-junit-jupiter）
- [x] R 的 domain/dal 测试类回合
- [x] `mvn -pl kdla-component-dal -am clean compile` 与 test 绿（dal 依赖 domain 一并验证）
- [x] 独立 commit（可 domain/dal 各一），不 push

## Comments

- 2026-11-14 完成。commit 01addf6。验证：`mvn -pl kdla-component-dal -am clean test` BUILD SUCCESS，domain 2 类 8 用例 + dal 4 类 16 用例全绿（上游 model/common/exception 同跑全绿）。
- 分叉判定（硬约束 6 基线 diff）：仅 SqlFilter FORKED，分叉点单行（T indexOf(keyword)!=-1 vs R 基线 contains(keyword)，语义等价）且位于 R HEAD 已删除的黑名单循环区 → 整覆盖 R HEAD 无 T 语义损失；其余 7 文件均 T==R 基线 → 整覆盖（ApplicationContextHelp/Page/IPageQuery/PlusPageQuery/SqlFilter/EsSourceData/EsBaseData），MpBaseDo 纯新增。
- pom：dal 整覆盖 R HEAD（唯一差异=补 spring-boot-starter-test provided 块）；domain pom R HEAD==T 无需改（T 已有 starter-test+mockito-junit-jupiter）。
- 测试回合 6 类：ApplicationContextHelpTest/PageTest/IPageQueryTest/PlusPageQueryTest/SqlFilterWhitelistTest/EsSourceDataSuperBuilderTest，无 nonce 语境（已扫描），零适配。
