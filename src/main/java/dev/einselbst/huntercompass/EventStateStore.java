package dev.einselbst.huntercompass;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class EventStateStore {
    private final JavaPlugin plugin;
    private final File file;
    private boolean active;
    private UUID targetId;
    private String targetName;
    private long startedAtMillis;
    private long deadlineMillis;
    private final Set<UUID> hunters = new LinkedHashSet<>();

    EventStateStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    void load() {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        active = data.getBoolean("event.active", false);
        targetName = data.getString("event.target-name");
        targetId = parseUuid(data.getString("event.target-uuid"));
        startedAtMillis = data.getLong("event.started-at-epoch-millis", 0L);
        deadlineMillis = data.getLong("event.deadline-epoch-millis", 0L);
        hunters.clear();
        for (String raw : data.getStringList("event.hunters")) {
            UUID id = parseUuid(raw);
            if (id != null && !id.equals(targetId)) {
                hunters.add(id);
            }
        }
        if (active && (targetId == null || targetName == null || startedAtMillis <= 0L
                || deadlineMillis <= startedAtMillis)) {
            plugin.getLogger().warning("Saved event state is incomplete; disabling the event.");
            active = false;
            save();
        }
    }

    void start(UUID id, String name, long startedAtMillis, long deadlineMillis) {
        active = true;
        targetId = id;
        targetName = name;
        this.startedAtMillis = startedAtMillis;
        this.deadlineMillis = deadlineMillis;
        hunters.remove(id);
        save();
    }

    void stop() {
        active = false;
        targetId = null;
        targetName = null;
        startedAtMillis = 0L;
        deadlineMillis = 0L;
        hunters.clear();
        save();
    }

    boolean addHunter(UUID id) {
        if (id.equals(targetId)) {
            return false;
        }
        boolean changed = hunters.add(id);
        if (changed) {
            save();
        }
        return changed;
    }

    boolean removeHunter(UUID id) {
        boolean changed = hunters.remove(id);
        if (changed) {
            save();
        }
        return changed;
    }

    void clearHunters() {
        if (!hunters.isEmpty()) {
            hunters.clear();
            save();
        }
    }

    boolean isActive() {
        return active;
    }

    UUID targetId() {
        return targetId;
    }

    String targetName() {
        return targetName;
    }

    long startedAtMillis() {
        return startedAtMillis;
    }

    long deadlineMillis() {
        return deadlineMillis;
    }

    boolean isHunter(UUID id) {
        return hunters.contains(id);
    }

    Set<UUID> hunters() {
        return Collections.unmodifiableSet(hunters);
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("event.active", active);
        data.set("event.target-uuid", targetId == null ? null : targetId.toString());
        data.set("event.target-name", targetName);
        data.set("event.started-at-epoch-millis", startedAtMillis == 0L ? null : startedAtMillis);
        data.set("event.deadline-epoch-millis", deadlineMillis == 0L ? null : deadlineMillis);
        data.set("event.hunters", hunters.stream().map(UUID::toString).toList());
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save event state: " + exception.getMessage());
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring invalid UUID in data.yml: " + raw);
            return null;
        }
    }
}
