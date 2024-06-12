package bio.terra.cbas.dao;

import static bio.terra.cbas.dao.MethodDao.METHOD_JOIN_GITHUB_METHOD_DETAILS;
import static bio.terra.cbas.dao.MethodVersionDao.METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS;
import static bio.terra.cbas.dao.MethodVersionDao.METHOD_VERSION_JOIN_METHOD;
import static bio.terra.cbas.models.Run.truncatedErrorMessage;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.mappers.RunSetMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.dao.util.WhereClause;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  // SQL query for reading Run records.

  private static final String RUN_SELECT_SQL =
      "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.run_set_id"
          + " INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
          + METHOD_VERSION_JOIN_METHOD
          + METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS;

  public RunDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createRun(Run run) {
    return jdbcTemplate.update(
        "insert into run (run_id, engine_id, run_set_id, record_id, submission_timestamp, status, last_modified_timestamp, last_polled_timestamp, error_messages)"
            + " values (:runId, :engineId, :runSetId, :recordId, :submissionTimestamp, :status, :lastModifiedTimestamp, :lastPolledTimestamp, :errorMessages)",
        new EnumAwareBeanPropertySqlParameterSource(run));
  }

  public List<Run> getRuns(RunsFilters filters) {
    WhereClause whereClause = filters.buildWhereClause();

    String sql = RUN_SELECT_SQL + METHOD_JOIN_GITHUB_METHOD_DETAILS + whereClause;
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource(whereClause.params()), new RunMapper());
  }

  public Map<CbasRunStatus, StatusCountRecord> getRunStatusCounts(RunsFilters filters) {
    WhereClause whereClause = filters.buildWhereClause();
    String sql =
        "SELECT %s, count(1) as status_count, max(%s) as last_modified FROM run "
                .formatted(Run.STATUS_COL, Run.LAST_MODIFIED_TIMESTAMP_COL)
            + whereClause
            + " GROUP BY run.status";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource(whereClause.params()), new StatusCountMapper())
        .stream()
        .collect(Collectors.toMap(StatusCountRecord::status, r -> r));
  }

  public int updateRunStatus(
      UUID runId, CbasRunStatus newStatus, OffsetDateTime lastModifiedTimestamp) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();
    String sql =
        "UPDATE run SET status = :status, last_modified_timestamp = :last_modified_timestamp, last_polled_timestamp = :last_polled_timestamp WHERE run_id = :run_id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runId,
                Run.STATUS_COL,
                newStatus.toString(),
                Run.LAST_MODIFIED_TIMESTAMP_COL,
                lastModifiedTimestamp,
                Run.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp)));
  }

  public int updateEngineIdAndRunStatus(
      UUID runId, UUID engineId, CbasRunStatus newStatus, OffsetDateTime lastModifiedTimestamp) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();
    String sql =
        "UPDATE run SET engine_id = :engine_id, status = :status, last_modified_timestamp = :last_modified_timestamp, last_polled_timestamp = :last_polled_timestamp WHERE run_id = :run_id";

    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runId,
                Run.ENGINE_ID_COL,
                engineId,
                Run.STATUS_COL,
                newStatus.toString(),
                Run.LAST_MODIFIED_TIMESTAMP_COL,
                lastModifiedTimestamp,
                Run.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp)));
  }

  public int updateRunStatusWithError(
      UUID runId,
      CbasRunStatus newStatus,
      OffsetDateTime lastModifiedTimestamp,
      String updatedErrorMessage) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();
    String sql =
        "UPDATE run SET status = :status, last_modified_timestamp = :last_modified_timestamp, last_polled_timestamp = :last_polled_timestamp, error_messages = :error_messages WHERE run_id = :run_id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runId,
                Run.STATUS_COL,
                newStatus.toString(),
                Run.LAST_MODIFIED_TIMESTAMP_COL,
                lastModifiedTimestamp,
                Run.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp,
                Run.ERROR_MESSAGES_COL,
                truncatedErrorMessage(updatedErrorMessage))));
  }

  public int updateLastPolledTimestamp(UUID runID) {
    String sql =
        "UPDATE run SET last_polled_timestamp = :last_polled_timestamp WHERE run_id = :run_id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                Run.RUN_ID_COL,
                runID,
                Run.LAST_POLLED_TIMESTAMP_COL,
                DateUtils.currentTimeInUTC())));
  }

  public record RunsFilters(UUID runSetId, Collection<CbasRunStatus> statuses, String engineId) {
    public RunsFilters(UUID runSetId, Collection<CbasRunStatus> statuses) {
      this(runSetId, statuses, null);
    }

    public static RunsFilters empty() {
      return new RunsFilters(null, null, null);
    }

    public WhereClause buildWhereClause() {
      if (runSetId == null
          && (statuses == null || statuses.isEmpty())
          && (engineId == null || engineId.isEmpty())) {
        return new WhereClause(List.of(), Map.of());
      } else {
        List<String> conditions = new LinkedList<>();
        Map<String, Object> params = new HashMap<>();
        if (runSetId != null) {
          conditions.add("run.run_set_id = :runSetId");
          params.put("runSetId", runSetId);
        }
        if (statuses != null && !statuses.isEmpty()) {
          SqlPlaceholderMapping<String> placeholderMapping =
              new SqlPlaceholderMapping<>(
                  "status", statuses.stream().map(CbasRunStatus::toString).toList());
          conditions.add(
              "run.status in (%s)".formatted(placeholderMapping.getSqlPlaceholderList()));
          params.putAll(placeholderMapping.getPlaceholderToValueMap());
        }
        if (engineId != null && !engineId.isEmpty()) {
          conditions.add("run.engine_id = :engineId");
          params.put("engineId", engineId);
        }
        return new WhereClause(conditions, params);
      }
    }
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

  public record StatusCountRecord(
      CbasRunStatus status, Integer count, OffsetDateTime lastModified) {}

  public int deleteRun(UUID runId) {
    return jdbcTemplate.update(
        "DELETE FROM run WHERE run_id = :run_id", new MapSqlParameterSource(Run.RUN_ID_COL, runId));
  }

  private static class StatusCountMapper implements RowMapper<StatusCountRecord> {
    public StatusCountRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new StatusCountRecord(
          CbasRunStatus.fromValue(rs.getString(Run.STATUS_COL)),
          rs.getInt("status_count"),
          rs.getObject("last_modified", OffsetDateTime.class));
    }
  }
}
