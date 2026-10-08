# Buck（蹶蹄子）

**为 Minecraft 添加竖半砖与不同材质的双拼台阶。**

*Buck adds vertical slabs and mixed-material slab combinations to Minecraft.*

## 功能

- 为对应 Minecraft 版本中的原版半砖提供东、西、南、北四个方向的竖半砖。
- 支持两块不同材质的半砖组合为一个方块：上下双拼、东西双拼、南北双拼。
- 可单独拆除双拼的一半，保留另一半，并掉落对应材料。
- 竖半砖与原版横半砖可以通过无序合成相互转换，支持相应的合成与切石机配方。
- 铜竖半砖和双拼中的未涂蜡铜半砖可以自然氧化；涂蜡铜不会自然氧化。
- 保留原版材质纹理及资源包适配；普通的其他模组横半砖在符合 `SlabBlock` 规则时也可参与双拼。特殊方块状态不保证兼容。

## 支持的版本

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.20.1 | ✓ | ✓ | — |
| 1.21.1 | ✓ | — | ✓ |
| 26.3 | ✓ | — | ✓ |

当前项目版本：**0.1.0**。NeoForge 26.3 已经过主要功能的游戏内调试；其他目标能构建，但不表示都已完成相同程度的运行测试。

## 安装

1. 选择与你的 **Minecraft 版本和模组加载器**对应的 Buck JAR。
2. 安装所需依赖：**Architectury API**；Fabric 版本还需要 **Fabric API**。
3. 将 JAR 放入该游戏实例的 `mods` 文件夹。不要在同一实例同时安装多个 Buck 版本。

## 从源码构建

需要 Java 与 Gradle Wrapper；具体 Java 版本由各 Minecraft 构建目标配置决定。

Windows：

```powershell
.\gradlew.bat chiseledBuild
```

Linux / macOS：

```bash
./gradlew chiseledBuild
```

各目标生成的 JAR 位于 `fabric/versions/<版本>/build/libs/`、`forge/versions/<版本>/build/libs/` 或 `neoforge/versions/<版本>/build/libs/`。

本项目使用 **Stonecutter** 管理 Minecraft 版本差异，使用 **Architectury** 共享不同加载器的实现。

## 许可证

Buck 项目源码以 [MIT License](LICENSE) 发布。

Minecraft 及其原版素材的权利归其各自权利人所有。Buck 是非官方社区模组，与 Mojang 或 Microsoft 无关联。
