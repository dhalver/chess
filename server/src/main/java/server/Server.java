package server;

import io.javalin.Javalin;

public class Server {
    private Javalin app;

    public int run(int desiredPort) {
        app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        });

        app.delete("/db", ctx -> {
            ctx.status(200);
            ctx.result("{}");
        });

        app.post("/user", ctx -> {
            ctx.status(200);
            ctx.result("{}");
        });

        app.post("/session", ctx -> {
            ctx.status(200);
            ctx.result("{}");
        });

        app.delete("/session", ctx -> {
            ctx.status(200);
            ctx.result("{}");
        });

        app.get("/game", ctx -> {
            ctx.status(200);
            ctx.result("{\"games\":[]}");
        });

        app.post("/game", ctx -> {
            ctx.status(200);
            ctx.result("{\"gameID\":1}");
        });

        app.put("/game", ctx -> {
            ctx.status(200);
            ctx.result("{}");
        });

        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
