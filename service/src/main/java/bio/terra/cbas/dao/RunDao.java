package bio.terra.cbas.dao;

import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
        "insert into run (id, engine_id, run_set_id, record_id, submission_timestamp, status)"
            + " values (:id, :engineId, :runSetId, :recordId, :submissionTimestamp, :status)",
        new BeanPropertySqlParameterSource(run));
  }

  public List<Run> getRuns() {
    String sql =
        "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.id"
            + " INNER JOIN method ON run_set.method_id = method.id";
    return jdbcTemplate.query(sql, new RunMapper());
  }

  public int updateRunStatus(Run run, CbasRunStatus newStatus) {
    String sql = "UPDATE run SET status = :status WHERE id = :id";
    return jdbcTemplate.update(
        sql, new MapSqlParameterSource(Map.of("id", run.id(), "status", newStatus)));
  }

  private static class RunMapper implements RowMapper<Run> {
    public Run mapRow(ResultSet rs, int rowNum) throws SQLException {
      Method method =
          new Method(
              rs.getObject("method_id", UUID.class),
              rs.getString("method_url"),
              rs.getString("input_definition"),
              rs.getString("record_type"));

      RunSet runSet = new RunSet(rs.getObject("run_set_id", UUID.class), method);

      return new Run(
          rs.getObject("id", UUID.class),
          rs.getString("engine_id"),
          runSet,
          rs.getString("record_id"),
          rs.getObject("submission_timestamp", OffsetDateTime.class),
          CbasRunStatus.fromValue(rs.getString("status")));
    }
  }
}
