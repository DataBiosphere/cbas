package bio.terra.cbas.dao;

import bio.terra.cbas.models.RunSet;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunSetDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public RunSetDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createRunSet(RunSet runSet) {
    return jdbcTemplate.update(
        "insert into run_set (id, method_id, status, submission_timestamp, run_count, error_count) values (:id, :methodId, :status, :submissionTimestamp, :runCount, :errorCount)",
        new BeanPropertySqlParameterSource(runSet));
  }
}
