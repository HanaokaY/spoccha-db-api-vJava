package spoccha.db.api.vjava.controller;

import io.vertx.pgclient.PgPool;
import io.vertx.ext.web.RoutingContext;

public class GymInformationController {

  private PgPool client;

  public GymInformationController(PgPool client) {
    this.client = client;
  }

  public void show(RoutingContext routingContext) {
    // Use this.client to query data and send response
  }

  // Define other actions...
}
