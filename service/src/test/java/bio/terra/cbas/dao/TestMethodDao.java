package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMethodDao {

  MethodDao methodDao;

  @Test
  void retrievesSingleMethod() {
    UUID methodId = UUID.randomUUID();
    Method actual = methodDao.getMethod(methodId);

    // Same values in db?
    Method expected = new Method('');

    assertEquals(expected, actual);
  }

  @Test
  void retriesAllMethods() {
    Method actual = methodDao.getMethods();
    Method expected = new Method(...);

    assertEquals(expected, actual);
  }
}
