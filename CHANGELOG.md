# 更新日志

## 1.2.0 — 2026-11-16

### 新增

- **六个功能总开关**：`config.yml` 新增 `features.homes` / `warps` / `tpa` / `spawn` / `back` / `rtp`，每组一个 `enabled`。
  - 关掉后**命令仍在**（Tab 补全、`/help` 不变），执行时统一回「该功能已被管理员禁用。」。
  - 改完 `/basiccmd reload` 立即生效，不用重启；缺键按 `true`，老配置直接兼容。
  - 新增 `Features.java` 维护「命令名 → 功能组」映射，现代线与 legacy 包共用同一套组名。
- 新增语言键 `general.feature-disabled`（zh_CN / en_US，现代线与 legacy 各一份）。

### 变更

- 两个包（paper 5 条线 + bukkit legacy）版本号统一升到 `1.2.0`，数据文件格式未变，可直接替换 1.1.0。

### 验证（6 条版本线全部实机通过）

| 版本线 | 服务端 | 结果 |
|---|---|---|
| 1.7.10 – 1.18.1 | CraftBukkit 1.7.10 | 加载 → 启用 → 命令 → 总开关关/开，全过 |
| 1.18.2 | Paper 1.18.2 (build 388) | 全过（含总开关回归） |
| 1.19 – 1.19.4 | Paper 1.19.4 (build 550) | 全过 |
| 1.20 – 1.20.6 | Paper 1.20.6 (build 151) | 全过 |
| 1.21 – 1.21.11 | Paper 1.21.11 (build 132) | 全过 |
| 26.3 | Paper 26.3 (build 249) | 全过 |

## 1.1.0 — 2026-11-16

- 新增地标四件套：`/warp`、`/warps`、`/setwarp`、`/delwarp`。
- `/homes` 与 `/warps` 支持分页。
- Paper 线的随机传送改为异步找点。
- `plugin.yml` 的 `authors` 修正为 `YHJM1208`。
- 产物命名改为 `BasicCommands-<版本>-<平台>-<MC最低>-<MC最高>.jar`。

## 1.0.0 — 2026-11-11

- 首个版本：`/sethome` `/delhome` `/home` `/homes`、`/tpa` `/tph` `/tpaccept` `/tpdeny`、`/spawn`、`/back`、`/rtp`、`/basiccmd`（版本、重载）。
- 现代线（Paper 1.18.2+，MiniMessage 文案、可点击接受/拒绝）与 legacy 线（1.7.10 – 1.18.1，`&` 颜色码）两套实现。
