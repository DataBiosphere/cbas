package bio.terra.cbas.dao;

import bio.terra.cbas.exception.RunQueryNotFoundException;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
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

  public List<Run> retrieve() {
    String sql =
        "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.id"
            + " INNER JOIN method ON run_set.method_id = method.id";

    try {
      return jdbcTemplate.query(sql, new RunMapper());
    } catch (EmptyResultDataAccessException er) {
      throw new RunQueryNotFoundException("Runs not found.");
    }
  }

  private static class RunMapper implements RowMapper<Run> {
    public Run mapRow(ResultSet rs, int rowNum) throws SQLException {
      Method method =
          new Method(
              rs.getObject("method_id", UUID.class),
              rs.getString("method_url"),
              rs.getString("input_definition"),
              rs.getString("entity_type"));

      RunSet runSet = new RunSet(rs.getObject("run_set_id", UUID.class), method);

      return new Run(
          rs.getObject("id", UUID.class),
          rs.getString("engine_id"),
          runSet,
          rs.getString("entity_id"),
          rs.getObject("submission_timestamp", OffsetDateTime.class),
          rs.getString("status"));
    }
  }
}
