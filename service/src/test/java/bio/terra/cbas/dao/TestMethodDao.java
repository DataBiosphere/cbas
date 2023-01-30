package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMethodDao {

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
    assertEquals(testMethod, actual);
  }

  @Test
  void retrievesAllMethods() {

    List<Method> allMethods = new ArrayList<>();

    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000005"),
            "assemble_refbased",
            "assemble_refbased",
            OffsetDateTime.parse("2023-01-27T19:21:24.542692Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000006"),
            "sarscov2_nextstrain",
            "sarscov2_nextstrain",
            OffsetDateTime.parse("2023-01-27T19:21:24.552878Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000008"),
            "fetch_sra_to_bam",
            "fetch_sra_to_bam",
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            null,
            "Github"));

    List<Method> actual = methodDao.getMethods();

    assertEquals(allMethods, actual);
    assertEquals(3, actual.size());
  }
}
