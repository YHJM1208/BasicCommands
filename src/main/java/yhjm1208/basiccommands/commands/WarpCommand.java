package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import yhjm1208.basiccommands.Warp;
import yhjm1208.basiccommands.WarpStore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

/** /warp /warps /setwarp /delwarp */
public final class WarpCommand extends BaseCommand {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    public WarpCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("warp", "warps", "setwarp", "delwarp");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "warp" -> goWarp(sender, args);
            case "warps" -> listWarps(sender, args);
            case "setwarp" -> setWarp(sender, args);
            case "delwarp" -> deleteWarp(sender, args);
            default -> false;
        };
    }

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
        Warp warp = plugin.warpStore().get(name);
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
        plugin.teleports().request(player, destination);
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
        WarpStore store = plugin.warpStore();
        boolean exists = store.exists(name);
        Location loc = player.getLocation();
        store.put(new Warp(name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        lang.send(sender, exists ? "warp.created-overwrite" : "warp.created", "warp", name);
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
        if (!plugin.warpStore().remove(name)) {
            lang.send(sender, "warp.not-found", "warp", name);
            return true;
        }
        lang.send(sender, "warp.deleted", "warp", name);
        return true;
    }

    private boolean listWarps(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "warps")) {
            return true;
        }
        List<Warp> warps = plugin.warpStore().all();
        if (warps.isEmpty()) {
            lang.send(sender, "warp.list-empty");
            return true;
        }
        int pageSize = Math.max(1, plugin.getConfig().getInt("warps.page-size", 8));
        int pages = (warps.size() + pageSize - 1) / pageSize;
        int page = 1;
        if (args.length > 0) {
            page = parsePage(args[0]);
            if (page < 1 || page > pages) {
                lang.send(sender, "general.invalid-page", "pages", pages);
                return true;
            }
        }
        lang.send(sender, "warp.list-header", "count", warps.size(), "page", page, "pages", pages);
        int from = (page - 1) * pageSize;
        int to = Math.min(warps.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            Warp warp = warps.get(i);
            lang.send(sender, "warp.list-item",
                    "warp", warp.name(),
                    "world", warp.world(),
                    "x", format(warp.x()),
                    "y", format(warp.y()),
                    "z", format(warp.z()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "warp", "delwarp" -> filter(args[0], plugin.warpStore().all().stream().map(Warp::name).toList());
            case "warps" -> filter(args[0], pageSuggestions(args[0]));
            default -> List.of();
        };
    }

    private List<String> pageSuggestions(String prefix) {
        int pageSize = Math.max(1, plugin.getConfig().getInt("warps.page-size", 8));
        int pages = (plugin.warpStore().count() + pageSize - 1) / pageSize;
        if (pages <= 1) {
            return List.of();
        }
        return filter(prefix, IntStream.rangeClosed(1, pages).mapToObj(String::valueOf).toList());
    }
}
