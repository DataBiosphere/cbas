package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.model.GithubMethodSourceDetails;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class GithubMethodSourceDetailsMapper implements RowMapper<GithubMethodSourceDetails> {

  @Override
  public GithubMethodSourceDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new GithubMethodSourceDetails()
        .methodId(rs.getObject("method_id", UUID.class))
        ._private(rs.getObject("private", Boolean.class))
        .path(rs.getString("path"))
        .organization(rs.getString("organization"))
        .repository(rs.getString("repository"));
  }
}
