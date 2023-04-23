package fr.uge.clonewareDBClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.common.http.Http;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.http.*;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example service using a database.
 */
public class InformationService implements HttpService {
  private static final Logger LOGGER = Logger.getLogger(InformationService.class.getName());

  /**
   * ObjectMapper instance used for all data-binding (about JSON).
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  // we use this approach as we are calling the same service
  // in a real application, we would use DNS resolving, or k8s service names
  private static Http1Client client;

  private final DbClient dbClient;

  static void client(Http1Client client) {
    InformationService.client = client;
  }

  InformationService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void routing(HttpRules httpRules) {
    httpRules
        .get("/", this::index)
        // List all information
        .get("/information", this::listInformation)
        // Get information by filename
        .get("/information/filename/{filename}", this::getInformationByFilename)
        // Get information by line_nb
        .get("/information/line_nb/{line_nb}", this::getInformationByLine_nb)
        // Get information by hash
        .get("/information/hash/{hash}", this::getInformationByHash)
        // Create new information
        .post("/information/create/{filename}/{line_nb}/{hash}", this::insertInformation)
        // Update filename of existing information
        .put("/information/modify/{filename}/{line_nb}/{hash}", this::updateInformation)
        // Delete information by filename
        .post("/information/delete/{filename}", this::deleteInformationByFilename)
        // Delete all information
        .delete(this::deleteAllInformation);
  }

  private static Http1Client client() {
    if (client == null) {
      throw new RuntimeException("Client must be configured on BlockingService");
    }
    return client;
  }

  /**
   * Return index page.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void index(ServerRequest request, ServerResponse response) {
    response.send("Information JDBC and DBClient example:\n"
        + "     GET /information                                        - List all information\n"
        + "     GET /information/filename/{filename}                    - Get information by filename\n"
        + "     GET /information/line_nb/{line_nb}                      - Get information by line_nb\n"
        + "     GET /information/hash/{hash}                            - Get information by hash\n"
        + "    POST /information/create/{filename}/{line_nb}/{hash}     - Insert new information:\n"
        + "                                                               {\"filename\":<filename>,\"line_nb\":<line_nb>,\"hash\":<hash>}\n"
        + "     PUT /information/modify/{filename}/{line_nb}/{hash}     - Update information\n"
        + "                                                               {\"filename\":<filename>,\"line_nb\":<line_nb>,\"hash\":<hash>}\n"
        + "  DELETE /information/delete/{filename}                      - Delete by filename\n"
        + "                                                               {\"filename\":<filename>}\n"
        + "  DELETE                                                     - Delete all information\n");
  }

  /**
   * Return Json with all stored information.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void listInformation(ServerRequest request, ServerResponse response) {
    response.send(dbClient.execute(exec -> exec.namedQuery("select-all-information"))
        .map(it -> {
          ObjectNode jsonNode = mapper.createObjectNode()
              .put("filename", it.column("filename").as(String.class))
              .put("line_nb", it.column("line_nb").as(long.class))
              .put("hash", it.column("hash").as(long.class));
          try {
            return mapper.writeValueAsString(jsonNode);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
    );
  }

  /**
   * Get single information by name.
   *
   * @param request  server request
   * @param response server response
   */
  private void getInformationByFilename(ServerRequest request, ServerResponse response) {
    String filename = request.path().pathParameters().value("filename");
    dbClient.execute(exec -> exec.namedGet("select-information-by-filename", filename))
        .thenAccept(it -> {
          if (it.isEmpty()) {
            sendNotFound(response, "Information from " + filename + " not found");
          } else {
            sendRow(it.get(), response);
          }
        })
        .exceptionally(throwable -> sendError(throwable, response));
  }

  /**
   * Get a single information by line_nb.
   *
   * @param request  server request
   * @param response server response
   */
  private void getInformationByLine_nb(ServerRequest request, ServerResponse response) {
    try {
      var line_nb = Long.parseLong(request.path().pathParameters().value("line_nb"));
      dbClient.execute(exec -> exec
              .createNamedGet("select-information-by-line_nb")
              .addParam("line_nb", line_nb)
              .execute())
          .thenAccept(maybeRow -> maybeRow
              .ifPresentOrElse(
                  row -> sendRow(row, response),
                  () -> sendNotFound(response, "Information about line " + line_nb + " not found")))
          .exceptionally(throwable -> sendError(throwable, response));
    } catch (NumberFormatException ex) {
      sendError(ex, response);
    }
  }

  /**
   * Get single information by hash.
   *
   * @param request  server request
   * @param response server response
   */
  private void getInformationByHash(ServerRequest request, ServerResponse response) {
    try {
      var hash = Long.parseLong(request.path().pathParameters().value("hash"));
      dbClient.execute(exec -> exec
              .createNamedGet("select-information-by-hash")
              .addParam("hash", hash)
              .execute())
          .thenAccept(maybeRow -> maybeRow
              .ifPresentOrElse(
                  row -> sendRow(row, response),
                  () -> sendNotFound(response, "Information of hash " + hash + " not found")))
          .exceptionally(throwable -> sendError(throwable, response));
    } catch (NumberFormatException ex) {
      sendError(ex, response);
    }
  }

  /**
   * Insert new information with specified filename, line_nb and hash.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void insertInformation(ServerRequest request, ServerResponse response) {
    try {
      var filename = request.path().pathParameters().value("filename");
      var line_nb = Long.parseLong(request.path().pathParameters().value("line_nb"));
      var hash = Long.parseLong(request.path().pathParameters().value("hash"));
      var information = new Information(filename, line_nb, hash);
      dbClient.execute(exec -> exec
              .createNamedInsert("insert-information")
              .indexedParam(information)
              .execute())
          .thenAccept(count -> response.send("Inserted : " + count + " values\n"))
          .exceptionally(throwable -> sendError(throwable, response));
    } catch (NumberFormatException ex) {
      sendError(ex, response);
    }
  }

  /**
   * Update an information.
   * Uses a transaction.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void updateInformation(ServerRequest request, ServerResponse response) {
    try {
      var filename = request.path().pathParameters().value("filename");
      var line_nb = Long.parseLong(request.path().pathParameters().value("line_nb"));
      var hash = Long.parseLong(request.path().pathParameters().value("hash"));
      var information = new Information(filename, line_nb, hash);
      dbClient.execute(exec -> exec
              .createNamedUpdate("update-information-by-filename-and-line_nb")
              .namedParam(information)
              .execute())
          .thenAccept(count -> response.send("Updated : " + count + " values\n"))
          .exceptionally(throwable -> sendError(throwable, response));
    } catch (NumberFormatException ex) {
      sendError(ex, response);
    }
  }

  /**
   * Delete information with specified filename.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void deleteInformationByFilename(ServerRequest request, ServerResponse response) {
    String informationFilename = request.path().pathParameters().value("filename");
    dbClient.execute(exec -> exec
            .createNamedDelete("delete-information-by-filename")
            .addParam("filename", informationFilename)
            .execute())
        .thenAccept(count -> response.send("Deleted : " + count + " values\n"))
        .exceptionally(throwable -> sendError(throwable, response));

  }

  /**
   * Delete all information.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void deleteAllInformation(ServerRequest request, ServerResponse response) {
    // Information must be removed from Information tables in transaction
    dbClient.execute(exec -> exec
            // Execute delete from Information table
            .createDelete("DELETE FROM Information")
            .execute())
        // Process response when transaction is completed
        .thenAccept(count -> response.send("Deleted : " + count + " values\n"))
        .exceptionally(throwable -> sendError(throwable, response));
  }

  /**
   * Send a 404 status code.
   *
   * @param response the server response
   * @param message  entity content
   */
  private void sendNotFound(ServerResponse response, String message) {
    response.status(Http.Status.NOT_FOUND_404);
    response.send(message);
  }

  /**
   * Send a single DB row as JSON object.
   *
   * @param row      row as read from the database
   * @param response server response
   */
  private void sendRow(DbRow row, ServerResponse response) {
    response.send(row.as(ObjectNode.class));
  }

  /**
   * Send a 500 response code and a few details.
   *
   * @param throwable throwable that caused the issue
   * @param response  server response
   * @param <T>       type of expected response, will be always {@code null}
   * @return {@code Void} so this method can be registered as a lambda
   * with {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}
   */
  private <T> T sendError(Throwable throwable, ServerResponse response) {
    Throwable realCause;
    switch (throwable) {
      case CompletionException e -> realCause = throwable.getCause();
      default -> realCause = throwable;
    }
    response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
    response.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
    LOGGER.log(Level.WARNING, "Failed to process request", throwable);
    return null;
  }
}
