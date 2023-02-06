package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMethodDao {

  @Autowired MethodDao methodDao;
  UUID methodId = UUID.fromString("00000000-0000-0000-0000-000000000005");

  @Test
  void retrievesSingleMethod() {
    Method testMethod =
        new Method(
            methodId,
            "assemble_refbased",
            "assemble_refbased",
            OffsetDateTime.parse("2023-01-27T19:21:24.542692Z"),
            null,
            "Github");

    Method actual = methodDao.getMethod(methodId);

    /*
    Asserting each column value separately here and omitting the 'created' column due to github
    passing in a current_timestamp() value, causing the test to fail.
    */

    assertEquals(testMethod.methodId(), actual.methodId());
    assertEquals(testMethod.name(), actual.name());
    assertEquals(testMethod.description(), actual.description());
    assertEquals(testMethod.lastRunSetId(), actual.lastRunSetId());
    assertEquals(testMethod.methodSource(), actual.methodSource());
  }

  @Test
  void retrievesAllMethods() {

    List<Method> actual = methodDao.getMethods();

    assertEquals(3, actual.size());
  }
}
