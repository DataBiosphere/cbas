package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.GithubMethodVersionDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class GithubMethodVersionDetailsMapper
    implements RowMapper<Optional<GithubMethodVersionDetails>> {

  @Override
  public Optional<GithubMethodVersionDetails> mapRow(ResultSet rs, int rowNum) throws SQLException {

    String githash = rs.getString(GithubMethodVersionDetails.GITHASH_COL);

    if (githash == null) {
      return Optional.empty();
    } else {
      return Optional.of(
          new GithubMethodVersionDetails(
              githash, rs.getObject(GithubMethodVersionDetails.METHOD_VERSION_ID_COL, UUID.class)));
    }
  }
}
