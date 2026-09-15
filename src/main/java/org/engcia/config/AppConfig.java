package org.engcia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;

    private final boolean mqttEnabled;
    private final String mqttBroker;
    private final String mqttClientId;
    private final boolean rpcEnabled;
    private final int rpcPort;

    private AppConfig(Properties properties) {
        this.mqttEnabled = Boolean.parseBoolean(envOr("MQTT_ENABLED", properties.getProperty("mqtt.enabled", "true")));
        this.mqttBroker = envOr("MQTT_BROKER", properties.getProperty("mqtt.broker", "tcp://localhost:1883"));
        this.mqttClientId = envOr("MQTT_CLIENT_ID", properties.getProperty("mqtt.clientId", "smart-home-expert"));
        this.rpcEnabled = Boolean.parseBoolean(envOr("RPC_ENABLED", properties.getProperty("rpc.enabled", "true")));
        this.rpcPort = Integer.parseInt(envOr("RPC_PORT", properties.getProperty("rpc.port", "8080")));
    }

    public static AppConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception e) {
            LOG.warn("Could not load application.properties, using defaults: {}", e.getMessage());
        }
        instance = new AppConfig(properties);
        return instance;
    }

    private static String envOr(String envKey, String fallback) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public boolean isMqttEnabled() {
        return mqttEnabled;
    }

    public String getMqttBroker() {
        return mqttBroker;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public boolean isRpcEnabled() {
        return rpcEnabled;
    }

    public int getRpcPort() {
        return rpcPort;
    }
}
