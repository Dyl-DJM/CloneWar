package fr.uge.clonewareDBClient;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps database statements to Information class.
 */
public class InformationMapper implements DbMapper<Information> {

  @Override
  public Information read(DbRow row) {
    DbColumn filename = row.column("filename");
    DbColumn line_nb = row.column("line_nb");
    DbColumn hash = row.column("hash");
    return new Information(filename.as(String.class), line_nb.as(long.class), hash.as(long.class));
  }

  @Override
  public Map<String, ?> toNamedParameters(Information information) {
    Map<String, Object> map = new HashMap<>(3);
    map.put("filename", information.getFilename());
    map.put("line_nb", information.getLine_nb());
    map.put("hash", information.getHash());
    return map;
  }

  @Override
  public List<?> toIndexedParameters(Information information) {
    List<Object> list = new ArrayList<>(3);
    list.add(information.getFilename());
    list.add(information.getLine_nb());
    list.add(information.getHash());
    return list;
  }
}
