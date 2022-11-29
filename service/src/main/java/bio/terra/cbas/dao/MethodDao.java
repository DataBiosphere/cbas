package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.RunSet;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class MethodDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Method getMethod(UUID methodId) {
    String sql = "SELECT * FROM method WHERE id = :method_id";
    return jdbcTemplate.query(sql,
        new MapSqlParameterSource("method_id", methodId.toString()),
        new MethodMapper()).get(0);
  }

  public List<Method> getMethods() {
    String sql = "SELECT * FROM method";
    return jdbcTemplate.query(sql, new MethodMapper());
  }

  public int createMethod(Method method) {
    return jdbcTemplate.update(
        "insert into method (id, method_url, input_definition, output_definition, record_type)"
            + " values (:id, :methodUrl, :inputDefinition, :outputDefinition, :recordType)",
        new BeanPropertySqlParameterSource(method));
  }
}
