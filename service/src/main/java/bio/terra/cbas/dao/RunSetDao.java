package bio.terra.cbas.dao;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.mappers.RunSetMapper;
import bio.terra.cbas.dao.util.SqlPlaceholderMapping;
import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

  public List<RunSet> getRunSets() {
    String sql =
        "SELECT * FROM run_set "
            + "INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
            + "INNER JOIN method on method_version.method_id = method.method_id";
    return jdbcTemplate.query(sql, new RunSetMapper());
  }

  public RunSet getRunSet(UUID runSetId) {
    String sql =
        "SELECT * FROM run_set "
            + "INNER JOIN method_version ON run_set.method_version_id = method_version.method_version_id "
            + "INNER JOIN method on method_version.method_id = method.method_id "
            + "WHERE run_set_id = :runSetId";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("runSetId", runSetId), new RunSetMapper())
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
    return jdbcTemplate.update(
        sql, new MapSqlParameterSource(placeholderMapping.getPlaceholderToValueMap()));
  }

  public int updateStateAndRunDetails(
      UUID runSetId, CbasRunSetStatus newStatus, Integer runCount, Integer errorCount) {
    OffsetDateTime currentTimestamp = DateUtils.currentTimeInUTC();
    String sql =
        "UPDATE run_set SET status = :status, last_modified_timestamp = :last_modified_timestamp, last_polled_timestamp = :last_polled_timestamp, run_count = :run_count, error_count = :error_count WHERE run_set_id = :run_set_id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                RunSet.RUN_SET_ID_COL,
                runSetId,
                RunSet.STATUS_COL,
                newStatus.toString(),
                RunSet.LAST_MODIFIED_TIMESTAMP_COL,
                currentTimestamp,
                RunSet.LAST_POLLED_TIMESTAMP_COL,
                currentTimestamp,
                RunSet.RUN_COUNT_COL,
                runCount,
                RunSet.ERROR_COUNT_COL,
                errorCount)));
  }
}
