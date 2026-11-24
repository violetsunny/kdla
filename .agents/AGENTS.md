# kdla

`top.kdla.framework:kdla-components` — 多模块 Maven 框架库（非应用），发布到公司私有 Nexus（nexus.enncloud.cn）供下游应用依赖。无入口类、无 mvnw（用系统 `mvn`）。当前检出 `2.0.0-SNAPSHOT` 分支。

## 分支 = 技术栈版本线（动手前必查）

| 分支 | JDK | Spring Boot | Spring Cloud | 状态 |
|---|---|---|---|---|
| `1.0.1-SNAPSHOT` | 8 | 2.7.18 | 2021.0.9 | 可编译，有测试 |
| `2.0.0-SNAPSHOT`（当前） | 21 | 3.4.0 | 2024.0.1 | 迁移进行中，编译不过（见下） |

- `main` 分支只有 .gitignore/LICENSE/README，无 pom；实际开发在版本名分支上进行。
- 勿假设 JDK/Spring 版本：先看当前分支根 pom 的 `java.version` / `spring.boot.version`。
- 本机 JDK 在 `/Library/Java/JavaVirtualMachines` 下：`jdk-1.8.jdk`(8u491)、`adoptopenjdk-8.jdk`(8u292)、`jdk-17.jdk`、`jdk-21.jdk`；默认 `java` 是 21。
- 1.x 分支禁用 JDK 9+ 语法；默认 JDK 21 可直接编译该分支（target 1.8，已验证通过）。真实 JDK 8 编译（已验证通过）：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home mvn clean compile`。
- 勿用 `/usr/libexec/java_home -v 1.8`：它解析到 `JavaAppletPlugin.plugin`（无 `bin/javac`），必须写显式路径。

## 2.0.0-SNAPSHOT 分支当前编译不过（2026-11 验证）

按顺序三道坎：

1. `kdla-component-supplement-trdplatformcloud` 与 `kdla-component-supplement-job-application` 的 parent 仍钉 `1.0.0-SNAPSHOT`（1.0.0 合并遗留），不可解析 → 根 reactor 解析 POM 即失败，`-pl`/`-am` 无法绕过（Maven 仍解析全部模块 POM）。修复 = 两个 pom 第 9 行 parent version 改 `2.0.0-SNAPSHOT`（其余 13 模块已同步）。
2. 修复后 `mvn clean compile` 走到 `kdla-component-exception` 编译失败：`UnifiedExceptionControllerAdvice.java:198` 用了 `WebRequest` 但缺 `import org.springframework.web.context.request.WebRequest`（jakarta 迁移漏网，一行修复）。
3. model / reactor / common 可编译；exception 之后的模块（domain/dal/log/validator/supplement-*）未验证。
- 本机无 `~/.m2/settings.xml`（无 Nexus 凭证），且 Maven 3.8+ 默认 http-blocker 拦截 `http://nexus.enncloud.cn` → 远程解析与 `mvn deploy` 均不可用；`~/.m2` 里也没有任何 2.0.0-SNAPSHOT kdla 构件（只有失败下载标记）。发版走 DevOps。

## 常用命令

- 全量编译：`mvn clean compile`
- 单模块（连同其仓库内依赖）：`mvn -pl kdla-component-model -am clean compile`
- 单模块测试（仅 1.x 分支有测试）：`mvn -pl kdla-component-common -am test`（JDK 8 与默认 JDK 21 均验证通过）

`-am` 不能省：GAV `top.kdla.framework:*` 与本地另一仓库共享，`~/.m2` 里的 SNAPSHOT 可能是被后者 install 的陈旧产物，不带 `-am` 会解析到旧构件报 `NoSuchMethodError`。

## 模块依赖方向（改 pom 前看，防成环）

`model`、`reactor` 零内部依赖 → `common` → `exception` → `domain` → `dal` / `log` / `validator` / `supplement-*`。聚合最多的是 `supplement-trdplatformcloud`（8 个内部依赖）和 `supplement-job`（6 个）。`supplement-job-application` 是叶子应用：内嵌 xxl-job-admin 源码，含 main 类 `com/xxl/job/admin/XxlJobAdminApplication`，自带 spring-boot-starter-parent BOM 与私有版本号，是"版本集中在根 pom"约定的唯一例外。给底层模块（model/reactor/common/exception）加内部依赖会成环。

## 版本与提交约定

- 版本号硬编码：根 pom `<version>` 与 `kdla.framework.version` property、13 个模块 pom 的 parent `<version>`（另 2 个见上），升版本需全部同步（无 flatten/`${revision}`）。
- 提交信息风格：`kdla <version> and upgrade <topic>`。

## 框架约定

- 2.x：Spring Boot 3 忽略 `spring.factories` 里的 `EnableAutoConfiguration` 注册（9 个模块仍留有该文件，条目多数已注释或失效）。新增 auto-config 必须建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（README 已记载该方式；cache 模块已有该文件，含 KRedissonAutoConfigure / KRedisSetNxLockAutoConfigure / CacheAutoConfigure / SequenceNoConfigure 四行，其余模块尚无）；只改 spring.factories 无效。1.x 仍用 spring.factories（9 个模块）。
- 模块间依赖版本统一由根 pom `dependencyManagement` 管理（`${project.version}`），模块 pom 内不写版本号。
- 第三方版本集中在根 pom `<properties>`。1.x 刻意 pin：`snakeyaml 1.33` 与 `spring-kafka 2.9.12`（绕过 boot 2.7.14 的 bug，pom 内有注释），勿随 BOM 升级；2.x 根 pom 则给所有模块强注 snakeyaml 2.3（compile，非 provided）。
- 几乎所有第三方依赖都是 `<scope>provided</scope>`，避免向下游应用传递强依赖；新增依赖默认评估是否 provided。例外：`model`/`reactor` 无第三方依赖，`job-application` 是应用不 provided。

## 测试（仅 1.x 分支有，2.x 无任何测试）

- 1.x 有 9 个模块带 `src/test`（测试文件数：common 23、cache 11、log 10、validator 6、dal 4、supplement 4、exception 3、domain 2、model 2）；2.x 分支零测试。
- 栈：JUnit 5 + Mockito（`mockito-junit-jupiter` 4.6.1），经 provided 作用域 `spring-boot-starter-test` 引入，不向下游传递。
- 约定：bugfix / 非平凡改动提交须带回归测试（git log 常见 "with regression tests"）。

## 未纳管目录（勿误提交）

`docs/`、`test-run/`、`.scratch/`、`.agents/` 均为 git 未纳管的本地内容。
