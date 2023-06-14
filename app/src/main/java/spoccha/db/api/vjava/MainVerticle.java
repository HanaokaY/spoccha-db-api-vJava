package spoccha.db.api.vjava;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private Map<Integer, Map<Integer, Map<String, Object>>> allData;
    private HikariDataSource ds;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(String[] args) {
        LOGGER.info("Vert.xアプリケーションを開始します...");
        Vertx.vertx().deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        LOGGER.info("start()が呼び出されました");
        startHttpServer(startPromise);
    }


    private void startHttpServer(Promise<Void> startPromise) {
        LOGGER.info("startHttpServer()が呼び出されました");
        Router router = Router.router(vertx);
        LOGGER.info("ルーティングを設定します...");
        router.route().handler(BodyHandler.create());
        LOGGER.info("BodyHandlerを設定しました");

        router.get("/api/v1/gym_informations/all_data").handler(this::handleAllData);

        router.get("/api/v1/dbtest").handler(this::handleDbTest);

        HttpServer server = vertx.createHttpServer();
        LOGGER.info("HTTPサーバーを作成しました");
        int port = Integer.parseInt(System.getenv("PORT"));
        LOGGER.info("PORT番号 => {}", port);

        initializeDbPool().onComplete(dbInitResult -> {
            if (dbInitResult.succeeded()) {
                server.requestHandler(router).listen(port, result -> {
                    if (result.succeeded()) {
                        LOGGER.info("サーバーがポート{}で起動しました", port);
                        startPromise.complete();
                        LOGGER.info("startHttpServer()が正常に完了しました");
                    } else {
                        LOGGER.error("サーバーの起動に失敗しました", result.cause());
                        startPromise.fail(result.cause());
                    }
                });
            } else {
                LOGGER.error("データベースプールの初期化に失敗しました", dbInitResult.cause());
                startPromise.fail(dbInitResult.cause());
            }
        });
    }
    

    private Future<Void> initializeDbPool() {
        Promise<Void> promise = Promise.promise();
    
        String dyno = System.getenv("DYNO");
        boolean isProduction = (dyno != null && !dyno.isEmpty());
        String dbUrl = null;

        LOGGER.info("DATABASE_URLを取得します...");

        if (isProduction) {
            LOGGER.info("環境変数からDATABASE_URLを読み込みます...");
            dbUrl = System.getenv("DATABASE_URL");
        } else {
            LOGGER.info(".envファイルを読み込みます...");
            Dotenv dotenv = Dotenv.configure().load();
            dbUrl = dotenv.get("DATABASE_URL");
        }
    
        if (dbUrl == null) {
            LOGGER.error("DATABASE_URLが見つかりません");
            promise.fail(new RuntimeException("DATABASE_URLが見つかりません"));
            return promise.future();
        }
    
        LOGGER.info("DATABASE_URLを解析します...");
    
        String username = "";
        String password = "";
        String host = "";
        int port = 0;
        String database = "";
    
        Pattern pattern = Pattern.compile("postgres://(.*):(.*?)@(.*):(\\d+)/(\\w+)");
        Matcher matcher = pattern.matcher(dbUrl);
    
        if (matcher.find()) {
            username = matcher.group(1);
            password = matcher.group(2);
            host = matcher.group(3);
            port = Integer.parseInt(matcher.group(4));
            database = matcher.group(5);
        } else {
            LOGGER.error("DATABASE_URLの解析に失敗しました");
            promise.fail(new RuntimeException("DATABASE_URLの解析に失敗しました"));
            return promise.future();
        }

        int check_port = Integer.parseInt(System.getenv("PORT"));
        LOGGER.info("check_port => {}", check_port);
    
        LOGGER.info("DATABASE_URLの確認");
        LOGGER.info("username => {}", username);
        LOGGER.info("password => {}", password);
        LOGGER.info("host => {}", host);
        LOGGER.info("port => {}", port);
        LOGGER.info("database => {}", database);

        LOGGER.info("データベース接続プールを作成します...");
    
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("sslmode", "require");
        config.setMaximumPoolSize(5);

        ds = new HikariDataSource(config);
    
        try {
            ds.getConnection().close();
            LOGGER.info("データベースへの接続に成功しました");
            promise.complete();
        } catch (SQLException e) {
            LOGGER.error("データベースへの接続に失敗しました: ", e);
            promise.fail(e);
        }
    
        return promise.future();
    }

    private void handleAllData(RoutingContext routingContext) {
        LOGGER.info("handleAllData()が呼び出されました");
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json");
    
        if (ds == null) {
            LOGGER.error("データベースへの接続に失敗しました");
            response.setStatusCode(500).end();
            return;
        }
        // テスト完了後、all_dataを実装する。
    }

    private void handleDbTest(RoutingContext routingContext) {
        LOGGER.info("handleDbTest()が呼び出されました");
        HttpServerResponse response = routingContext.response();
        if (ds == null) {
            response.putHeader("content-type", "application/json").end(Json.encodePrettily(
                    new JsonObject().put("status", "failure").put("message", "データベースへの接続に失敗しました")
            ));
            return;
        }
    
        try {
            ds.getConnection().close();
            response.putHeader("content-type", "application/json").end(Json.encodePrettily(
                    new JsonObject().put("status", "success").put("message", "データベースへの接続に成功しました")
            ));
        } catch (SQLException e) {
            response.putHeader("content-type", "application/json").end(Json.encodePrettily(
                    new JsonObject().put("status", "failure").put("message", "データベースへの接続に失敗しました: " + e.getMessage())
            ));
        }
    }
}
