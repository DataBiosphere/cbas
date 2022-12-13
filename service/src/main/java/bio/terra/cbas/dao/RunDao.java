package bio.terra.cbas.dao;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.dao.util.WhereClause;
import bio.terra.cbas.models.CbasRunStatus;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import bio.terra.cbas.util.Pair;
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

    String sql =
        "SELECT * FROM run INNER JOIN run_set ON run.run_set_id = run_set.run_set_id"
            + " INNER JOIN method ON run_set.method_id = method.method_id "
            + whereClause;
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource(whereClause.params()), new RunMapper());
  }

  public Map<CbasRunStatus, Integer> getRunStatusCounts(RunsFilters filters) {
    WhereClause whereClause = filters.buildWhereClause();
    String sql =
        "SELECT status, count(1) as status_count FROM run " + whereClause + " GROUP BY run.status";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource(whereClause.params()), new StatusCountMapper())
        .stream()
        .collect(Collectors.toMap(Pair::a, Pair::b));
  }

  public int updateRunStatus(UUID runId, CbasRunStatus newStatus) {
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
                currentTimestamp,
                Run.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp)));
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

  public int updateErrorMessage(UUID runId, String updatedErrorMessage) {
    String sql = "UPDATE run SET error_messages = :error_messages WHERE run_id = :run_id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(Run.RUN_ID_COL, runId, Run.ERROR_MESSAGES_COL, updatedErrorMessage)));
  }

  public record RunsFilters(UUID runSetId, Collection<CbasRunStatus> statuses) {
    public static RunsFilters empty() {
      return new RunsFilters(null, null);
    }

    public WhereClause buildWhereClause() {
      if (runSetId == null && (statuses == null || statuses.isEmpty())) {
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

  private static class StatusCountMapper implements RowMapper<Pair<CbasRunStatus, Integer>> {
    public Pair<CbasRunStatus, Integer> mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new Pair<>(
          CbasRunStatus.fromValue(rs.getString(Run.STATUS_COL)), rs.getInt("status_count"));
    }
  }
}
