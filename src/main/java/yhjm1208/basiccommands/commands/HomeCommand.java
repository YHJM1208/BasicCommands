package yhjm1208.basiccommands.commands;

import yhjm1208.basiccommands.BasicCommands;
import yhjm1208.basiccommands.Home;
import yhjm1208.basiccommands.HomeStore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** /sethome /delhome /home /homes */
public final class HomeCommand extends BaseCommand {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    public HomeCommand(BasicCommands plugin) {
        super(plugin);
    }

    public void register() {
        register("sethome", "delhome", "home", "homes");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "sethome" -> setHome(sender, args);
            case "delhome" -> deleteHome(sender, args);
            case "home" -> goHome(sender, args);
            case "homes" -> listHomes(sender);
            default -> false;
        };
    }

    private boolean setHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "sethome") || !cooldownReady(player, "sethome")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        if (!VALID_NAME.matcher(name).matches()) {
            lang.send(sender, "home.invalid-name");
            return true;
        }

        HomeStore store = plugin.homeStore();
        UUID id = player.getUniqueId();
        int limit = plugin.getConfig().getInt("homes.limit", 5);
        boolean exists = store.get(id, name) != null;
        if (!exists && store.count(id) >= limit) {
            lang.send(sender, "home.limit", "limit", limit);
            return true;
        }

        Location loc = player.getLocation();
        store.put(id, new Home(name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        lang.send(sender, exists ? "home.set-overwrite" : "home.set", "home", name);
        startCooldown(player, "sethome");
        return true;
    }

    private boolean deleteHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "delhome")) {
            return true;
        }
        if (args.length == 0) {
            lang.send(sender, "home.usage-delhome");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!plugin.homeStore().remove(player.getUniqueId(), name)) {
            lang.send(sender, "home.not-found", "home", name);
            return true;
        }
        lang.send(sender, "home.deleted", "home", name);
        return true;
    }

    private boolean goHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "home") || !cooldownReady(player, "home")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        Home home = plugin.homeStore().get(player.getUniqueId(), name);
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
        plugin.teleports().request(player, destination);
        startCooldown(player, "home");
        return true;
    }

    private boolean listHomes(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !checkPermission(sender, "homes")) {
            return true;
        }
        Map<String, Home> homes = plugin.homeStore().homes(player.getUniqueId());
        int limit = plugin.getConfig().getInt("homes.limit", 5);
        if (homes.isEmpty()) {
            lang.send(sender, "home.list-empty");
            return true;
        }
        lang.send(sender, "home.list-header", "count", homes.size(), "limit", limit);
        for (Home home : homes.values()) {
            lang.send(sender, "home.list-item",
                    "home", home.name(),
                    "world", home.world(),
                    "x", format(home.x()),
                    "y", format(home.y()),
                    "z", format(home.z()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("home") || name.equals("delhome")) {
            return filter(args[0], plugin.homeStore().homes(player.getUniqueId()).keySet());
        }
        return List.of();
    }
}
