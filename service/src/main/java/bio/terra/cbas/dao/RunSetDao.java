package bio.terra.cbas.dao;

import static bio.terra.cbas.dao.MethodVersionDao.METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS;
import static bio.terra.cbas.dao.MethodVersionDao.METHOD_VERSION_JOIN_METHOD;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.mappers.RunSetMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.MethodVersion;
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

  public static final String RUN_SET_JOIN_METHOD_VERSION =
      "INNER JOIN method_version ON run_set.%S = method_version.%S ".formatted(
          RunSet.METHOD_VERSION_ID_COL, MethodVersion.METHOD_VERSION_ID_COL);

  public List<RunSet> getRunSets(Integer pageSize, boolean isTemplate) {
    String sql =
        "SELECT * FROM run_set "
            + RUN_SET_JOIN_METHOD_VERSION
            + METHOD_VERSION_JOIN_METHOD
            + METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS
            + "WHERE run_set.is_template = :isTemplate "
            + "ORDER BY run_set.submission_timestamp DESC";

    HashMap<String, Object> parameterMap = new HashMap<>(Map.of("isTemplate", isTemplate));

    if (pageSize != null) {
      sql = sql + " LIMIT :pageSize";
      parameterMap.put("pageSize", pageSize);
    }

    return jdbcTemplate.query(sql, new MapSqlParameterSource(parameterMap), new RunSetMapper());
  }

  public RunSet getRunSet(UUID runSetId) {
    String sql =
        "SELECT * FROM run_set "
            + RUN_SET_JOIN_METHOD_VERSION
            + METHOD_VERSION_JOIN_METHOD
            + METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS
            + "WHERE run_set.run_set_id = :runSetId ";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("runSetId", runSetId), new RunSetMapper())
        .get(0);
  }

  public RunSet getLatestRunSetWithMethodId(UUID methodId) {
    return getRunSetsWithMethodId(methodId).get(0);
  }

  public List<RunSet> getRunSetsWithMethodId(UUID methodId) {
    // Returns run sets in order from most to least recent
    String sql =
        "SELECT * FROM run_set "
            + RUN_SET_JOIN_METHOD_VERSION
            + METHOD_VERSION_JOIN_METHOD
            + METHOD_VERSION_JOIN_GITHUB_METHOD_VERSION_DETAILS
            + "WHERE method.method_id = :methodId "
            + "ORDER BY run_set.submission_timestamp DESC";
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource(Map.of("methodId", methodId)), new RunSetMapper());
  }

  public int createRunSet(RunSet runSet) {
    return jdbcTemplate.update(
        "insert into run_set (run_set_id, method_version_id, run_set_name, run_set_description, call_caching_enabled, is_template, status, submission_timestamp, last_modified_timestamp, last_polled_timestamp, run_count, error_count, input_definition, output_definition, record_type, user_id, run_set_original_workspace_id)"
            + " values (:runSetId, :methodVersionId, :name, :description, :callCachingEnabled, :isTemplate, :status, :submissionTimestamp, :lastModifiedTimestamp, :lastPolledTimestamp, :runCount, :errorCount, :inputDefinition, :outputDefinition, :recordType, :userId, :originalWorkspaceId)",
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

    Optional.ofNullable(lastModified).ifPresent(lm -> parameterMap.put("last_modified", lm));

    String sql = updateClause + " WHERE %s = :run_set_id".formatted(RunSet.RUN_SET_ID_COL);
    return jdbcTemplate.update(sql, new MapSqlParameterSource(parameterMap));
  }

  public int updateOriginalWorkspaceId(UUID runSetId, UUID originalWorkspaceId) {
    String updateClause =
        "UPDATE run_set SET %s = :run_set_original_workspace_id"
            .formatted(RunSet.ORIGINAL_WORKSPACE_ID_COL);

    HashMap<String, Object> parameterMap =
        new HashMap<>(
            Map.of("run_set_id", runSetId, "run_set_original_workspace_id", originalWorkspaceId));

    String sql = updateClause + " WHERE %s = :run_set_id".formatted(RunSet.RUN_SET_ID_COL);

    return jdbcTemplate.update(sql, new MapSqlParameterSource(parameterMap));
  }

  public int deleteRunSet(UUID runSetId) {
    return jdbcTemplate.update(
        "DELETE FROM run_set WHERE run_set_id = :run_set_id",
        new MapSqlParameterSource(RunSet.RUN_SET_ID_COL, runSetId));
  }

  public int updateIsTemplate(UUID runSetId, Boolean isTemplate) {
    String updateClause = "UPDATE run_set SET %s = :is_template".formatted(RunSet.IS_TEMPLATE_COL);
    HashMap<String, Object> parameterMap =
        new HashMap<>(
            Map.of(
                "run_set_id", runSetId,
                "is_template", isTemplate));
    String sql = updateClause + " WHERE %s = :run_set_id".formatted(RunSet.RUN_SET_ID_COL);
    return jdbcTemplate.update(sql, new MapSqlParameterSource(parameterMap));
  }
}
