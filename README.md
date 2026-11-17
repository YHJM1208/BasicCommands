# BasicCommands

轻量基础命令插件：**家、地标、TPA/TPH、出生点、返回上一点、随机传送**。
无数据库、无前置插件，丢一个 jar 进 `plugins/` 就能用；六个包对应六条版本线，从 1.7.10 一直覆盖到 1.21.11。

- **六个功能总开关**：改一行配置就能整组关掉某个功能，命令不消失，`/basiccmd reload` 生效
- **热重载**：改配置、换语言不用重启服务器
- **中英双语**：`zh_CN` / `en_US` 现成可用
- **老服可用**：1.7.10–1.18.1 的包跑在 Java 8 上
- **数据平铺**：`homes.yml` / `warps.yml` / `back.yml`，换包不用迁移

## 支持范围

| 服务端核心 | MC 版本 | Java | 用哪个包 |
|---|---|---|---|
| CraftBukkit / Spigot | 1.7.10 – 1.18.1 | 8+ | `BasicCommands-1.2.0-bukkit-1.7.10-1.18.1.jar` |
| Paper | 1.18.2 | 17+ | `BasicCommands-1.2.0-paper-1.18.2.jar` |
| Paper | 1.19 – 1.19.4 | 17+ | `BasicCommands-1.2.0-paper-1.19-1.19.4.jar` |
| Paper | 1.20 – 1.20.6 | 17+ | `BasicCommands-1.2.0-paper-1.20-1.20.6.jar` |
| Paper | 1.21 – 1.21.11 | 21+ | `BasicCommands-1.2.0-paper-1.21-1.21.11.jar` |
| Paper | 26.3 | 25+ | `BasicCommands-1.2.0-paper-26.3.jar` |

Purpur / Pufferfish 等 Paper 分支按对应版本线装，未列入实测矩阵。**只装一个包**，多个包同时放会冲突。

## 安装

1. 按上表选一个 jar
2. 放进服务端 `plugins/` 目录
3. 重启服务器
4. 游戏内 `/basiccmd` 看到版本号即安装成功

## 命令与权限

| 命令 | 说明 | 权限节点 | 默认 | 冷却 |
|---|---|---|---|---|
| `/sethome [名字]` | 设家，默认名 `home`，同名覆盖 | `basiccmd.sethome` | 所有人 | 3s |
| `/delhome <名字>` | 删除家 | `basiccmd.delhome` | 所有人 | — |
| `/home [名字]` | 传送到家 | `basiccmd.home` | 所有人 | 5s |
| `/homes [页码]` | 列出家（分页） | `basiccmd.homes` | 所有人 | — |
| `/warp <名字>` | 传送到地标 | `basiccmd.warp` | 所有人 | 5s |
| `/warps [页码]` | 列出全服地标（分页） | `basiccmd.warps` | 所有人 | — |
| `/setwarp <名字>` | 创建地标，同名覆盖 | `basiccmd.setwarp` | 仅 OP | — |
| `/delwarp <名字>` | 删除地标 | `basiccmd.delwarp` | 仅 OP | — |
| `/tpa <玩家>` | 请求传送到对方 | `basiccmd.tpa` | 所有人 | 10s |
| `/tph <玩家>` | 请求对方传送到你身边 | `basiccmd.tph` | 所有人 | 10s |
| `/tpaccept [玩家]` | 接受请求（不带参数取最近一条） | `basiccmd.tpa` | 所有人 | — |
| `/tpdeny [玩家]` | 拒绝请求 | `basiccmd.tpa` | 所有人 | — |
| `/spawn` | 传送到出生点 | `basiccmd.spawn` | 所有人 | 5s |
| `/back` | 返回上一个位置 | `basiccmd.back` | 所有人 | 5s |
| `/rtp` | 随机传送到安全落点 | `basiccmd.rtp` | 所有人 | 60s |
| `/basiccmd` | 显示版本号 | — | 所有人 | — |
| `/basiccmd reload` | 热重载配置与语言文件 | `basiccmd.reload` | 仅 OP | — |

另有 `basiccmd.cooldown.bypass`（仅 OP）可跳过全部冷却。

## 配置项

配置文件 `plugins/BasicCommands/config.yml`，改完执行 `/basiccmd reload`。

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `language` | `zh_CN` | 语言，可选 `zh_CN` / `en_US` |
| `features.homes/warps/tpa/spawn/back/rtp.enabled` | `true` | 六组功能总开关，关掉后命令仍在、执行时提示已禁用 |
| `homes.limit` | `5` | 每人的家的数量上限 |
| `homes.cross-world` | `true` | `false` = 只能回当前世界的家 |
| `homes.page-size` | `8` | `/homes` 每页条数 |
| `warps.page-size` | `8` | `/warps` 每页条数 |
| `teleport.countdown-seconds` | `3` | 传送前倒计时，`0` = 立即传送 |
| `teleport.cancel-on-move` | `true` | 倒计时中移动取消 |
| `teleport.cancel-on-damage` | `true` | 倒计时中受伤取消 |
| `teleport.move-threshold` | `0.5` | 判定「移动了」的距离（格） |
| `tpa.timeout-seconds` | `60` | 请求有效期 |
| `tpa.tph-requires-accept` | `true` | `false` = `/tph` 直接把对方拉过来 |
| `tpa.cross-world` | `true` | 是否允许跨世界请求 |
| `spawn.mode` | `world` | `world` = 当前世界出生点，`main` = 固定主世界 |
| `spawn.main-world` | `world` | `mode: main` 时使用哪个世界 |
| `back.mode` | `global` | `global` = 全局共用一个返回点，`per-world` = 各世界分开 |
| `back.on-death` | `true` | 死亡时记录位置 |
| `back.on-teleport` | `true` | 传送前记录位置 |
| `rtp.default.min-range` / `max-range` | `100` / `5000` | 随机传送距离范围 |
| `rtp.worlds` | `{}` | 按世界覆盖距离范围 |
| `rtp.max-attempts` | `16` | 寻找安全落点的重试次数 |
| `rtp.blacklist-worlds` | `world_nether`、`world_the_end` | 禁止随机传送的世界 |
| `cooldowns.*` | 3–60 秒 | 各命令冷却 |

语言文件在 `plugins/BasicCommands/lang/`，升级只补缺失键，不覆盖你改过的内容；目录为空时插件自动释放一份默认的。

## 兼容说明

- 1.7.10–1.18.1 的 bukkit 包没有 Adventure：消息用 `&` 色码，聊天栏没有可点击的 `[接受]` / `[拒绝]` 按钮，需要手输 `/tpaccept`
- 六个包的数据文件格式完全一致，换包不用迁移
- 数据文件由插件自动维护，不要手工编辑；写入异步，关服同步落盘

## 更新日志

见 [`CHANGELOG.md`](CHANGELOG.md)。1.2.0：新增 `features.*` 六个功能总开关（支持热重载）、新增语言键 `general.feature-disabled`，旧配置缺键按 `true` 处理。

## 许可与来源

MIT License，Copyright (c) 2026 YHJM1208。
转载/整合请保留出处与许可声明。
