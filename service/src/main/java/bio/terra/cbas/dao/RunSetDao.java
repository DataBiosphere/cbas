package bio.terra.cbas.dao;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.mappers.RunSetMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunSetDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public RunSetDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<RunSet> getRunSets(Integer pageSize) {
    String sql =
        "SELECT * FROM run_set "
            + "INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
            + "INNER JOIN method on method_version.method_id = method.method_id GROUP BY run_set.run_set_id, method_version.method_version_id, method.method_id ORDER BY MIN(run_set.submission_timestamp) DESC LIMIT :pageSize";
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource("pageSize", pageSize), new RunSetMapper());
  }

  public RunSet getRunSet(UUID runSetId) {
    String sql =
        "SELECT * FROM run_set "
            + "INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE run_set.run_set_id = :runSetId GROUP BY run_set.run_set_id, method_version.method_version_id, method.method_id "
            + "ORDER BY MIN(run_set.submission_timestamp) DESC";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("runSetId", runSetId), new RunSetMapper())
        .get(0);
  }

  public RunSet getRunSetWithMethodId(UUID methodId, Integer pageSize) {
    String sql =
        "SELECT * FROM run_set "
            + "INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE method.method_id = :methodId GROUP BY run_set.run_set_id, method_version.method_version_id, method.method_id "
            + "ORDER BY MIN(run_set.submission_timestamp) DESC LIMIT :pageSize";
    return jdbcTemplate
        .query(
            sql,
            new MapSqlParameterSource(Map.of("methodId", methodId, "pageSize", pageSize)),
            new RunSetMapper())
        .get(0);
  }

  public int createRunSet(RunSet runSet) {
    return jdbcTemplate.update(
        "insert into run_set (run_set_id, method_version_id, run_set_name, run_set_description, is_template, status, submission_timestamp, last_modified_timestamp, last_polled_timestamp, run_count, error_count, input_definition, output_definition, record_type)"
            + " values (:runSetId, :methodVersionId, :name, :description, false, :status, :submissionTimestamp, :lastModifiedTimestamp, :lastPolledTimestamp, :runCount, :errorCount, :inputDefinition, :outputDefinition, :recordType)",
        new EnumAwareBeanPropertySqlParameterSource(runSet));
  }

  public int updateLastPolled(List<UUID> runSetIds) {

    SqlPlaceholderMapping<UUID> placeholderMapping =
        new SqlPlaceholderMapping<>("runSet", runSetIds);

    String sql =
        "UPDATE run_set SET last_modified_timestamp = :last_modified_timestamp WHERE run_set.run_set_id in (%s)"
            .formatted(placeholderMapping.getSqlPlaceholderList());
    Map<String, Object> params = new HashMap<>();
    params.put("last_modified_timestamp", OffsetDateTime.now());
    params.putAll(placeholderMapping.getPlaceholderToValueMap());
    return jdbcTemplate.update(sql, new MapSqlParameterSource(params));
  }

  public int updateStateAndRunDetails(
      UUID runSetId,
      CbasRunSetStatus newStatus,
      Integer runCount,
      Integer errorCount,
      OffsetDateTime lastModified) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();

    String updateClause =
        "UPDATE run_set SET %s = :status, %s = :last_polled_timestamp, %s = :run_count, %s = :error_count"
            .formatted(
                RunSet.STATUS_COL,
                RunSet.LAST_POLLED_TIMESTAMP_COL,
                RunSet.RUN_COUNT_COL,
                RunSet.ERROR_COUNT_COL);

    if (lastModified != null) {
      updateClause =
          updateClause + ", %s = :last_modified".formatted(RunSet.LAST_MODIFIED_TIMESTAMP_COL);
    }

    HashMap<String, Object> parameterMap =
        new HashMap<>(
            Map.of(
                "run_set_id",
                runSetId,
                "status",
                newStatus.toString(),
                "last_polled_timestamp",
                currentTimestamp,
                "run_count",
                runCount,
                "error_count",
                errorCount));

    Optional.ofNullable(lastModified)
        .ifPresent(lm -> parameterMap.put("last_modified", lm));

    String sql = updateClause + " WHERE %s = :run_set_id".formatted(RunSet.RUN_SET_ID_COL);
    return jdbcTemplate.update(sql, new MapSqlParameterSource(parameterMap));
  }
}
