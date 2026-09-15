package org.engcia.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.engcia.model.QuestionCatalog;
import org.engcia.services.ExpertEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class JsonRpcHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String handle(String body) {
        return handle(body, null);
    }

    public static String handle(String body, String topicConsumerId) {
        JsonNode request;
        try {
            request = MAPPER.readTree(body);
        } catch (Exception e) {
            return error(null, -32700, "Parse error");
        }
        if (request == null || !request.isObject()) {
            return error(null, -32600, "Invalid Request");
        }

        JsonNode id = request.get("id");
        String method = text(request.get("method"));
        JsonNode params = request.get("params");
        if (method == null || method.isBlank()) {
            return error(id, -32600, "Invalid Request: missing method");
        }

        try {
            if (topicConsumerId != null && !topicConsumerId.isBlank() && !"setConsumer".equals(method)) {
                ensureConsumer(topicConsumerId);
            }
            Object result = dispatch(method, params, topicConsumerId);
            if (id == null || id.isNull()) {
                return null;
            }
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("result", MAPPER.valueToTree(result));
            response.set("id", id);
            return MAPPER.writeValueAsString(response);
        } catch (IllegalArgumentException e) {
            return error(id, -32602, e.getMessage());
        } catch (UnsupportedOperationException e) {
            return error(id, -32601, e.getMessage());
        } catch (Exception e) {
            LOG.warn("JSON-RPC method {} failed: {}", method, e.getMessage());
            return error(id, -32603, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static Object dispatch(String method, JsonNode params, String topicConsumerId) throws Exception {
        ExpertEngine engine = ExpertEngine.get();
        switch (method) {
            case "setConsumer": {
                String consumerId = firstText(params, "consumerId", topicConsumerId);
                if (consumerId == null || consumerId.isBlank()) {
                    throw new IllegalArgumentException("consumerId is required");
                }
                try {
                    engine.startConsumer(consumerId);
                } catch (java.io.IOException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
                return Map.of("consumerId", engine.getConsumerId());
            }
            case "getQuestions":
                requireSession(engine);
                return engine.getQuestions();
            case "answer": {
                requireSession(engine);
                String description = requiredText(params, "description");
                JsonNode valueNode = param(params, "value");
                if (valueNode == null || valueNode.isNull()) {
                    throw new IllegalArgumentException("value is required");
                }
                @SuppressWarnings("unchecked")
                List<String> numerical = (List<String>) engine.getQuestions().get("numerical");
                boolean numericalQuestion = (numerical != null && numerical.contains(description)) || valueNode.isNumber();
                QuestionCatalog.QuestionDef def = QuestionCatalog.get(description);
                if (def != null) {
                    numericalQuestion = def.isNumerical();
                }
                if (numericalQuestion) {
                    double number = valueNode.isNumber() ? valueNode.asDouble() : Double.parseDouble(valueNode.asText());
                    engine.answerNumerical(description, number);
                } else {
                    engine.answer(description, valueNode.asText());
                }
                return Map.of("ok", true, "pending", engine.getQuestions());
            }
            case "recommend":
                requireSession(engine);
                List<String> conclusions = engine.recommend();
                return Map.of("consumerId", engine.getConsumerId() == null ? "" : engine.getConsumerId(),
                        "conclusions", conclusions);
            case "how": {
                requireSession(engine);
                JsonNode factId = param(params, "factId");
                if (factId == null || !factId.isNumber()) {
                    throw new IllegalArgumentException("factId is required");
                }
                return Map.of("explanation", engine.how(factId.asInt()));
            }
            case "whyNot": {
                requireSession(engine);
                String expected = requiredText(params, "expectedConclusion");
                return Map.of("explanation", engine.whyNot(expected));
            }
            case "upsertConsumption": {
                String consumerId = firstText(params, "consumerId", topicConsumerId);
                JsonNode payload = params;
                if (params != null && params.has("payload")) {
                    payload = params.get("payload");
                } else if (params != null && params.isObject()) {
                    ObjectNode copy = params.deepCopy();
                    copy.remove("consumerId");
                    payload = copy;
                }
                engine.upsertConsumption(consumerId, payload);
                return Map.of("ok", true, "consumerId", engine.getConsumerId() == null ? consumerId : engine.getConsumerId());
            }
            default:
                throw new UnsupportedOperationException("Method not found: " + method);
        }
    }

    private static void ensureConsumer(String consumerId) throws Exception {
        ExpertEngine engine = ExpertEngine.get();
        if (!consumerId.equals(engine.getConsumerId())) {
            engine.startConsumer(consumerId);
        }
    }

    private static void requireSession(ExpertEngine engine) {
        if (!engine.hasSession()) {
            throw new IllegalArgumentException("No consumer session. Call setConsumer first.");
        }
    }

    private static JsonNode param(JsonNode params, String name) {
        if (params == null || params.isNull()) {
            return null;
        }
        if (params.isObject()) {
            return params.get(name);
        }
        return null;
    }

    private static String requiredText(JsonNode params, String name) {
        String value = text(param(params, name));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String firstText(JsonNode params, String name, String fallback) {
        String value = text(param(params, name));
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private static String error(JsonNode id, int code, String message) {
        try {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("jsonrpc", "2.0");
            ObjectNode error = response.putObject("error");
            error.put("code", code);
            error.put("message", message);
            if (id == null) {
                response.putNull("id");
            } else {
                response.set("id", id);
            }
            return MAPPER.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"},\"id\":null}";
        }
    }
}
