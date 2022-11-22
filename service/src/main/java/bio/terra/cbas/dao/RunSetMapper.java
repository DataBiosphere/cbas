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
            rs.getObject(RunSet.METHOD_ID, UUID.class),
            rs.getString(Method.METHOD_URL),
            rs.getString(Method.INPUT_DEFINITION),
            rs.getString(Method.OUTPUT_DEFINITION),
            rs.getString(Method.RECORD_TYPE));

    return new RunSet(
        rs.getObject(RunSet.ID, UUID.class),
        method,
        CbasRunSetStatus.fromValue(rs.getString(RunSet.STATUS)),
        rs.getObject(RunSet.SUBMISSION_TIMESTAMP, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_MODIFIED_TIMESTAMP, OffsetDateTime.class),
        rs.getObject(RunSet.LAST_POLLED_TIMESTAMP, OffsetDateTime.class),
        rs.getInt(RunSet.RUN_COUNT),
        rs.getInt(RunSet.ERROR_COUNT));
  }
}
