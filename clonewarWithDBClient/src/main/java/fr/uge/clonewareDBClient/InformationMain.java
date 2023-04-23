package fr.uge.clonewareDBClient;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

public class InformationMain {

  private static final Http.HeaderValue SERVER = Http.Header.create(Http.Header.SERVER, "Nima");

  /**
   * Cannot be instantiated.
   */
  private InformationMain() {
  }

  /**
   * Application main entry point.
   *
   * @param args Command line arguments. Run with JDBC support.
   */
  public static void main(final String[] args) {
    System.out.println("JDBC database selected");
    startServer();
  }

  /**
   * Start the server.
   *
   * @return the created {@link io.helidon.nima.webserver.WebServer} instance
   */
  static WebServer startServer() {

    // By default, this will pick up application.yaml from the classpath
    Config config = Config.create();

    // Default server port if no one is specified in application.yaml
    var defaultPort = 8080;

    WebServer server = WebServer.builder()
        .routing(InformationMain::createRouting)
        .port(config.get("server").get("port").asInt().orElse(defaultPort))
        .start();
    return server;
  }

  static void createRouting(HttpRouting.Builder rules) {
    // By default, this will pick up application.yaml from the classpath
    Config config = Config.create();

    Config dbConfig = config.get("db");

    // Client services are added through a service loader
    DbClient dbClient = DbClient.builder(dbConfig)
        .build();

    // Initialize database schema
    InitializeDb.init(dbClient);

    rules.addFilter((chain, req, res) -> {
          res.header(SERVER);
          chain.proceed();
        })
        .register("/db", new InformationService(dbClient));
  }
}
