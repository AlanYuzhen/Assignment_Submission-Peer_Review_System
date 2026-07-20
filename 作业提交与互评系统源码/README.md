# 作业提交与互评系统

这是一个 Java Swing 桌面应用，入口类为 `com.yuzhen.assignment.App`。

## 目录结构

```text
.
├── src/main/java/com/yuzhen/assignment/  Java 源码
├── docs/                                设计文档
├── scripts/                             构建和启动脚本
├── build/                               编译输出目录
└── start.bat                            Windows 启动入口
```

## 构建

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

编译后的 `.class` 文件会输出到 `build/classes`。

## 运行

```powershell
.\start.bat
```

或者直接运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

运行数据会保存在项目根目录下的 `data` 文件夹中。
