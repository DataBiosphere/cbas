package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.GithubMethodDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class GithubMethodSourceDetailsMapper implements RowMapper<GithubMethodDetails> {

  @Override
  public GithubMethodDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new GithubMethodDetails(
        rs.getString(GithubMethodDetails.REPOSITORY_COL),
        rs.getString(GithubMethodDetails.ORGANIZATION_COL),
        rs.getString(GithubMethodDetails.PATH_COL),
        rs.getBoolean(GithubMethodDetails.PRIVATE_COL),
        rs.getObject(GithubMethodDetails.METHOD_ID_COL, UUID.class));
  }
}
