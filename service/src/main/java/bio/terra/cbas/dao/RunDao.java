package bio.terra.cbas.dao;

import bio.terra.cbas.models.CbasRunSetStatus;
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
        "insert into run (id, engine_id, run_set_id, record_id, submission_timestamp, status, last_modified_timestamp, last_polled_timestamp, error_messages)"
            + " values (:id, :engineId, :runSetId, :recordId, :submissionTimestamp, :status, :lastModifiedTimestamp, :lastPolledTimestamp, :errorMessages)",
        new EnumAwareBeanPropertySqlParameterSource(run));
  }

  public List<Run> getRuns(String runSetId) {
    String whereClause = runSetId == null ? "" : " WHERE run.run_set_id = :runSetId";

    MapSqlParameterSource source =
        runSetId == null
            ? new MapSqlParameterSource()
            : new MapSqlParameterSource("runSetId", UUID.fromString(runSetId));

    String sql =
        "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.id"
            + " INNER JOIN method ON run_set.method_id = method.id"
            + whereClause;
    return jdbcTemplate.query(sql, source, new RunMapper());
  }

  public int updateRunStatus(UUID runId, CbasRunStatus newStatus) {
    OffsetDateTime currentTimestamp = OffsetDateTime.now();
    String sql =
        "UPDATE run SET status = :status, last_modified_timestamp =:lastModifiedTimestamp, last_polled_timestamp = :lastPolledTimestamp  WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                "id",
                runId,
                "status",
                newStatus.toString(),
                "lastModifiedTimestamp",
                currentTimestamp,
                "lastPolledTimestamp",
                currentTimestamp)));
  }

  public int updateLastPolledTimestamp(UUID runID) {
    String sql = "UPDATE run SET last_polled_timestamp = :lastPolledTimestamp  WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of("id", runID, "lastPolledTimestamp", OffsetDateTime.now())));
  }

  public int updateErrorMessage(UUID runId, String updatedErrorMessage) {
    String sql = "UPDATE run SET error_messages = :errorMessages WHERE id = :id";
    return jdbcTemplate.update(
        sql, new MapSqlParameterSource(Map.of("id", runId, "errorMessages", updatedErrorMessage)));
  }

  private static class RunMapper implements RowMapper<Run> {
    public Run mapRow(ResultSet rs, int rowNum) throws SQLException {
      Method method =
          new Method(
              rs.getObject("method_id", UUID.class),
              rs.getString("method_url"),
              rs.getString("input_definition"),
              rs.getString("output_definition"),
              rs.getString("record_type"));

      RunSet runSet =
          new RunSet(
              rs.getObject("run_set_id", UUID.class),
              method,
              CbasRunSetStatus.fromValue(rs.getString("status")),
              rs.getObject("submission_timestamp", OffsetDateTime.class),
              rs.getObject("last_modified_timestamp", OffsetDateTime.class),
              rs.getObject("last_polled_timestamp", OffsetDateTime.class),
              rs.getInt("run_count"),
              rs.getInt("error_count"));

      return new Run(
          rs.getObject("id", UUID.class),
          rs.getString("engine_id"),
          runSet,
          rs.getString("record_id"),
          rs.getObject("submission_timestamp", OffsetDateTime.class),
          CbasRunStatus.fromValue(rs.getString("status")),
          rs.getObject("last_modified_timestamp", OffsetDateTime.class),
          rs.getObject("last_polled_timestamp", OffsetDateTime.class),
          rs.getString("error_messages"));
    }
  }
}
