package com.trillai.trillroot.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Public, non-unique device/build information. No serial, IMEI, accounts, or location. */
public final class DeviceInfo {
    public final String manufacturer;
    public final String brand;
    public final String model;
    public final String device;
    public final String androidVersion;
    public final int sdk;
    public final String securityPatch;
    public final String fingerprint;
    public final String incremental;
    public final String displayId;
    public final String abi;
    public final Map<String, String> properties;

    private DeviceInfo(Builder b) {
        manufacturer = safe(b.manufacturer);
        brand = safe(b.brand);
        model = safe(b.model);
        device = safe(b.device);
        androidVersion = safe(b.androidVersion);
        sdk = b.sdk;
        securityPatch = safe(b.securityPatch);
        fingerprint = safe(b.fingerprint);
        incremental = safe(b.incremental);
        displayId = safe(b.displayId);
        abi = safe(b.abi);
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(b.properties));
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Not reported" : value.trim();
    }

    public String property(String key) {
        String value = properties.get(key);
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    public boolean isSamsung() {
        return manufacturer.toLowerCase(java.util.Locale.ROOT).contains("samsung")
                || brand.toLowerCase(java.util.Locale.ROOT).contains("samsung");
    }

    public String title() {
        return "Not reported".equals(model) ? manufacturer : model;
    }

    public static final class Builder {
        private String manufacturer = "";
        private String brand = "";
        private String model = "";
        private String device = "";
        private String androidVersion = "";
        private int sdk;
        private String securityPatch = "";
        private String fingerprint = "";
        private String incremental = "";
        private String displayId = "";
        private String abi = "";
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Builder manufacturer(String v) { manufacturer = v; return this; }
        public Builder brand(String v) { brand = v; return this; }
        public Builder model(String v) { model = v; return this; }
        public Builder device(String v) { device = v; return this; }
        public Builder androidVersion(String v) { androidVersion = v; return this; }
        public Builder sdk(int v) { sdk = v; return this; }
        public Builder securityPatch(String v) { securityPatch = v; return this; }
        public Builder fingerprint(String v) { fingerprint = v; return this; }
        public Builder incremental(String v) { incremental = v; return this; }
        public Builder displayId(String v) { displayId = v; return this; }
        public Builder abi(String v) { abi = v; return this; }
        public Builder property(String key, String value) {
            if (PropertySignals.ALLOWED.contains(key) && value != null && !value.isEmpty()) {
                properties.put(key, value);
            }
            return this;
        }
        public DeviceInfo build() { return new DeviceInfo(this); }
    }
}
