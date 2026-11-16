# BasicCommands

Minecraft 基础命令插件：家、地标、TPA/TPH、出生点、返回上一点、随机传送。

## 选哪个包

按你的服务端版本选，**整条线通用**：

| 服务端 | 用这个包 | Java |
|---|---|---|
| Spigot / CraftBukkit 1.7.10 – 1.18.1 | `BasicCommands-1.2.0-bukkit-1.7.10-1.18.1.jar` | 8+ |
| Paper 1.18.2 | `BasicCommands-1.2.0-paper-1.18.2.jar` | 17+ |
| Paper 1.19 – 1.19.4 | `BasicCommands-1.2.0-paper-1.19-1.19.4.jar` | 17+ |
| Paper 1.20 – 1.20.6 | `BasicCommands-1.2.0-paper-1.20-1.20.6.jar` | 17+ |
| Paper 1.21 – 1.21.11 | `BasicCommands-1.2.0-paper-1.21-1.21.11.jar` | 21+ |
| Paper 26.3 | `BasicCommands-1.2.0-paper-26.3.jar` | 25+ |

安装：把 jar 放进服务端 `plugins/`，重启即可。**只装一个**，不要多个包同时放。

## 两种包的差异

1.7.10 – 1.18.1 的 bukkit 包没有 Adventure 组件系统，所以有一处体验差异：

| 功能 | paper 包 | bukkit 包 |
|---|---|---|
| 消息颜色 | MiniMessage 富文本 | `&` 传统色码 |
| 聊天栏 `[接受]` `[拒绝]` 按钮 | 有，可点击 | **没有**，玩家手输 `/tpaccept` |
| hover 提示 | 有 | 没有 |
| 其余功能（家、地标、分页、tpa、spawn、back、rtp、倒计时、权限、冷却、中英双语） | 完整 | 完整 |
| 数据文件格式 | `homes.yml` / `warps.yml` / `back.yml` | **完全相同**，换包不用迁移数据 |

## 命令

| 命令 | 说明 | 权限 | 冷却 |
|---|---|---|---|
| `/sethome [名字]` | 设家，默认名 `home`，同名覆盖 | `basiccmd.sethome` | 3s |
| `/delhome <名字>` | 删家 | `basiccmd.delhome` | — |
| `/home [名字]` | 传送到家 | `basiccmd.home` | 5s |
| `/homes [页码]` | 列出家（每页 8 条，超过自动分页） | `basiccmd.homes` | — |
| `/warp <名字>` | 传送到地标 | `basiccmd.warp` | 5s |
| `/warps [页码]` | 列出全服地标（每页 8 条） | `basiccmd.warps` | — |
| `/setwarp <名字>` | 在当前位置创建地标，同名覆盖 | `basiccmd.setwarp` | — |
| `/delwarp <名字>` | 删除地标 | `basiccmd.delwarp` | — |
| `/tpa <玩家>` | 请求传送到对方 | `basiccmd.tpa` | 10s |
| `/tph <玩家>` | 请求对方传送到你身边 | `basiccmd.tph` | 10s |
| `/tpaccept [玩家]` | 接受请求（不带参数取最近一条） | `basiccmd.tpa` | — |
| `/tpdeny [玩家]` | 拒绝请求 | `basiccmd.tpa` | — |
| `/spawn` | 传送到出生点 | `basiccmd.spawn` | 5s |
| `/back` | 返回上一个位置 | `basiccmd.back` | 5s |
| `/rtp` | 随机传送到安全落点 | `basiccmd.rtp` | 60s |
| `/basiccmd` | 显示版本号 | — | — |
| `/basiccmd reload` | 热加载配置与语言文件 | `basiccmd.reload` | — |

除 `/setwarp`、`/delwarp` 默认仅 op 可用外，其余命令默认对所有玩家开放。另有 `basiccmd.cooldown.bypass`（op 默认）可跳过冷却。

传送请求默认 60 秒过期；`/tph` 需要对方同意，可配置成直接拉人。

## 配置

配置文件 `plugins/BasicCommands/config.yml`，改完执行 `/basiccmd reload`。

### 功能开关

六组功能各有一个总开关，关掉后**命令还在**，只是执行时统一回一句「该功能已被管理员禁用」：

```yaml
features:
  homes:
    enabled: true    # /sethome /delhome /home /homes
  warps:
    enabled: true    # /warp /warps /setwarp /delwarp
  tpa:
    enabled: true    # /tpa /tph /tpaccept /tpdeny
  spawn:
    enabled: true    # /spawn
  back:
    enabled: true    # /back
  rtp:
    enabled: true    # /rtp
```

改完 `/basiccmd reload` 立即生效，不用重启服务器。

### 家

```yaml
homes:
  limit: 5             # 所有人的家的上限（不分级）
  cross-world: true    # false = 只能回当前世界的家
  page-size: 8         # /homes 每页显示几条
```

### 地标

```yaml
warps:
  page-size: 8         # /warps 每页显示几条
```

地标是全服共享的，没有数量上限，只有 op 能创建和删除。

### 传送倒计时

```yaml
teleport:
  countdown-seconds: 3   # 0 = 立即传送
  cancel-on-move: true   # 倒计时中移动取消
  cancel-on-damage: true # 倒计时中受伤取消
  move-threshold: 0.5    # 判定为“移动了”的距离（格）
```

### 传送请求

```yaml
tpa:
  timeout-seconds: 60        # 请求有效期
  tph-requires-accept: true  # false = /tph 直接把对方拉过来
  cross-world: true          # false = 禁止跨世界请求
```

### 出生点

```yaml
spawn:
  mode: world        # world = 当前世界出生点；main = 固定主世界出生点
  main-world: world  # mode=main 时用哪个世界
```

`world` 适合单世界服；`main` 适合多世界服（玩家在末地也能一键回主城）。

### 返回上一点

```yaml
back:
  mode: global      # global = 所有世界共用一个返回点
                    # per-world = 每个世界分别记录，只在对应世界有效
  on-death: true    # 死亡时记录死亡位置
  on-teleport: true # 传送前记录当前位置
```

### 随机传送

```yaml
rtp:
  default:
    min-range: 100
    max-range: 5000
  worlds: {}          # 按世界覆盖，见下
  max-attempts: 16    # 找不到安全落点的重试次数
  blacklist-worlds:   # 禁止随机传送的世界
    - world_nether
    - world_the_end
```

按世界设置不同范围：

```yaml
rtp:
  worlds:
    world:
      min-range: 200
      max-range: 3000
```

没单独配置的世界用 `rtp.default`。

在 Paper 服务端上，找落点是异步的：目标区块在后台加载好之后才判定安全落点，所以玩家不会感到卡顿。同一个玩家同时只能有一个进行中的随机传送。

## 语言

文本在 `plugins/BasicCommands/lang/zh_CN.yml` 和 `en_US.yml`，改颜色、文案、措辞都在这里，不用动代码。切换语言改 `config.yml` 的 `language` 再 reload。

`plugins/BasicCommands/lang/` 下没有文件时，插件会自动释放一份默认的。

**插件升级不会覆盖你改过的语言文件**；新版本新增的文案会直接用内置默认值生效。

## 数据文件

| 文件 | 内容 |
|---|---|
| `homes.yml` | 每个玩家的家 |
| `warps.yml` | 全服地标 |
| `back.yml` | `/back` 的返回点 |

这些文件由插件自动维护，不要手工编辑。写入是异步的，关服时会同步落盘。

## 常见问题

**换服务端版本后玩家数据还在吗？** 在。所有包的 `homes.yml` / `warps.yml` / `back.yml` 格式一致，直接拷 `plugins/BasicCommands/` 目录即可。

**倒计时时动了就取消了，能关吗？** 能，`teleport.cancel-on-move: false`。

**`/rtp` 会不会让服务器卡一下？** Paper 包不会：先在后台加载目标区块，再回主线程判定落点。bukkit 包（1.7.10 – 1.18.1）受限于旧 API 仍是同步找点，冷却 60 秒下影响很小。

**家很多、地标很多，列表刷屏怎么办？** 用 `/homes 2`、`/warps 2` 翻页，Tab 也会补页码。

**怎么让别人也能建地标？** 给玩家 `basiccmd.setwarp`、`basiccmd.delwarp` 权限即可，默认只有 op 有。

**传送请求重启后就没了？** 请求是内存态，重启清空；家、地标与返回点会持久化。
