package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemIdentityTest {
    @Test
    void recognizesOnlyTheMarkedCompassForItsOwner() {
        UUID owner = UUID.randomUUID();

        assertTrue(ItemIdentity.isPluginCompass(ItemIdentity.MARKER, owner.toString(), owner));
        assertFalse(ItemIdentity.isPluginCompass((byte) 0, owner.toString(), owner));
        assertFalse(ItemIdentity.isPluginCompass(ItemIdentity.MARKER, UUID.randomUUID().toString(), owner));
        assertFalse(ItemIdentity.isPluginCompass(ItemIdentity.MARKER, "not-a-uuid", owner));
    }
}
