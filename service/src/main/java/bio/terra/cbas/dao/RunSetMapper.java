package bio.terra.cbas.dao;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
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
            rs.getString(Method.METHOD_URL_COL),
            rs.getString(Method.INPUT_DEFINITION_COL),
            rs.getString(Method.OUTPUT_DEFINITION_COL),
            rs.getString(Method.RECORD_TYPE_COL));

    return new RunSet(
        rs.getObject(RunSet.ID_COL, UUID.class),
        method,
        CbasRunSetStatus.fromValue(rs.getString(RunSet.STATUS_COL)),
        rs.getObject(RunSet.SUBMISSION_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_MODIFIED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_POLLED_TIMESTAMP_COL, OffsetDateTime.class),
        rs.getInt(RunSet.RUN_COUNT_COL),
        rs.getInt(RunSet.ERROR_COUNT_COL));
  }
}
