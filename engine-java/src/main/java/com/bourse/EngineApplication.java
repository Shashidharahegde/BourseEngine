import io.javalin.Javalin;

void main() {
    var app = Javalin.create(config -> {
        config.routes.get("/", ctx -> ctx.result("Hello World"));
    }).start(7070);
}