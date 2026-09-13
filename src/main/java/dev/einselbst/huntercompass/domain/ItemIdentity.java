package dev.einselbst.huntercompass.domain;

import java.util.UUID;

public final class ItemIdentity {
    public static final byte MARKER = 1;

    private ItemIdentity() {
    }

    public static boolean isPluginCompass(Byte marker, String owner, UUID expectedOwner) {
        if (marker == null || marker != MARKER || owner == null || expectedOwner == null) {
            return false;
        }
        try {
            return UUID.fromString(owner).equals(expectedOwner);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
