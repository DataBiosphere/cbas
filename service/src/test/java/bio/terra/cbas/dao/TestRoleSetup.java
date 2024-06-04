package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.dependencies.bard.BardService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TestRoleSetup extends ContainerizedDatabaseTest {

  @MockBean BardService bardService;
  @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

  static class PgTableMapper implements RowMapper<PgTable> {

    @Override
    public PgTable mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new PgTable(rs.getString("tablename"), rs.getString("tableowner"));
    }
  }

  record PgTable(String tablename, String tableowner) {}

  @Test
  void validateRoleReassignment() {

    String sql =
        "SELECT * FROM pg_tables where tablename not like 'pg_%' and tablename not like 'sql_%' and tableowner = '"
            + TEST_USER
            + "'";

    List<PgTable> pgTables = jdbcTemplate.query(sql, Map.of(), new PgTableMapper());

    for (PgTable pgTable : pgTables) {
      assertEquals(
          TEST_ROLE,
          pgTable.tableowner(),
          "Table "
              + pgTable.tablename()
              + " is owned by '"
              + pgTable.tableowner()
              + "' but should be owned by '"
              + TEST_ROLE
              + "'. Do you need to update the 20231102_set_table_role.yaml changeset?");
    }
  }
}
