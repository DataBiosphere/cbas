package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MethodVersionDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodVersionDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public MethodVersion getMethodVersion(UUID methodVersionId) {
    String sql = "SELECT * FROM method_version " +
        "INNER JOIN method on method_version.method_id = method.method_id " +
        "WHERE method_version_id = :method_version_id";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("method_version_id", methodVersionId), new MethodVersionMapper())
        .get(0);
  }
}
