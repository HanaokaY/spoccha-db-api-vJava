package spoccha.db.api.vjava;

import java.util.ArrayList;
import java.util.List;
import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {
  private PgPool pgPool;

  @Override
  public void start(Promise<Void> startPromise) {
    // Initialize the PgPool instance
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(Integer.parseInt(System.getenv("DB_PORT")))
      .setHost(System.getenv("DB_HOST"))
      .setDatabase(System.getenv("DB_NAME"))
      .setUser(System.getenv("DB_USER"))
      .setPassword(System.getenv("DB_PASSWORD"));

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);

    pgPool = PgPool.pool(vertx, connectOptions, poolOptions);

    // routes
    Router router = Router.router(vertx);
    router.get("/api/v1/gym_informations/all_data").handler(this::handleGetAllData);

    vertx.createHttpServer().requestHandler(router).listen(8080);
    startPromise.complete();
  }

  // 都道府県、市区町村、体育館のデータを全て取得する

  private void handleGetAllData(RoutingContext routingContext) {
    // responseの型をJsonObjectに指定
    JsonObject response = new JsonObject();
    List<Future> cityQueries = new ArrayList<>();

    pgPool.preparedQuery("SELECT * FROM prefectures")
        .execute()
        .onSuccess(prefecturesRows -> {
            for (Row prefecture : prefecturesRows) {
                String prefectureName = prefecture.getString("name");
                Integer prefectureId = prefecture.getInteger("id");

                Future<RowSet<Row>> cityQuery = pgPool.preparedQuery("SELECT * FROM cities WHERE prefecture_id = $1")
                    .execute(Tuple.of(prefectureId))
                    .compose(citiesRows -> {
                        JsonObject cities = new JsonObject();

                        for (Row city : citiesRows) {
                            String cityName = city.getString("name");
                            Integer cityId = city.getInteger("id");

                            Future<RowSet<Row>> gymQuery = pgPool.preparedQuery("SELECT * FROM gyms WHERE city_id = $1")
                                .execute(Tuple.of(cityId))
                                .compose(gymsRows -> {
                                    JsonArray gyms = new JsonArray();

                                    for (Row gym : gymsRows) {
                                        JsonObject gymObject = new JsonObject();
                                        gymObject.put("id", gym.getInteger("id"));
                                        gymObject.put("name", gym.getString("name"));
                                        gymObject.put("schedule_url", gym.getString("schedule_url"));
                                        gyms.add(gymObject);
                                    }

                                    cities.put(cityName, gyms);
                                    return Future.succeededFuture();
                                });

                            cityQueries.add(gymQuery);
                        }

                        response.put(prefectureName, cities);
                        return Future.succeededFuture();
                    });

                cityQueries.add(cityQuery);
            }
        })
        .onFailure(err -> routingContext.response()
            .setStatusCode(500).end(err.getMessage()));

    CompositeFuture.all(cityQueries).onSuccess(v -> routingContext.response()
        .putHeader("content-type", "application/json")
        // responseがJsonObjectであるため、encodeメソッドが利用可能
        .end(response.encode()))
        .onFailure(err -> routingContext.response()
            .setStatusCode(500).end(err.getMessage()));
  }
  // 他のハンドラーのメソッドは下記に書いていく

}
