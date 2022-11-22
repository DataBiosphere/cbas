package bio.terra.cbas.dao;

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
    String sql = "SELECT * FROM run_set INNER JOIN method ON run_set.method_id = method.id";
    return jdbcTemplate.query(sql, new RunSetMapper());
  }

  public int createRunSet(RunSet runSet) {
    return jdbcTemplate.update(
        "insert into run_set (id, method_id, status, submission_timestamp, last_modified_timestamp, last_polled_timestamp, run_count, error_count)"
            + " values (:id, :methodId, :status, :submissionTimestamp, :lastModifiedTimestamp, :lastPolledTimestamp, :runCount, :errorCount)",
        new EnumAwareBeanPropertySqlParameterSource(runSet));
  }

  public int updateStateAndRunDetails(
      UUID runSetId, CbasRunSetStatus newStatus, Integer runCount, Integer errorCount) {
    OffsetDateTime currentTimestamp = OffsetDateTime.now();
    String sql =
        "UPDATE run_set SET status = :status, last_modified_timestamp =:lastModifiedTimestamp, last_polled_timestamp = :lastPolledTimestamp, run_count = :runCount, error_count = :errorCount  WHERE id = :id";
    return jdbcTemplate.update(
        sql,
        new MapSqlParameterSource(
            Map.of(
                RunSet.ID,
                runSetId,
                RunSet.STATUS,
                newStatus.toString(),
                RunSet.LAST_MODIFIED_TIMESTAMP,
                currentTimestamp,
                RunSet.LAST_POLLED_TIMESTAMP,
                currentTimestamp,
                RunSet.RUN_COUNT,
                runCount,
                RunSet.ERROR_COUNT,
                errorCount)));
  }
}
