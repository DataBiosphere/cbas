package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.GithubMethodSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class GithubMethodSourceDetailsMapper implements RowMapper<GithubMethodSource> {

  @Override
  public GithubMethodSource mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new GithubMethodSource(
        rs.getString(GithubMethodSource.REPOSITORY_COL),
        rs.getString(GithubMethodSource.ORGANIZATION_COL),
        rs.getString(GithubMethodSource.PATH_COL),
        rs.getBoolean(GithubMethodSource.PRIVATE_COL),
        rs.getObject(GithubMethodSource.METHOD_ID_COL, UUID.class));
  }
}
