package bio.terra.cbas.dao;

import bio.terra.cbas.dao.mappers.GithubMethodDetailsMapper;
import bio.terra.cbas.models.GithubMethodDetails;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GithubMethodDetailsDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public GithubMethodDetailsDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createGithubMethodSourceDetails(GithubMethodDetails details) {
    return jdbcTemplate.update(
        "insert into github_method_details (repository, organization, path, private, method_id) "
            + "values (:repository, :organization, :path, :isPrivate, :methodId)",
        new BeanPropertySqlParameterSource(details));
  }

  public GithubMethodDetails getMethodSourceDetails(UUID methodId) {
    String sql =
        "SELECT * FROM github_method_details WHERE github_method_details.method_id = :methodId";

    MapSqlParameterSource params = new MapSqlParameterSource(Map.of("methodId", methodId));

    // Until backfilling has been executed, we could potentially look for details of an un-migrated
    // method.
    try {
      return jdbcTemplate.queryForObject(sql, params, new GithubMethodDetailsMapper());
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public int deleteMethodSourceDetails(UUID methodId) {
    return jdbcTemplate.update(
        "DELETE FROM github_method_details WHERE method_id = :methodId",
        new MapSqlParameterSource("methodId", methodId));
  }
}
