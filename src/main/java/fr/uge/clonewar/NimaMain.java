package fr.uge.clonewar;

import io.helidon.common.http.Http;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.cors.CorsSupport;
import io.helidon.nima.webserver.cors.CrossOriginConfig;
import io.helidon.nima.webserver.http.HttpRouting;
import io.helidon.nima.webserver.staticcontent.StaticContentSupport;

import java.nio.file.Path;

/**
 * Allow to use Helidon Nima server.
 *
 * @author Dylan DE JESUS & Vincent RICHARD
 *
 */
public class NimaMain {

  /**
   * HTTP header value.
   */
  private static final Http.HeaderValue SERVER = Http.Header.create(Http.Header.SERVER, "Nima");

  /**
   * CorsSupport instance used to configure cross-origin resource sharing (CORS).
   */
  private static final CorsSupport CORS_SUPPORT = CorsSupport.builder()
      .addCrossOrigin(CrossOriginConfig.builder()
          .allowOrigins("http://127.0.0.1:8080", "http://localhost:8080", "http://127.0.0.1:3000", "http://localhost:3000")
          .build())
      .build();

  /**
   * Allow to configure some server routes.
   *
   * @param rules HTTP routing builder
   */
  static void routing(HttpRouting.Builder rules) {
    rules.addFilter((chain, req, res) -> {
          res.header(SERVER);
          chain.proceed();
        })
        .register("/", CORS_SUPPORT, new Asm(Path.of("hashesBackupFile.txt"), Path.of("jarsBackupFile.txt")))
        .register("/", CORS_SUPPORT, StaticContentSupport.builder("/static-content"));
  }

  /**
   * Main method.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    WebServer ws = WebServer.builder()
        .routing(NimaMain::routing)
        .port(8080)
        .start();
  }
}