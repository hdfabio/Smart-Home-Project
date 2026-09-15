package org.engcia.integration;

import org.engcia.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Integrations {
    private static final Logger LOG = LoggerFactory.getLogger(Integrations.class);
    private static final JsonRpcServer RPC_SERVER = new JsonRpcServer();
    private static final MqttBridge MQTT_BRIDGE = new MqttBridge();

    public static void start() {
        AppConfig config = AppConfig.get();
        try {
            RPC_SERVER.start(config);
        } catch (Exception e) {
            LOG.warn("JSON-RPC did not start: {}", e.getMessage());
        }
        try {
            MQTT_BRIDGE.start(config);
        } catch (Exception e) {
            LOG.warn("MQTT did not start: {}", e.getMessage());
        }
    }

    public static void stop() {
        try {
            RPC_SERVER.stop();
        } catch (Exception e) {
            LOG.debug("JSON-RPC stop: {}", e.getMessage());
        }
        try {
            MQTT_BRIDGE.stop();
        } catch (Exception e) {
            LOG.debug("MQTT stop: {}", e.getMessage());
        }
    }
}
