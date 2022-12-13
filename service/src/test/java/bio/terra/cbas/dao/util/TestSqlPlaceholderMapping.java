package bio.terra.cbas.dao.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestSqlPlaceholderMapping {

  @Test
  void noElements() {
    List<String> foos = List.of();
    SqlPlaceholderMapping<String> mapping = new SqlPlaceholderMapping<>("foo", foos);

    assertEquals(Map.of(), mapping.getPlaceholderToValueMap());
    assertEquals("", mapping.getSqlPlaceholderList());
  }

  @Test
  void preserveOrderOfElements() {
    List<String> foos = List.of("a", "b", "c", "d", "e");
    SqlPlaceholderMapping<String> mapping = new SqlPlaceholderMapping<>("foo", foos);

    assertEquals(
        Map.of(
            "foo_0", "a",
            "foo_1", "b",
            "foo_2", "c",
            "foo_3", "d",
            "foo_4", "e"),
        mapping.getPlaceholderToValueMap());
    assertEquals(":foo_0,:foo_1,:foo_2,:foo_3,:foo_4", mapping.getSqlPlaceholderList());
  }
}
