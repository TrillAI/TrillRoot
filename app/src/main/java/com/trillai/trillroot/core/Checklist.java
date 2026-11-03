package com.trillai.trillroot.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Checklist {
    public static final class Item {
        public final String id;
        public final String title;
        public final String detail;
        Item(String id, String title, String detail) {
            this.id = id; this.title = title; this.detail = detail;
        }
    }
    public static final List<Item> ITEMS = Collections.unmodifiableList(Arrays.asList(
            new Item("ownership", "I own this device or have explicit permission", "Do not modify a managed, carrier-restricted, or another person's device without authorization."),
            new Item("backup", "My data is backed up and verified", "Bootloader unlocking normally wipes data. Keep copies of photos, authenticator recovery codes, and app data."),
            new Item("eligibility", "I checked official unlock eligibility", "Confirm the exact model, carrier, region, and account requirements with the manufacturer. Do not bypass restrictions."),
            new Item("firmware", "I have matching official firmware", "Match the installed build and keep stock recovery images. A similar model or Android version is not enough."),
            new Item("tools", "I have a computer and reliable USB cable", "Use official Android platform-tools and the root project's current documentation."),
            new Item("recovery", "I understand the recovery procedure", "Know how to restore the exact stock firmware before changing boot images."),
            new Item("consequences", "I accept the security and warranty trade-offs", "Root and unlocking can affect attestation, banking, DRM, updates, warranty, and Samsung Knox."),
            new Item("norelock", "I will not relock with modified images", "Relocking after modification can make a device unbootable. Follow the official restoration procedure first.")
    ));
    private Checklist() { }

    public static int completed(Iterable<String> checked) {
        int count = 0;
        for (Item item : ITEMS) for (String id : checked) if (item.id.equals(id)) { count++; break; }
        return count;
    }
}
