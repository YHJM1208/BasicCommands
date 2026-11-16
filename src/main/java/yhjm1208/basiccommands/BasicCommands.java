package yhjm1208.basiccommands;

import yhjm1208.basiccommands.commands.AdminCommand;
import yhjm1208.basiccommands.commands.BackCommand;
import yhjm1208.basiccommands.commands.HomeCommand;
import yhjm1208.basiccommands.commands.RtpCommand;
import yhjm1208.basiccommands.commands.SpawnCommand;
import yhjm1208.basiccommands.commands.TpaCommand;
import yhjm1208.basiccommands.commands.WarpCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** 基础命令插件：家、地标、TPA/TPH、随机传送。 */
public final class BasicCommands extends JavaPlugin {

    private Lang lang;
    private Cooldowns cooldowns;
    private HomeStore homeStore;
    private WarpStore warpStore;
    private BackService back;
    private TeleportService teleports;
    private TpaManager tpa;
    private RtpFinder rtpFinder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lang = new Lang(this);
        lang.load();
        cooldowns = new Cooldowns();
        homeStore = new HomeStore(this);
        homeStore.load();
        warpStore = new WarpStore(this);
        warpStore.load();
        back = new BackService(this);
        back.load();
        getServer().getPluginManager().registerEvents(back, this);
        teleports = new TeleportService(this);
        getServer().getPluginManager().registerEvents(teleports, this);
        tpa = new TpaManager(this);
        tpa.start();
        rtpFinder = new RtpFinder(this);

        new HomeCommand(this).register();
        new WarpCommand(this).register();
        new TpaCommand(this).register();
        new SpawnCommand(this).register();
        new BackCommand(this).register();
        new RtpCommand(this).register();
        new AdminCommand(this).register();

        getLogger().info("BasicCmd 已启用，语言 = " + lang.language());
    }

    @Override
    public void onDisable() {
        if (tpa != null) {
            tpa.stop();
        }
        if (teleports != null) {
            teleports.shutdown();
        }
        if (homeStore != null) {
            homeStore.saveNow();
        }
        if (warpStore != null) {
            warpStore.saveNow();
        }
        if (back != null) {
            back.saveNow();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        lang.load();
    }

    public Lang lang() {
        return lang;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public HomeStore homeStore() {
        return homeStore;
    }

    public WarpStore warpStore() {
        return warpStore;
    }

    public BackService back() {
        return back;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public TpaManager tpa() {
        return tpa;
    }

    public RtpFinder rtpFinder() {
        return rtpFinder;
    }
}
