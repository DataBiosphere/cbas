package bio.terra.cbas.dao;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.models.CbasRunStatus;
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
        "insert into run (run_id, engine_id, run_set_id, record_id, submission_timestamp, status, last_modified_timestamp, last_polled_timestamp, error_messages)"
            + " values (:run_id, :engineId, :runSetId, :recordId, :submissionTimestamp, :status, :lastModifiedTimestamp, :lastPolledTimestamp, :errorMessages)",
        new EnumAwareBeanPropertySqlParameterSource(run));
  }

  public List<Run> getRuns(String runSetId) {
    String whereClause = runSetId == null ? "" : " WHERE run.run_set_id = :runSetId";

    MapSqlParameterSource source =
        runSetId == null
            ? new MapSqlParameterSource()
            : new MapSqlParameterSource("runSetId", UUID.fromString(runSetId));

    String sql =
        "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.run_set_id"
            + " INNER JOIN method ON run_set.method_id = method.method_id"
            + whereClause;
    return jdbcTemplate.query(sql, source, new RunMapper());
  }

  public int updateRunStatus(UUID runId, CbasRunStatus newStatus) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();
    String sql =
        "UPDATE run SET status = :status, last_modified_timestamp = :last_modified_timestamp, last_polled_timestamp = :last_polled_timestamp WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runId,
                Run.STATUS_COL,
                newStatus.toString(),
                Run.LAST_MODIFIED_TIMESTAMP_COL,
                currentTimestamp,
                Run.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp)));
  }

  public int updateLastPolledTimestamp(UUID runID) {
    String sql = "UPDATE run SET last_polled_timestamp = :last_polled_timestamp WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runID,
                Run.LAST_POLLED_TIMESTAMP_COL,
                DateUtils.currentTimeInUTC())));
  }

  public int updateErrorMessage(UUID runId, String updatedErrorMessage) {
    String sql = "UPDATE run SET error_messages = :error_messages WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(Run.RUN_ID_COL, runId, Run.ERROR_MESSAGES_COL, updatedErrorMessage)));
  }

  private static class RunMapper implements RowMapper<Run> {
    public Run mapRow(ResultSet rs, int rowNum) throws SQLException {
      RunSet runSet = new RunSetMapper().mapRow(rs, rowNum);

      return new Run(
          rs.getObject(Run.RUN_ID_COL, UUID.class),
          rs.getString(Run.ENGINE_ID_COL),
          runSet,
          rs.getString(Run.RECORD_ID_COL),
          rs.getObject(Run.SUBMISSION_TIMESTAMP_COL, OffsetDateTime.class),
          CbasRunStatus.fromValue(rs.getString(Run.STATUS_COL)),
          rs.getObject(Run.LAST_MODIFIED_TIMESTAMP_COL, OffsetDateTime.class),
          rs.getObject(Run.LAST_POLLED_TIMESTAMP_COL, OffsetDateTime.class),
          rs.getString(Run.ERROR_MESSAGES_COL));
    }
  }
}
