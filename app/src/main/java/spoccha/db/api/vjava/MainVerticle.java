package spoccha.db.api.vjava;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

    private Map<Integer, String> allCities;
    private Map<Integer, String> allPrefectures;
    private Map<Integer, Map<Integer, Map<String, Object>>> allData;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        initializeData();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/api/v1/gym_informations/all_cities").handler(this::handleAllCities);
        router.get("/api/v1/gym_informations/all_prefectures").handler(this::handleAllPrefectures);
        router.get("/api/v1/gym_informations/all_data").handler(this::handleAllData);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(8080, result -> {
            if (result.succeeded()) {
                System.out.println("Server started on port 8080");
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private void initializeData() {
        allCities = new HashMap<>();
        allCities.put(1, "City A");
        allCities.put(2, "City B");
        allCities.put(3, "City C");

        allPrefectures = new HashMap<>();
        allPrefectures.put(1, "Prefecture X");
        allPrefectures.put(2, "Prefecture Y");
        allPrefectures.put(3, "Prefecture Z");

        allData = new HashMap<>();
        Map<Integer, Map<String, Object>> cityData = new HashMap<>();
        cityData.put(1, createGymData("Gym 1", "Schedule 1", "Schedule 2"));
        cityData.put(2, createGymData("Gym 2", "Schedule 3", "Schedule 4"));
        cityData.put(3, createGymData("Gym 3", "Schedule 5", "Schedule 6"));
        allData.put(1, cityData);
    }

    private Map<String, Object> createGymData(String name, String... schedules) {
        Map<String, Object> gymData = new HashMap<>();
        gymData.put("name", name);
        gymData.put("schedule_urls", schedules);
        return gymData;
    }

    private void handleAllCities(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json");
        response.end(allCities.toString());
    }

    private void handleAllPrefectures(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json");
        response.end(allPrefectures.toString());
    }

    private void handleAllData(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json");
        response.end(allData.toString());
    }
}
