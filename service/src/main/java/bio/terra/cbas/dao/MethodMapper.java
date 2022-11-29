package bio.terra.cbas.dao;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MethodMapper implements RowMapper<Method> {

  @Override
  public Method mapRow(ResultSet rs, int rowNum) throws SQLException {
   return new Method(
        rs.getObject(RunSet.METHOD_ID_COL, UUID.class),
        rs.getString(Method.NAME_COL),
        rs.getString(Method.DESCRIPTION__COL),
        rs.getObject(Method.CREATED_COL, OffsetDateTime.class),
        rs.getObject(Method.LAST_RUN_COL, OffsetDateTime.class),
        rs.getString(Method.METHOD_SOURCE_COL),
        rs.getString(Method.METHOD_SOURCE_URL_COL));
  }
}
