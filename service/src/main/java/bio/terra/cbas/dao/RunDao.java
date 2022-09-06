package bio.terra.cbas.dao;

import bio.terra.cbas.models.Run;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public RunDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createRun(Run run) {
    return jdbcTemplate.update(
        "insert into run (id, engine_id, run_set_id, entity_id, submission_timestamp, status)"
            + " values (:id, :engineId, :runSetId, :entityId, :submissionTimestamp, :status)",
        new BeanPropertySqlParameterSource(run));
  }
}
