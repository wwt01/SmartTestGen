# SmartTestGen
✨ AI 驱动的智能 Java 单元测试自动生成工具 • IDEA 插件集成 • 一键生成高质量测试用例
[![GitHub stars](https://img.shields.io/github/stars/wwt01/SmartTestGen?style=flat-square)](https://github.com/wwt01/SmartTestGen)
[![GitHub forks](https://img.shields.io/github/forks/wwt01/SmartTestGen?style=flat-square)](https://github.com/wwt01/SmartTestGen)
[![GitHub license](https://img.shields.io/github/license/wwt01/SmartTestGen?style=flat-square)](https://github.com/wwt01/SmartTestGen/blob/main/LICENSE)

## 🌟 项目介绍
SmartTestGen 是一款面向 Java 开发者的智能测试用例自动生成工具，旨在解决传统单元测试编写繁琐、耗时、覆盖率低的问题。

项目通过静态代码分析 + AI 语义理解，自动解析类结构、方法入参出参、依赖关系，生成可直接编译运行的 JUnit 5 测试代码，包含：
- 自动方法调用示例
- 自动边界值测试
- 自动 Mockito 模拟依赖
- 自动生成断言逻辑
- 支持业务层、控制层、数据访问层测试

同时提供 IntelliJ IDEA 插件，无需切换工具，在编码界面右键即可一键生成测试用例，真正做到开箱即用。

## 🎯 核心特性
- 🤖 AI 智能生成：基于方法语义自动生成合理测试逻辑与断言
- 🧪 开箱即用：生成可直接运行的 JUnit 5 + Mockito 测试代码
- 🔌 IDEA 深度集成：右键生成，无需命令行与复杂配置
- 📦 低侵入性：不修改原有业务代码，不依赖运行时环境
- 📈 提升测试覆盖率：快速为遗留项目补充测试用例
- 🛠 高度可扩展：支持自定义测试模板、生成规则、命名规范

## 🧰 适用场景
- 新项目快速搭建单元测试体系
- 老项目/第三方库快速补充测试
- 提高单元测试覆盖率与代码质量
- 日常开发减少重复测试代码编写
- 团队统一测试代码风格与规范

## 📂 项目结构
```
SmartTestGen/
├── backend/          # 测试生成核心服务（代码解析 + AI 生成逻辑）
├── idea-plugin/      # IntelliJ IDEA 插件源码
├── tests/            # 项目自测用例
├── .gitignore        # 版本控制忽略文件
└── README.md         # 项目说明文档
```

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/wwt01/SmartTestGen.git
cd SmartTestGen
```
### 2. 后端核心模块部署（Python）
#### 前提条件
确保本地已安装 Python 3.8+，建议使用虚拟环境隔离依赖。

#### 步骤1：进入后端目录并创建虚拟环境
```bash
# 进入backend目录
cd backend

# 创建虚拟环境（可选，推荐）
python -m venv venv

# 激活虚拟环境
# Windows系统
venv\Scripts\activate
# Mac/Linux系统
source venv/bin/activate
```

#### 步骤2：安装依赖
后端依赖已整理在 requirements.txt 文件中，执行以下命令安装：
```bash
pip install -r requirements.txt
```

#### 步骤3：使用uvicorn运行后端服务
```bash
# 基础运行命令（默认端口8000）
uvicorn main:app --host 0.0.0.0 --port 8000

# 开发模式（自动重载，便于调试）
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
运行成功后，后端服务将在 http://localhost:8000 启动，供IDEA插件调用。

### 3. IDEA 插件安装与使用
#### 方式 1：本地编译安装
确保本地已安装 Gradle 7.0+、JDK 11+，执行以下命令编译插件：
```bash
cd idea-plugin
./gradlew buildPlugin
```
生成的插件包（zip 格式）位于：
```
idea-plugin/build/distributions/SmartTestGenPlugin.zip
```

在 IntelliJ IDEA 中安装插件：
1. 打开 IDEA，进入 `File → Settings → Plugins`
2. 点击 `Install Plugin from Disk…`，选择上述生成的 zip 包
3. 安装完成后，重启 IDEA 即可生效

#### 方式 2：插件市场安装
后续版本将上传至 JetBrains Marketplace，敬请期待。

### 4. 一键生成测试用例
1. 打开任意 Java 类（支持 Spring Boot 业务类、工具类等）
2. 右键点击类名或方法名，选择 `Generate → Smart TestGen`
3. 在弹出的配置窗口中，设置相关参数：
   - 测试类生成路径（默认与原类对应，位于 src/test/java 下）
   - 是否生成 Mock 依赖（默认开启）
   - 是否生成边界值测试（默认开启）
   - 测试框架选择（JUnit 4 / JUnit 5，默认 JUnit 5）
4. 点击「确认」，系统将自动在指定路径生成完整的测试类，可直接运行

## 📌 生成效果示例
以一个简单的 UserService 业务类为例：

```java
// 原业务代码（Spring Boot Service）
@Service
public class UserService {
    private final UserMapper userMapper;

    // 构造器注入依赖
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 业务方法：根据ID查询用户
    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户ID非法");
        }
        return userMapper.selectById(id);
    }
}
```

SmartTestGen 自动生成的测试类：

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    // Mock 依赖的 UserMapper
    @Mock
    private UserMapper userMapper;

    // 注入被测试的 UserService
    @InjectMocks
    private UserService userService;

    // 测试正常场景：ID合法且存在，返回用户
    @Test
    void getUserById_ShouldReturnUser_WhenIdIsValidAndExists() {
        // given：准备测试数据与Mock行为
        Long validUserId = 1L;
        User mockUser = new User();
        mockUser.setId(validUserId);
        mockUser.setUsername("testUser");
        when(userMapper.selectById(validUserId)).thenReturn(mockUser);

        // when：调用被测试方法
        User result = userService.getUserById(validUserId);

        // then：断言结果与Mock调用
        assertNotNull(result);
        assertEquals(validUserId, result.getId());
        assertEquals("testUser", result.getUsername());
        verify(userMapper, times(1)).selectById(validUserId);
    }

    // 测试异常场景：ID为null，抛出异常
    @Test
    void getUserById_ShouldThrowIllegalArgumentException_WhenIdIsNull() {
        // given：准备非法参数
        Long nullUserId = null;

        // when & then：断言抛出指定异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(nullUserId);
        });
        assertEquals("用户ID非法", exception.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    // 测试异常场景：ID小于等于0，抛出异常
    @Test
    void getUserById_ShouldThrowIllegalArgumentException_WhenIdIsInvalid() {
        // given：准备非法参数
        Long invalidUserId = -1L;

        // when & then：断言抛出指定异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(invalidUserId);
        });
        assertEquals("用户ID非法", exception.getMessage());
        verify(userMapper, never()).selectById(any());
    }
}
```

## 🧩 支持框架
- 测试框架：JUnit 4、JUnit 5
- 模拟框架：Mockito 3.x+
- 开发框架：Spring Boot 2.x+、Spring Boot 3.x+
- 断言库：JUnit 内置断言、AssertJ
- 辅助工具：Lombok（支持生成带 Lombok 注解类的测试用例）

## 📝 配置说明
可在 IDEA 设置中自定义测试生成规则，路径：`File → Settings → Tools → SmartTestGen`，支持以下配置：
1. 测试类命名规则（默认：原类名 + Test）
2. 生成注释风格（默认：简洁注释，支持详细注释切换）
3. 依赖导入策略（默认：自动导入所需依赖）
4. 断言库选择（JUnit 内置 / AssertJ）
5. 边界测试强度（基础 / 中等 / 全面）
6. 自定义模板路径（支持导入团队自定义测试模板）

## 🐛 常见问题
### Q1：构建 backend 模块时提示依赖缺失？
A：确保本地 Maven 配置了阿里云镜像，或手动下载缺失依赖，具体可参考 backend/pom.xml 中的依赖配置。

### Q2：IDEA 插件安装后，右键没有「Smart TestGen」选项？
A：检查 IDEA 版本是否兼容（建议 IDEA 2021.3+），重启 IDEA 后重新尝试，若仍有问题，可在 Issues 中提交问题。

### Q3：生成的测试用例无法运行？
A：检查项目是否引入了对应测试框架依赖（JUnit、Mockito），若未引入，可通过 IDEA 自动导入，或手动在 pom.xml / build.gradle 中添加依赖。

## 🤝 贡献指南
欢迎所有开发者参与贡献，共同完善 SmartTestGen，贡献步骤如下：
1. Fork 本项目（点击 GitHub 页面右上角 Fork 按钮）
2. 克隆 Fork 后的项目到本地：`git clone https://github.com/你的用户名/SmartTestGen.git`
3. 创建 feature 分支：`git checkout -b feature/xxx`（xxx 为功能描述，如 feature/add-custom-template）
4. 编写代码并提交修改：`git commit -m "Add some feature: 具体功能描述"`
5. 推送到远程分支：`git push origin feature/xxx`
6. 打开 GitHub 页面，提交 Pull Request，描述修改内容与用途，等待审核

## 📄 许可证
本项目基于 MIT License 开源，允许个人、企业自由使用、修改、分发，使用时请保留原作者信息。

详细许可证内容可查看 [LICENSE](https://github.com/wwt01/SmartTestGen/blob/main/LICENSE) 文件。

## ✨ 作者
- GitHub：[wwt01](https://github.com/wwt01)
- 初学者，专注于学习，欢迎交流。

---
如果你觉得这个工具对你有帮助，欢迎 Star ⭐ 支持一下，你的支持是我持续优化的动力！
