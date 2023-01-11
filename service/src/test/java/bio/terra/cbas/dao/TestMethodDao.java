package bio.terra.cbas.dao;

import bio.terra.cbas.models.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMethodDao {

  MethodDao methodDao;

  @Test
  void retrievesSingleMethod() {
    UUID methodId = UUID.randomUUID();
    Method actual = methodDao.getMethod(methodId);

    // Same values in db?
    // Method expected = new Method('');

    // assertEquals(expected, actual);
  }

  @Test
  void retrievesAllMethods() {

    List<Method> actual = methodDao.getMethods();

    // Same as in db?
    // List<Method> expected = new Method(...);

    // assertEquals(expected, actual);
  }
}
