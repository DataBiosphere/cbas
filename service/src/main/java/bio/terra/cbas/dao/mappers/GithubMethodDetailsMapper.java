package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.GithubMethodDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class GithubMethodDetailsMapper implements RowMapper<Optional<GithubMethodDetails>> {

  @Override
  public Optional<GithubMethodDetails> mapRow(ResultSet rs, int rowNum) throws SQLException {

    String repository = rs.getString(GithubMethodDetails.REPOSITORY_COL);

    if (repository == null) {
      return Optional.empty();
    } else {
      return Optional.of(
          new GithubMethodDetails(
              repository,
              rs.getString(GithubMethodDetails.ORGANIZATION_COL),
              rs.getString(GithubMethodDetails.PATH_COL),
              rs.getBoolean(GithubMethodDetails.PRIVATE_COL),
              rs.getObject(GithubMethodDetails.METHOD_ID_COL, UUID.class)));
    }
  }
}
