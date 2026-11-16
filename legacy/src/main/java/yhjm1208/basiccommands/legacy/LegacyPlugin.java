package yhjm1208.basiccommands.legacy;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** legacy 主类：一套代码覆盖 1.7.10 – 1.18.1（Spigot/Bukkit API，Java 8）。 */
public final class LegacyPlugin extends JavaPlugin {

    private static final String[] COMMANDS = {
            "sethome", "delhome", "home", "homes",
            "warp", "warps", "setwarp", "delwarp",
            "tpa", "tph", "tpaccept", "tpdeny",
            "spawn", "back", "rtp", "basiccmd"
    };

    private LegacyLang lang;
    private LegacyStore store;
    private LegacyTeleport teleports;
    private LegacyCommands commands;
    private BukkitTask ticker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lang = new LegacyLang(this);
        lang.load();
        store = new LegacyStore(this);
        store.load();
        teleports = new LegacyTeleport(this, lang, store);
        getServer().getPluginManager().registerEvents(teleports, this);

        commands = new LegacyCommands(this, lang, store, teleports);
        for (String name : COMMANDS) {
            commands.register(name);
        }

        ticker = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                commands.tick();
            }
        }, 100L, 100L);

        getLogger().info("BasicCmd (legacy) 已启用，语言 = " + lang.language());
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.cancel();
        }
        if (teleports != null) {
            teleports.shutdown();
        }
        if (store != null) {
            store.saveNow();
        }
    }
}
