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
            rs.getObject("method_id", UUID.class),
            rs.getString("method_url"),
            rs.getString("input_definition"),
            rs.getString("output_definition"),
            rs.getString("record_type"));

    return new RunSet(
        rs.getObject("id", UUID.class),
        method,
        CbasRunSetStatus.fromValue(rs.getString("status")),
        rs.getObject("submission_timestamp", OffsetDateTime.class),
        rs.getObject("last_modified_timestamp", OffsetDateTime.class),
        rs.getObject("last_polled_timestamp", OffsetDateTime.class),
        rs.getInt("run_count"),
        rs.getInt("error_count"));
  }
}
