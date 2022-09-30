package bio.terra.cbas.dao;

import java.sql.Types;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class EnumAwareBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {
  public EnumAwareBeanPropertySqlParameterSource(Object object) {
    super(object);
  }

  @Override
  public int getSqlType(@NotNull String var) {
    int sqlType = super.getSqlType(var);
    if (sqlType == TYPE_UNKNOWN && hasValue(var)) {
      if (Objects.requireNonNull(getValue(var)).getClass().isEnum()) {
        sqlType = Types.VARCHAR;
      }
    }
    return sqlType;
  }
}
