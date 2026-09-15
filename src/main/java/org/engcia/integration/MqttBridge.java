package org.engcia.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.engcia.config.AppConfig;
import org.engcia.services.ExpertEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MqttBridge implements MqttCallbackExtended {
    private static final Logger LOG = LoggerFactory.getLogger(MqttBridge.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MqttClient client;

    public void start(AppConfig config) {
        if (!config.isMqttEnabled()) {
            LOG.info("MQTT bridge disabled");
            return;
        }
        String clientId = config.getMqttClientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            client = new MqttClient(config.getMqttBroker(), clientId, new MemoryPersistence());
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            client.connect(options);
            client.subscribe("smarthome/+/consumption", 1);
            client.subscribe("smarthome/+/rpc/request", 1);
            ExpertEngine.get().addRecommendationListener(this::publishRecommendations);
            LOG.info("MQTT connected to {}", config.getMqttBroker());
        } catch (Exception e) {
            LOG.warn("MQTT broker {} unavailable ({}). Using JSON consumer files instead.",
                    config.getMqttBroker(), e.getMessage());
            closeQuietly();
        }
    }

    public void stop() {
        closeQuietly();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOG.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
        try {
            if (client != null && client.isConnected()) {
                client.subscribe("smarthome/+/consumption", 1);
                client.subscribe("smarthome/+/rpc/request", 1);
            }
        } catch (MqttException e) {
            LOG.warn("MQTT resubscribe failed: {}", e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        LOG.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        String consumerId = consumerIdFrom(topic);
        try {
            if (topic.endsWith("/consumption")) {
                JsonNode json = MAPPER.readTree(payload);
                ExpertEngine.get().upsertConsumption(consumerId, json);
                LOG.info("Applied MQTT consumption for consumer {}", consumerId);
            } else if (topic.endsWith("/rpc/request")) {
                String response = JsonRpcHandler.handle(payload, consumerId);
                if (response != null && client != null && client.isConnected()) {
                    client.publish("smarthome/" + consumerId + "/rpc/response",
                            new MqttMessage(response.getBytes(StandardCharsets.UTF_8)));
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to handle MQTT message on {}: {}", topic, e.getMessage());
            if (topic.endsWith("/rpc/request")) {
                publishError(consumerId, e.getMessage());
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }

    private void publishRecommendations(String consumerId, List<String> conclusions) {
        if (client == null || !client.isConnected() || consumerId == null) {
            return;
        }
        try {
            String json = MAPPER.writeValueAsString(Map.of(
                    "consumerId", consumerId,
                    "conclusions", conclusions
            ));
            client.publish("smarthome/" + consumerId + "/recommendations",
                    new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            LOG.warn("Could not publish MQTT recommendations: {}", e.getMessage());
        }
    }

    private void publishError(String consumerId, String message) {
        if (client == null || !client.isConnected() || consumerId == null) {
            return;
        }
        try {
            String json = MAPPER.writeValueAsString(Map.of("error", message == null ? "unknown" : message));
            client.publish("smarthome/" + consumerId + "/rpc/response",
                    new MqttMessage(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            LOG.debug("Could not publish MQTT error: {}", e.getMessage());
        }
    }

    private static String consumerIdFrom(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    private void closeQuietly() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (Exception e) {
            LOG.debug("MQTT close: {}", e.getMessage());
        } finally {
            client = null;
        }
    }
}
