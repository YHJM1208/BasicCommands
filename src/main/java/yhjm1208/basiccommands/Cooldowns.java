package yhjm1208.basiccommands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 内存中的命令冷却表，只在主线程访问。 */
public final class Cooldowns {

    private final Map<String, Long> until = new HashMap<>();

    public long remainingSeconds(UUID player, String key) {
        String id = player + ":" + key;
        Long expireAt = until.get(id);
        if (expireAt == null) {
            return 0;
        }
        long diff = expireAt - System.currentTimeMillis();
        if (diff <= 0) {
            until.remove(id);
            return 0;
        }
        return (diff + 999) / 1000;
    }

    public void set(UUID player, String key, int seconds) {
        if (seconds <= 0) {
            return;
        }
        until.put(player + ":" + key, System.currentTimeMillis() + seconds * 1000L);
    }
}
