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
  UUID methodId = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void retrievesSingleMethod() {
    Method testMethod =
        new Method(
            methodId,
            "Target workflow 2",
            "Add description",
            OffsetDateTime.parse("2023-01-10T16:46:23.946326Z"),
            null,
            "Dockstore-Github");

    Method expected = methodDao.getMethod(methodId);
    assertEquals(testMethod, expected);
  }

  @Test
  void retrievesAllMethods() {

    List<Method> allMethods = new ArrayList<>();

    System.out.print(methodDao.getMethods());

    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "Target workflow 2",
            "Add description",
            OffsetDateTime.parse("2023-01-10T16:46:23.946326Z"),
            null,
            "Dockstore-Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "Target workflow 3",
            "Add description",
            OffsetDateTime.parse("2023-01-10T16:46:23.946326Z"),
            null,
            "Dockstore-Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000004"),
            "Target Workflow 4",
            "Target Workflow 4",
            OffsetDateTime.parse("2023-01-10T16:46:23.982587Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000008"),
            "fetch_sra_to_bam",
            "fetch_sra_to_bam / Target Workflow 4",
            OffsetDateTime.parse("2023-01-17T18:40:48.824995Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000006"),
            "sarscov2_nextstrain",
            "sarscov2_nextstrain / Target Workflow 3",
            OffsetDateTime.parse("2023-01-17T19:50:21.319178Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Target Workflow 1",
            "Target Workflow 1",
            OffsetDateTime.parse("2023-01-10T16:46:23.946326Z"),
            null,
            "Github"));
    allMethods.add(
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000005"),
            "assemble_refbased",
            "assemble_refbased / Target Workflow 2",
            OffsetDateTime.parse("2023-01-18T21:59:40.528573Z"),
            null,
            "Github"));

    List<Method> actual = methodDao.getMethods();

    assertEquals(allMethods, actual);
    assertEquals(7, actual.size());
  }
}
