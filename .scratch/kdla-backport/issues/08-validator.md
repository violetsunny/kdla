# 08: validator 模块回合

**What to build:** EnumValid null 值校验 FALSE→TRUE（对齐 JSR-303，BREAKING B3）+方法按可赋值性匹配；IEnumValid.checkForNull 默认 false+Objects.equals 防 NPE（B4）；DBLengthValid BigDecimal 全程比较修 pow 溢出+JUL→SLF4J；ObjectValidator 删 sun.misc.Unsafe、排除列表 isAssignableFrom、深度 16+IdentityHashMap 防环、遍历父类字段；CommonValidator 删 Locale.setDefault 全局副作用。装配：ValidatorAutoConfigure 新注册 CommonValidator（COBM+kdla.config.validator.enabled 默认开，矩阵 11.2 独立无副作用）。pom：补 slf4j-api(provided)；按「保持 T 依赖策略、只补缺失」原则核对 property 命名与 logback 依赖（103 项，不照搬 R 的删除动作）。

**Blocked by:** 07

**Status:** done

依据：investigation.md 第 3 节 27/51/52/53/55/85/100 项、第 11.2 节矩阵、无需裁定第 6 条。

- [x] 五个校验类修复落地（B3/B4 BREAKING 已裁定接受）
- [x] ValidatorAutoConfigure 注册且默认开
- [x] pom 补 slf4j-api(provided)，其余依赖策略保持 T 现状
- [x] R 的 validator 测试类回合
- [x] `mvn -pl kdla-component-validator -am clean compile` 与 test 绿
- [x] 独立 commit，不 push

## Comments

- 2026-11-14 完成。commit bba7f03。验证：`mvn -pl kdla-component-validator -am clean test` BUILD SUCCESS，validator 6 测试类 28 用例全绿（上游同跑全绿）。
- 分叉判定（硬约束 6 基线 diff）：11 个 main 文件全部 T==R 基线，零分叉 → R-CHANGED 6 文件（EnumValid/IEnumValid/DBLengthValid/ObjectValidator/CommonValidator/BaseAssert）整覆盖 R HEAD；ValidatorAutoConfigure+spring.factories 纯新增。
- 装配：ValidatorAutoConfigure 按矩阵 11.2 改 matchIfMissing=true（R 为 false，独立无副作用默认开），javadoc 同步改"默认开启,可显式关闭"。
- pom 按无需裁定第 6 条「保持 T 依赖策略、只补缺失」：补 slf4j-api(provided)（DBLengthValid JUL→SLF4J 需要）+ spring-boot-starter-test(provided，排除 slf4j-api，同 R)；保留 T 的 logback-classic/core、fluent-validator-spring.version property 命名与显式版本号，不照搬 R 的删除/更名动作（103 项）。
- 测试回合 6 类零适配（无 nonce 语境，已扫描）。B3/B4 BREAKING 已裁定接受，票 15 CHANGELOG 需记录（EnumValid null 校验 FALSE→TRUE、IEnumValid checkForNull 默认 false）。
