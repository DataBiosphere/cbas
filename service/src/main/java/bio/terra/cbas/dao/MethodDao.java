package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MethodDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public MethodDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int createMethod(Method method) {
    return jdbcTemplate.update(
        "insert into method (id, method_url, input_definition, output_definition, entity_type)"
            + " values (:id, :methodUrl, :inputDefinition, :outputDefinition, :entityType)",
        new BeanPropertySqlParameterSource(method));
  }
}
