package spoccha.db.api.vjava;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

    private Map<Integer, Map<Integer, Map<String, Object>>> allData;
    private Pool dbPool;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        initializeDbPool().onComplete(ar -> {
            if (ar.failed()) {
                startPromise.fail(ar.cause());
            } else {
                startHttpServer(startPromise);
            }
        });
    }

    private void startHttpServer(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/api/v1/gym_informations/all_data").handler(this::handleAllData);

        router.get("/api/v1/dbtest").handler(this::handleDbTest);

        HttpServer server = vertx.createHttpServer();
        int port = Integer.parseInt(System.getenv().get("PORT"));
        server.requestHandler(router).listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("Server started on port " + port);
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private Future<Void> initializeDbPool() {
        Promise<Void> promise = Promise.promise();

        String env = System.getenv("env");
        String dbUrl = null;
    
        if (env != null && env.equals("production")) {
            dbUrl = System.getenv("DATABASE");
        } else {
            Dotenv dotenv = Dotenv.configure().load();
            dbUrl = dotenv.get("DATABASE_URL").substring(5);
        }

        String username = "";
        String password = "";
        String host = "";
        int port = 0;
        String database = "";

        Pattern pattern = Pattern.compile("postgresql://(.*):(\\d+)/([^\\?]*)\\?user=([^&]*)&password=(.*)");
        Matcher matcher = pattern.matcher(dbUrl);

        if (matcher.find()) {
            host = matcher.group(1);
            port = Integer.parseInt(matcher.group(2));
            database = matcher.group(3);
            username = matcher.group(4);
            password = matcher.group(5);
        } else {
            System.err.println("Unable to parse DATABASE_URL");
            return;
        }

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(database)
                .setUser(username)
                .setPassword(password)
                .setSsl(true)
                .setTrustAll(true);

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(5);

        dbPool = PgPool.pool(vertx, connectOptions, poolOptions);

        dbPool.query("SELECT 1")
            .execute(ar -> {
                if (ar.failed()) {
                    System.err.println("Failed to establish a connection with the database: " + ar.cause().getMessage());
                    promise.fail(ar.cause());
                } else {
                    System.out.println("Successfully connected to the database");
                    promise.complete();
                }
            });
        
        return promise.future();
    }

    private void handleAllData(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json");

        dbPool.query("SELECT * FROM prefectures p " +
                "JOIN cities c ON p.id = c.prefecture_id " +
                "JOIN gyms g ON c.id = g.city_id " +
                "JOIN schedule_urls s ON g.id = s.gym_id")
                .execute(ar -> {
                    if (ar.failed()) {
                        System.err.println("Failed to fetch data from the database: " + ar.cause().getMessage());
                        response.setStatusCode(500).end();
                    } else {
                        RowSet<Row> result = ar.result();
                        Map<String, Object> responseMap = new HashMap<>();

                        for (Row row : result) {
                            // process each row and build responseMap
                        }

                        String responseJson = Json.encode(responseMap);
                        response.end(responseJson);
                    }
                });
    }

    private void handleDbTest(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        dbPool.query("SELECT 1")
            .execute(ar -> {
                if (ar.failed()) {
                    response.putHeader("content-type", "application/json").end(Json.encodePrettily(
                            new JsonObject().put("status", "failure").put("message", "Failed to establish a connection with the database: " + ar.cause().getMessage())
                    ));
                } else {
                    response.putHeader("content-type", "application/json").end(Json.encodePrettily(
                            new JsonObject().put("status", "success").put("message", "Successfully connected to the database")
                    ));
                }
            });
    }
}
