package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.Run;
import bio.terra.cbas.models.RunSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class RunSetMapper implements RowMapper<RunSet> {

  @Override
  public RunSet mapRow(ResultSet rs, int rowNum) throws SQLException {
    MethodVersion methodVersion =
        new MethodVersionMappers.DeepMethodVersionMapper().mapRow(rs, rowNum);

    return new RunSet(
        rs.getObject(Run.RUN_SET_ID_COL, UUID.class),
        methodVersion,
        rs.getString(RunSet.NAME_COL),
        rs.getString(RunSet.DESCRIPTION_COL),
        rs.getBoolean(RunSet.CALL_CACHING_ENABLED_COL),
        rs.getBoolean(RunSet.IS_TEMPLATE_COL),
        CbasRunSetStatus.fromValue(rs.getString(RunSet.STATUS_COL)),
        rs.getObject(RunSet.SUBMISSION_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_MODIFIED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_POLLED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getInt(RunSet.RUN_COUNT_COL),
        rs.getInt(RunSet.ERROR_COUNT_COL),
        rs.getString(RunSet.INPUT_DEFINITION_COL),
        rs.getString(RunSet.OUTPUT_DEFINITION_COL),
        rs.getString(RunSet.RECORD_TYPE_COL),
        rs.getString(RunSet.USER_ID_COL),
        rs.getObject(RunSet.ORIGINAL_WORKSPACE_ID_COL, UUID.class));
  }
}
