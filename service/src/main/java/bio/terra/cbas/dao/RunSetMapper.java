package bio.terra.cbas.dao;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
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
    Method method =
        new Method(
            rs.getObject(RunSet.METHOD_ID_COL, UUID.class),
            rs.getString(Method.NAME_COL),
            rs.getString(Method.DESCRIPTION__COL),
            rs.getObject(Method.CREATED_COL, OffsetDateTime.class),
            rs.getObject(Method.LAST_RUN_COL, OffsetDateTime.class),
            rs.getString(Method.METHOD_SOURCE_COL),
            rs.getString(Method.METHOD_SOURCE_URL_COL));

    return new RunSet(
        rs.getObject(Run.RUN_SET_ID_COL, UUID.class),
        method,
        CbasRunSetStatus.fromValue(rs.getString(RunSet.STATUS_COL)),
        rs.getObject(RunSet.SUBMISSION_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_MODIFIED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_POLLED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getInt(RunSet.RUN_COUNT_COL),
        rs.getInt(RunSet.ERROR_COUNT_COL),
        rs.getString(RunSet.INPUT_DEFINITION_COL),
        rs.getString(RunSet.OUTPUT_DEFINITION_COL),
        rs.getString(RunSet.RECORD_TYPE_COL));
  }
}
