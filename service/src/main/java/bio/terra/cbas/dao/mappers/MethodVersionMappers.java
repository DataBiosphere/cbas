package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public final class MethodVersionMappers {

  private MethodVersionMappers() {}

  public static class DeepMethodVersionMapper implements RowMapper<MethodVersion> {
    @Override
    public MethodVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
      Method method = new MethodMapper().mapRow(rs, rowNum);

      return new MethodVersion(
          rs.getObject(MethodVersion.METHOD_VERSION_ID_COL, UUID.class),
          method,
          rs.getString(MethodVersion.NAME_COL),
          rs.getString(MethodVersion.DESCRIPTION__COL),
          rs.getObject(MethodVersion.CREATED_COL, OffsetDateTime.class),
          rs.getObject(MethodVersion.LAST_RUN_SET_ID_COL, UUID.class),
          rs.getString(MethodVersion.URL_COL),
          rs.getObject(MethodVersion.ORIGINAL_WORKSPACE_ID_COL, UUID.class),
          rs.getString(MethodVersion.BRANCH_OR_TAG_NAME));
    }
  }

  public static class ShallowMethodVersionMapper implements RowMapper<MethodVersion> {
    private final Method method;

    public ShallowMethodVersionMapper(Method method) {
      this.method = method;
    }

    @Override
    public MethodVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new MethodVersion(
          rs.getObject(MethodVersion.METHOD_VERSION_ID_COL, UUID.class),
          method,
          rs.getString(MethodVersion.NAME_COL),
          rs.getString(MethodVersion.DESCRIPTION__COL),
          rs.getObject(MethodVersion.CREATED_COL, OffsetDateTime.class),
          rs.getObject(MethodVersion.LAST_RUN_SET_ID_COL, UUID.class),
          rs.getString(MethodVersion.URL_COL),
          rs.getObject(MethodVersion.ORIGINAL_WORKSPACE_ID_COL, UUID.class),
          rs.getString(MethodVersion.BRANCH_OR_TAG_NAME));
    }
  }
}
