package fr.uge.clonewareDBClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;

import java.io.File;
import java.io.IOException;

/**
 * Initialize JDBC database schema and populate it with sample data.
 */
public class InitializeDb {

  /**
   * ObjectMapper instance used for all data-binding (about JSON).
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Information types source file.
   */
  private static final String INFORMATION = "/Information.json";

  /**
   * Initialize JDBC database schema and populate it with sample data.
   *
   * @param dbClient database client
   */
  static void init(DbClient dbClient) {
    try {
      initData(dbClient);
    } catch (Exception ex) {
      System.out.printf("Could not initialize database: %s\n", ex.getMessage());
    }
  }

  /**
   * Initializes database schema (tables).
   *
   * @param dbClient database client
   */
  private static void initSchema(DbClient dbClient) {
    try {
      dbClient.execute(exec -> exec
              .namedDml("create-information"))
          .await();
    } catch (Exception ex1) {
      System.out.printf("Could not create tables: %s", ex1.getMessage());
      try {
        deleteData(dbClient);
      } catch (Exception ex2) {
        System.out.printf("Could not delete tables: %s", ex2.getMessage());
      }
    }
  }

  /**
   * InitializeDb database content (rows in tables).
   *
   * @param dbClient database client
   */
  private static void initData(DbClient dbClient) {
    dbClient.execute(exec -> {
          try {
            return initInformation(exec);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .await();
  }

  /**
   * Delete content of all tables.
   *
   * @param dbClient database client
   */
  private static void deleteData(DbClient dbClient) {
    dbClient.execute(exec -> exec
            .namedDelete("delete-all-information"))
        .await();
  }

  /**
   * Initialize information.
   * Source data file is JSON file containing array of type objects:
   * <pre>
   * [
   *   { "filename": ..., "line_nb": ..., "hash": ... },
   *   ...
   * ]
   * </pre>
   * where {@code filename} is JSON String, {@code line_nb} is JSON number on 8 bytes and , {@code hash} is JSON number on 8 bytes.
   *
   * @param exec database client executor
   * @return executed statements future
   */
  private static Single<Long> initInformation(DbExecute exec) throws IOException {
    Single<Long> stage = Single.just(0L);
    var tree = mapper.readTree(new File(INFORMATION));
    for (var jsonNode : tree) {
      var filename = jsonNode.get("filename").asText();
      var line_nb = jsonNode.get("line_nb").asLong();
      var hash = jsonNode.get("hash").asLong();
      stage = stage.flatMapSingle(it -> exec.namedInsert(
          "insert-information", filename, line_nb, hash));
    }
    return stage;
  }

  /**
   * Creates an instance of database initialization.
   */
  private InitializeDb() {
    throw new UnsupportedOperationException("Instances of InitializeDb utility class are not allowed");
  }
}