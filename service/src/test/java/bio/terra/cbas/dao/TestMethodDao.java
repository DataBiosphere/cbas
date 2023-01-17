package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMethodDao {

  @Autowired MethodDao methodDao;
  UUID methodId = UUID.randomUUID();
  String time = "2023-01-13T20:19:41.400292Z";

  @BeforeEach
  void setUp() {
    String name = "test method";
    String description = "test method for the db";
    Method dbMethod =
        new Method(methodId, name, description, OffsetDateTime.parse(time), null, "Github");
    methodDao.createMethod(dbMethod);
  }

  @Test
  void retrievesSingleMethod() {
    Method testMethod =
        new Method(
            methodId,
            "test method",
            "test method for the db",
            OffsetDateTime.parse(time),
            null,
            "Github");

    Method expected = methodDao.getMethod(methodId);
    assertEquals(testMethod, expected);
  }

  @Test
  void retrievesAllMethods() {

    List<Method> actual = methodDao.getMethods();
  }
}
