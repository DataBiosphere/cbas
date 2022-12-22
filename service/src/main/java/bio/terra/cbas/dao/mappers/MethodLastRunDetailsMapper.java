package bio.terra.cbas.dao.mappers;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.model.MethodLastRunDetails;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public class MethodLastRunDetailsMapper implements RowMapper<MethodLastRunDetails> {

  @Override
  public MethodLastRunDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new MethodLastRunDetails()
        .previouslyRun(true)
        .timestamp(
            DateUtils.convertToDate(
                rs.getObject(RunSet.SUBMISSION_TIMESTAMP_COL, OffsetDateTime.class)))
        .runSetId(rs.getObject(RunSet.RUN_SET_ID_COL, UUID.class))
        .methodVersionId(rs.getObject(MethodVersion.METHOD_VERSION_ID_COL, UUID.class))
        .methodVersionName(rs.getString(MethodVersion.NAME_COL));
  }
}
