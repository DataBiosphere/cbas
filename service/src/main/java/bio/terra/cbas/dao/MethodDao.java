package bio.terra.cbas.dao;

import bio.terra.cbas.dao.mappers.MethodMapper;
import bio.terra.cbas.models.Method;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MethodDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Method getMethod(UUID methodId) {
    String sql = "SELECT * FROM method WHERE method_id = :method_id";
    return jdbcTemplate
        .query(sql, new MapSqlParameterSource("method_id", methodId), new MethodMapper())
        .get(0);
  }

  public List<Method> getMethods() {
    String sql = "SELECT * FROM method";
    return jdbcTemplate.query(sql, new MethodMapper());
  }
}
