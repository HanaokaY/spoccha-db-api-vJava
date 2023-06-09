package spoccha.db.api.vjava;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

    private Map<Integer, Map<Integer, Map<String, Object>>> allData;
    private Pool dbPool;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        initializeDbPool();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/api/v1/gym_informations/all_data").handler(this::handleAllData);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(3000, result -> {
            if (result.succeeded()) {
                System.out.println("Server started on port 3000");
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private void initializeDbPool() {

        Dotenv dotenv = Dotenv.configure().load();
    
        String dbHost = dotenv.get("DB_HOST");
        int dbPort = Integer.parseInt(dotenv.get("DB_PORT"));
        String dbName = dotenv.get("DB_NAME");
        String dbUrl = dotenv.get("DB_URL");
        String dbUser = dotenv.get("DB_USER");
        String dbPassword = dotenv.get("DB_PASSWORD");

        PgConnectOptions connectOptions = new PgConnectOptions()
            .setHost(dbHost)
            .setPort(dbPort)
            .setDatabase(dbName)
            .setUser(dbUser)
            .setPassword(dbPassword);
    
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(5);
    
        dbPool = PgPool.pool(vertx, connectOptions, poolOptions);
    

        // Test database connection
        dbPool.query("SELECT 1")
            .execute(ar -> {
                if (ar.failed()) {
                    System.err.println("Failed to establish a connection with the database: " + ar.cause().getMessage());
                } else {
                    System.out.println("Successfully connected to the database");
                }
            });
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

}
