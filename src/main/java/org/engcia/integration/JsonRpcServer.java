package org.engcia.integration;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.engcia.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcServer.class);
    private Javalin app;

    public void start(AppConfig config) {
        if (!config.isRpcEnabled()) {
            LOG.info("JSON-RPC HTTP server disabled");
            return;
        }
        try {
            app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
            app.post("/rpc", this::handleRpc);
            app.get("/health", ctx -> ctx.json(java.util.Map.of("status", "ok")));
            app.start(config.getRpcPort());
            LOG.info("JSON-RPC listening on http://localhost:{}/rpc", config.getRpcPort());
        } catch (Exception e) {
            LOG.warn("JSON-RPC HTTP server did not start ({}). Desktop app will still run.", e.getMessage());
            app = null;
        }
    }

    private void handleRpc(Context ctx) {
        ctx.contentType("application/json");
        String response = JsonRpcHandler.handle(ctx.body());
        ctx.result(response == null ? "" : response);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }
}
