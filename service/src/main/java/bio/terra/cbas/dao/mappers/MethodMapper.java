package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.GithubMethodVersionDetails;
import bio.terra.cbas.models.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class MethodMapper implements RowMapper<Method> {

  @Override
  public Method mapRow(ResultSet rs, int rowNum) throws SQLException {
    Optional<GithubMethodDetails> githubMethodDetails =
        new GithubMethodDetailsMapper().mapRow(rs, rowNum);

    return new Method(
        rs.getObject(Method.METHOD_ID_COL, UUID.class),
        rs.getString(Method.NAME_COL),
        rs.getString(Method.DESCRIPTION__COL),
        rs.getObject(Method.CREATED_COL, OffsetDateTime.class),
        rs.getObject(Method.LAST_RUN_SET_ID_COL, UUID.class),
        rs.getString(Method.METHOD_SOURCE_COL),
        rs.getObject(Method.ORIGINAL_WORKSPACE_ID_COL, UUID.class),
        githubMethodDetails);
  }
}
