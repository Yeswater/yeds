# Spring Boot 插件识别与配置方法

## 1. 适用场景

适用于 Maven 多模块工程中，IDE 无法识别某个模块为 Spring Boot 应用（例如看不到 Spring Boot 运行入口、插件面板不显示模块等）的情况。

## 2. 常见现象

- `mvn` 能正常编译，但 IDE 中该模块没有被识别为 Spring Boot 模块。
- 执行 `mvn validate` 出现类似告警：

```text
'build.plugins.plugin.version' for org.springframework.boot:spring-boot-maven-plugin is missing
```

## 3. 根因说明

当父 POM 不是 `spring-boot-starter-parent` 时，`spring-boot-maven-plugin` 的版本通常不会被自动推断。  
如果在 `pluginManagement` 或子模块 `plugins` 中引用该插件但未显式给出版本，Maven 可能仅给出告警，而 IDE 侧的 Spring Boot 识别会不稳定或失败。

## 4. 推荐配置（父 POM 统一管理）

在父 POM（例如 `apig/pom.xml`）中统一声明 Spring Boot 插件版本：

```xml
<properties>
    <spring.boot.version>3.3.5</spring.boot.version>
</properties>

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

子模块（例如 `gateway-dataplane`）继续按需声明插件即可，不需要重复写版本：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

## 5. 验证步骤

1. 在目标模块目录执行：

```bash
mvn validate
```

2. 确认不再出现 `spring-boot-maven-plugin is missing` 告警。
3. 在 IDE 中执行 Maven Reimport（重新导入项目）。
4. 若仍未恢复识别，重启 IDE 后再次 Reimport。

## 6. 项目内已落地示例

- 父 POM：`apig/pom.xml`
- 子模块：`apig/gateway-dataplane/pom.xml`

本项目已通过上述方式补齐版本，并验证 `gateway-dataplane` 模块 `mvn validate` 正常。
