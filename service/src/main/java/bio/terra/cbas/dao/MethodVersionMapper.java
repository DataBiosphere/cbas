package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MethodVersionMapper implements RowMapper<MethodVersion> {

  @Override
  public MethodVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
    Method method = new MethodMapper().mapRow(rs, rowNum);

    return new MethodVersion(
        rs.getObject(MethodVersion.METHOD_VERSION_ID_COL, UUID.class),
        method,
        rs.getString(MethodVersion.NAME_COL),
        rs.getString(MethodVersion.DESCRIPTION__COL),
        rs.getObject(MethodVersion.CREATED_COL, OffsetDateTime.class),
        rs.getObject(MethodVersion.LAST_RUN_COL, OffsetDateTime.class),
        rs.getString(MethodVersion.URL_COL));
  }
}
