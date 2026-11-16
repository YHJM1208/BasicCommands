package yhjm1208.basiccommands.legacy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * legacy 版所有命令。
 * 与新版的差异：消息是 String + & 色码，没有点击按钮；传送用同步 teleport。
 */
public final class LegacyCommands implements CommandExecutor, TabCompleter {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    private static final class Request {
        final UUID from;
        final UUID to;
        final boolean tph;
        final long created;

        Request(UUID from, UUID to, boolean tph) {
            this.from = from;
            this.to = to;
            this.tph = tph;
            this.created = System.currentTimeMillis();
        }
    }

    private final JavaPlugin plugin;
    private final LegacyLang lang;
    private final LegacyStore store;
    private final LegacyTeleport teleports;

    private final Map<String, Long> cooldowns = new HashMap<String, Long>();
    private final Map<UUID, Map<UUID, Request>> pending = new HashMap<UUID, Map<UUID, Request>>();

    public LegacyCommands(JavaPlugin plugin, LegacyLang lang, LegacyStore store, LegacyTeleport teleports) {
        this.plugin = plugin;
        this.lang = lang;
        this.store = store;
        this.teleports = teleports;
    }

    // ------------------------------------------------------------ 框架

    public void register(String name) {
        if (plugin.getCommand(name) == null) {
            plugin.getLogger().warning("命令 /" + name + " 已被其他插件占用，跳过注册");
            return;
        }
        plugin.getCommand(name).setExecutor(this);
        plugin.getCommand(name).setTabCompleter(this);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        lang.send(sender, "general.player-only");
        return null;
    }

    private boolean checkPermission(CommandSender sender, String node) {
        if (sender.hasPermission("basiccmd." + node)) {
            return true;
        }
        lang.send(sender, "general.no-permission");
        return false;
    }

    private boolean cooldownReady(Player player, String key) {
        if (player.hasPermission("basiccmd.cooldown.bypass")) {
            return true;
        }
        Long until = cooldowns.get(player.getUniqueId() + ":" + key);
        if (until == null) {
            return true;
        }
        long remaining = until.longValue() - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(player.getUniqueId() + ":" + key);
            return true;
        }
        lang.send(player, "general.cooldown", "seconds", Integer.valueOf((int) ((remaining + 999) / 1000)));
        return false;
    }

    private void startCooldown(Player player, String key) {
        if (player.hasPermission("basiccmd.cooldown.bypass")) {
            return;
        }
        int seconds = plugin.getConfig().getInt("cooldowns." + key, 0);
        if (seconds > 0) {
            cooldowns.put(player.getUniqueId() + ":" + key, Long.valueOf(System.currentTimeMillis() + seconds * 1000L));
        }
    }

    private Player findOnline(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            lang.send(sender, "general.player-not-found", "player", name);
        }
        return target;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", Double.valueOf(value));
    }

    private static int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static List<String> pageNumbers(int pageSize, int count) {
        List<String> numbers = new ArrayList<String>();
        int pages = (count + pageSize - 1) / pageSize;
        for (int i = 1; i <= pages; i++) {
            numbers.add(String.valueOf(i));
        }
        return numbers;
    }

    private static List<String> filter(String prefix, Iterable<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        Collections.sort(result);
        return result;
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<String>();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                names.add(player.getName());
            }
        }
        return names;
    }

    // ------------------------------------------------------------ 分发

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ("sethome".equals(name)) {
            return sethome(sender, args);
        }
        if ("delhome".equals(name)) {
            return delhome(sender, args);
        }
        if ("home".equals(name)) {
            return home(sender, args);
        }
        if ("homes".equals(name)) {
            return homes(sender, args);
        }
        if ("warp".equals(name)) {
            return goWarp(sender, args);
        }
        if ("warps".equals(name)) {
            return warps(sender, args);
        }
        if ("setwarp".equals(name)) {
            return setWarp(sender, args);
        }
        if ("delwarp".equals(name)) {
            return deleteWarp(sender, args);
        }
        if ("tpa".equals(name)) {
            return sendRequest(sender, args, false);
        }
        if ("tph".equals(name)) {
            return sendRequest(sender, args, true);
        }
        if ("tpaccept".equals(name)) {
            return respond(sender, args, true);
        }
        if ("tpdeny".equals(name)) {
            return respond(sender, args, false);
        }
        if ("spawn".equals(name)) {
            return spawn(sender);
        }
        if ("back".equals(name)) {
            return back(sender);
        }
        if ("rtp".equals(name)) {
            return rtp(sender);
        }
        if ("basiccmd".equals(name)) {
            return basiccmd(sender, args);
        }
        return false;
    }

    // ------------------------------------------------------------ 家

    private boolean sethome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "sethome") || !cooldownReady(player, "sethome")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        if (!VALID_NAME.matcher(name).matches()) {
            lang.send(sender, "home.invalid-name");
            return true;
        }
        int limit = plugin.getConfig().getInt("homes.limit", 5);
        boolean exists = store.getHome(player.getUniqueId(), name) != null;
        if (!exists && store.homeCount(player.getUniqueId()) >= limit) {
            lang.send(sender, "home.limit", "limit", Integer.valueOf(limit));
            return true;
        }
        Location loc = player.getLocation();
        store.putHome(player.getUniqueId(), new LegacyStore.Home(name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        lang.send(sender, exists ? "home.set-overwrite" : "home.set", "home", name);
        startCooldown(player, "sethome");
        return true;
    }

    private boolean delhome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "delhome")) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, "home.usage-delhome");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!store.removeHome(player.getUniqueId(), name)) {
            lang.send(sender, "home.not-found", "home", name);
            return true;
        }
        lang.send(sender, "home.deleted", "home", name);
        return true;
    }

    private boolean home(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "home") || !cooldownReady(player, "home")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        LegacyStore.Home home = store.getHome(player.getUniqueId(), name);
        if (home == null) {
            lang.send(sender, "home.not-found", "home", name);
            return true;
        }
        Location destination = home.toLocation();
        if (destination == null) {
            lang.send(sender, "home.invalid-world", "home", name);
            return true;
        }
        if (!plugin.getConfig().getBoolean("homes.cross-world", true)
                && !home.world().equals(player.getWorld().getName())) {
            lang.send(sender, "home.cross-world-denied", "home", name, "world", home.world());
            return true;
        }
        lang.send(sender, "home.teleporting", "home", name);
        teleports.request(player, destination);
        startCooldown(player, "home");
        return true;
    }

    private boolean homes(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "homes")) {
            return true;
        }
        Map<String, LegacyStore.Home> map = store.homes(player.getUniqueId());
        if (map.isEmpty()) {
            lang.send(sender, "home.list-empty");
            return true;
        }
        int pageSize = Math.max(1, plugin.getConfig().getInt("homes.page-size", 8));
        int pages = (map.size() + pageSize - 1) / pageSize;
        int page = args.length > 0 ? parsePage(args[0]) : 1;
        if (page < 1 || page > pages) {
            lang.send(sender, "general.invalid-page", "pages", Integer.valueOf(pages));
            return true;
        }
        List<LegacyStore.Home> list = new ArrayList<LegacyStore.Home>(map.values());
        lang.send(sender, "home.list-header",
                "count", Integer.valueOf(map.size()),
                "limit", Integer.valueOf(plugin.getConfig().getInt("homes.limit", 5)),
                "page", Integer.valueOf(page),
                "pages", Integer.valueOf(pages));
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, list.size());
        for (int i = from; i < to; i++) {
            LegacyStore.Home home = list.get(i);
            lang.send(sender, "home.list-item",
                    "home", home.name(),
                    "world", home.world(),
                    "x", format(home.x), "y", format(home.y), "z", format(home.z));
        }
        return true;
    }

    // ------------------------------------------------------------ 地标

    private boolean goWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "warp") || !cooldownReady(player, "warp")) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, "warp.usage");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        LegacyStore.Warp warp = store.getWarp(name);
        if (warp == null) {
            lang.send(sender, "warp.not-found", "warp", name);
            return true;
        }
        Location destination = warp.toLocation();
        if (destination == null) {
            lang.send(sender, "warp.invalid-world", "warp", name);
            return true;
        }
        lang.send(sender, "warp.teleporting", "warp", name);
        teleports.request(player, destination);
        startCooldown(player, "warp");
        return true;
    }

    private boolean setWarp(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "setwarp")) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, "warp.usage-set");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!VALID_NAME.matcher(name).matches()) {
            lang.send(sender, "warp.invalid-name");
            return true;
        }
        Location location = player.getLocation();
        boolean overwritten = store.putWarp(new LegacyStore.Warp(name,
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
        lang.send(sender, overwritten ? "warp.created-overwrite" : "warp.created", "warp", name);
        return true;
    }

    private boolean deleteWarp(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "delwarp")) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, "warp.usage-del");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!store.removeWarp(name)) {
            lang.send(sender, "warp.not-found", "warp", name);
            return true;
        }
        lang.send(sender, "warp.deleted", "warp", name);
        return true;
    }

    private boolean warps(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "warps")) {
            return true;
        }
        List<LegacyStore.Warp> list = store.warps();
        if (list.isEmpty()) {
            lang.send(sender, "warp.list-empty");
            return true;
        }
        int pageSize = Math.max(1, plugin.getConfig().getInt("warps.page-size", 8));
        int pages = (list.size() + pageSize - 1) / pageSize;
        int page = args.length > 0 ? parsePage(args[0]) : 1;
        if (page < 1 || page > pages) {
            lang.send(sender, "general.invalid-page", "pages", Integer.valueOf(pages));
            return true;
        }
        lang.send(sender, "warp.list-header",
                "count", Integer.valueOf(list.size()),
                "page", Integer.valueOf(page),
                "pages", Integer.valueOf(pages));
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, list.size());
        for (int i = from; i < to; i++) {
            LegacyStore.Warp warp = list.get(i);
            lang.send(sender, "warp.list-item",
                    "warp", warp.name(),
                    "world", warp.world(),
                    "x", format(warp.x()), "y", format(warp.y()), "z", format(warp.z()));
        }
        return true;
    }

    // ------------------------------------------------------------ tpa / tph

    private boolean sendRequest(CommandSender sender, String[] args, boolean tph) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String permission = tph ? "tph" : "tpa";
        if (!checkPermission(sender, permission)) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, tph ? "tpa.usage-tph" : "tpa.usage");
            return true;
        }
        if (!cooldownReady(player, permission)) {
            return true;
        }
        Player target = findOnline(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            lang.send(sender, "tpa.self");
            return true;
        }
        if (!crossWorldAllowed(player, target)) {
            lang.send(sender, "tpa.cross-world-denied", "player", target.getName());
            return true;
        }

        if (tph && !plugin.getConfig().getBoolean("tpa.tph-requires-accept", true)) {
            lang.send(sender, "tpa.sent-tph", "player", target.getName());
            lang.send(target, "tpa.accepted-tph-pulled", "player", player.getName());
            teleports.request(target, player.getLocation());
            startCooldown(player, permission);
            return true;
        }

        Map<UUID, Request> incoming = pending.get(target.getUniqueId());
        if (incoming == null) {
            incoming = new HashMap<UUID, Request>();
            pending.put(target.getUniqueId(), incoming);
        }
        incoming.put(player.getUniqueId(), new Request(player.getUniqueId(), target.getUniqueId(), tph));

        lang.send(sender, tph ? "tpa.sent-tph" : "tpa.sent", "player", target.getName());
        lang.send(target, tph ? "tpa.received-tph" : "tpa.received", "player", player.getName());
        startCooldown(player, permission);
        return true;
    }

    private boolean respond(CommandSender sender, String[] args, boolean accept) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "tpa")) {
            return true;
        }
        Request request;
        if (args.length > 0) {
            Player from = Bukkit.getPlayerExact(args[0]);
            if (from == null) {
                lang.send(sender, "tpa.none", "player", args[0]);
                return true;
            }
            Map<UUID, Request> incoming = pending.get(player.getUniqueId());
            request = incoming == null ? null : incoming.get(from.getUniqueId());
            if (request == null) {
                lang.send(sender, "tpa.none", "player", from.getName());
                return true;
            }
        } else {
            request = latestPending(player.getUniqueId());
            if (request == null) {
                lang.send(sender, "tpa.no-pending");
                return true;
            }
        }

        removeRequest(request);
        Player from = Bukkit.getPlayer(request.from);
        if (from == null) {
            lang.send(sender, "tpa.expired", "player", args.length > 0 ? args[0] : "?");
            return true;
        }
        int timeout = plugin.getConfig().getInt("tpa.timeout-seconds", 60);
        if (System.currentTimeMillis() - request.created > timeout * 1000L) {
            lang.send(sender, "tpa.expired", "player", from.getName());
            return true;
        }

        if (!accept) {
            lang.send(sender, "tpa.denied", "player", from.getName());
            lang.send(from, "tpa.denied-target", "player", player.getName());
            return true;
        }
        if (!crossWorldAllowed(player, from)) {
            lang.send(sender, "tpa.cross-world-denied", "player", from.getName());
            return true;
        }

        if (!request.tph) {
            lang.send(sender, "tpa.accepted", "player", from.getName());
            lang.send(from, "tpa.accepted-target", "player", player.getName());
            teleports.request(from, player.getLocation());
        } else {
            lang.send(sender, "tpa.accepted-tph", "player", from.getName());
            lang.send(from, "tpa.accepted-tph-target", "player", player.getName());
            teleports.request(player, from.getLocation());
        }
        return true;
    }

    private Request latestPending(UUID target) {
        Map<UUID, Request> incoming = pending.get(target);
        if (incoming == null || incoming.isEmpty()) {
            return null;
        }
        Request latest = null;
        for (Request request : incoming.values()) {
            if (latest == null || request.created > latest.created) {
                latest = request;
            }
        }
        return latest;
    }

    private void removeRequest(Request request) {
        Map<UUID, Request> incoming = pending.get(request.to);
        if (incoming == null) {
            return;
        }
        incoming.remove(request.from);
        if (incoming.isEmpty()) {
            pending.remove(request.to);
        }
    }

    private boolean crossWorldAllowed(Player first, Player second) {
        if (plugin.getConfig().getBoolean("tpa.cross-world", true)) {
            return true;
        }
        return first.getWorld().equals(second.getWorld());
    }

    /** 由主类定时调用，清理过期请求。 */
    public void tick() {
        int timeout = plugin.getConfig().getInt("tpa.timeout-seconds", 60);
        long now = System.currentTimeMillis();
        List<UUID> emptyTargets = new ArrayList<UUID>();
        for (Map.Entry<UUID, Map<UUID, Request>> entry : pending.entrySet()) {
            List<UUID> expired = new ArrayList<UUID>();
            for (Map.Entry<UUID, Request> inner : entry.getValue().entrySet()) {
                if (now - inner.getValue().created > timeout * 1000L) {
                    expired.add(inner.getKey());
                }
            }
            for (UUID id : expired) {
                entry.getValue().remove(id);
            }
            if (entry.getValue().isEmpty()) {
                emptyTargets.add(entry.getKey());
            }
        }
        for (UUID id : emptyTargets) {
            pending.remove(id);
        }
    }

    // ------------------------------------------------------------ spawn / back / rtp

    private boolean spawn(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "spawn") || !cooldownReady(player, "spawn")) {
            return true;
        }
        World world = player.getWorld();
        if ("main".equalsIgnoreCase(plugin.getConfig().getString("spawn.mode", "world"))) {
            String mainWorldName = plugin.getConfig().getString("spawn.main-world", "world");
            World mainWorld = Bukkit.getWorld(mainWorldName);
            if (mainWorld == null) {
                lang.send(sender, "spawn.main-world-missing", "world", mainWorldName);
                return true;
            }
            world = mainWorld;
        }
        lang.send(sender, "spawn.teleporting", "world", world.getName());
        teleports.request(player, world.getSpawnLocation());
        startCooldown(player, "spawn");
        return true;
    }

    private boolean back(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "back") || !cooldownReady(player, "back")) {
            return true;
        }
        LegacyStore.Point point = store.getBack(player);
        if (point == null) {
            lang.send(sender, "back.none");
            return true;
        }
        Location destination = point.toLocation();
        if (destination == null) {
            lang.send(sender, "back.world-gone");
            return true;
        }
        lang.send(sender, "back.teleporting", "world", point.world);
        teleports.request(player, destination, false);
        startCooldown(player, "back");
        return true;
    }

    private boolean rtp(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "rtp") || !cooldownReady(player, "rtp")) {
            return true;
        }
        World world = player.getWorld();
        if (plugin.getConfig().getStringList("rtp.blacklist-worlds").contains(world.getName())) {
            lang.send(sender, "rtp.blacklisted");
            return true;
        }
        int min = plugin.getConfig().getInt("rtp.default.min-range",
                plugin.getConfig().getInt("rtp.min-range", 100));
        int max = plugin.getConfig().getInt("rtp.default.max-range",
                plugin.getConfig().getInt("rtp.max-range", 5000));
        if (world.getName().equals(plugin.getConfig().getString("rtp.worlds." + world.getName() + ".world"))) {
            min = plugin.getConfig().getInt("rtp.worlds." + world.getName() + ".min-range", min);
            max = plugin.getConfig().getInt("rtp.worlds." + world.getName() + ".max-range", max);
        }

        lang.send(sender, "rtp.searching");
        Location target = findSafeLocation(world, min, max,
                plugin.getConfig().getInt("rtp.max-attempts", 16));
        if (target == null) {
            lang.send(sender, "rtp.failed");
            return true;
        }
        lang.send(sender, "rtp.success",
                "x", Integer.valueOf(target.getBlockX()),
                "y", Integer.valueOf(target.getBlockY()),
                "z", Integer.valueOf(target.getBlockZ()),
                "world", world.getName());
        teleports.request(player, target);
        startCooldown(player, "rtp");
        return true;
    }

    /** 1.7.10 没有 WorldBorder，以出生点为中心取范围。 */
    private Location findSafeLocation(World world, int minRange, int maxRange, int attempts) {
        Location center = world.getSpawnLocation();
        int min = Math.max(1, minRange);
        int max = Math.max(min, maxRange);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < attempts; i++) {
            int radius = min + random.nextInt(max - min + 1);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);
            if (y <= 2) {
                continue;
            }
            Block ground = world.getBlockAt(x, y, z);
            if (!isSafeGround(ground)) {
                continue;
            }
            if (!isClear(world.getBlockAt(x, y + 1, z)) || !isClear(world.getBlockAt(x, y + 2, z))) {
                continue;
            }
            return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        }
        return null;
    }

    private boolean isSafeGround(Block block) {
        Material type = block.getType();
        if (!type.isSolid() || block.isLiquid()) {
            return false;
        }
        return type != Material.CACTUS && type != Material.FIRE && type != Material.LAVA;
    }

    private boolean isClear(Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }

    // ------------------------------------------------------------ 管理

    private boolean basiccmd(CommandSender sender, String[] args) {
        if (args.length == 0) {
            lang.send(sender, "general.version", "version", plugin.getDescription().getVersion());
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!checkPermission(sender, "reload")) {
                return true;
            }
            plugin.reloadConfig();
            lang.load();
            lang.send(sender, "general.reloaded");
            return true;
        }
        lang.send(sender, "general.usage-admin");
        return true;
    }

    // ------------------------------------------------------------ 补全

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ("home".equals(name) || "delhome".equals(name)) {
            return filter(args[0], store.homes(player.getUniqueId()).keySet());
        }
        if ("homes".equals(name)) {
            return filter(args[0], pageNumbers(
                    Math.max(1, plugin.getConfig().getInt("homes.page-size", 8)),
                    store.homeCount(player.getUniqueId())));
        }
        if ("warp".equals(name) || "delwarp".equals(name)) {
            List<String> names = new ArrayList<String>();
            for (LegacyStore.Warp warp : store.warps()) {
                names.add(warp.name());
            }
            return filter(args[0], names);
        }
        if ("warps".equals(name)) {
            return filter(args[0], pageNumbers(
                    Math.max(1, plugin.getConfig().getInt("warps.page-size", 8)),
                    store.warpCount()));
        }
        if ("tpa".equals(name) || "tph".equals(name) || "tpaccept".equals(name) || "tpdeny".equals(name)) {
            return filter(args[0], onlineNames());
        }
        if ("basiccmd".equals(name)) {
            return filter(args[0], Collections.singletonList("reload"));
        }
        return Collections.emptyList();
    }
}
