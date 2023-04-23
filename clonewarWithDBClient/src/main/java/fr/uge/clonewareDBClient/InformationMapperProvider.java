package fr.uge.clonewareDBClient;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.Optional;

/**
 * Provides information mappers.
 */
public class InformationMapperProvider implements DbMapperProvider {
  private static final InformationMapper MAPPER = new InformationMapper();

  @Override
  public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
    if (type.equals(Information.class)) {
      return Optional.of((DbMapper<T>) MAPPER);
    }
    return Optional.empty();
  }
}
