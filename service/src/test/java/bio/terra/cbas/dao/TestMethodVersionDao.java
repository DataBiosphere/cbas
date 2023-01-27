package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
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
public class TestMethodVersionDao {

  @Autowired MethodVersionDao methodVersionDao;

  UUID methodVersionId = UUID.fromString("20000000-0000-0000-0000-000000000002");

  UUID methodId = UUID.fromString("00000000-0000-0000-0000-000000000002");
  String time = "2023-01-10T16:46:23.946326Z";

  String name = "Target workflow 2";
  String description = "Add description";

  Method dbMethod =
      new Method(methodId, name, description, OffsetDateTime.parse(time), null, "Dockstore-Github");

  @Test
  void retrievesSingleMethodVersion() {

    MethodVersion methodVersion =
        new MethodVersion(
            methodVersionId,
            dbMethod,
            "1.0",
            "First version of target workflow 2",
            OffsetDateTime.parse("2023-01-10T16:46:23.955430Z"),
            null,
            "https://raw.githubusercontent.com/DataBiosphere/cbas/main/useful_workflows/target_workflow_2/target_workflow_2.wdl");

    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    assertEquals(methodVersion, actual);
  }

  @Test
  void retrievesMethodVersionsForMethod() {

    List<MethodVersion> methodVersions = new ArrayList<>();

    methodVersions.add(
        new MethodVersion(
            UUID.fromString("20000000-0000-0000-0000-000000000002"),
            dbMethod,
            "1.0",
            "First version of target workflow 2",
            OffsetDateTime.parse("2023-01-10T16:46:23.955430Z"),
            null,
            "https://raw.githubusercontent.com/DataBiosphere/cbas/main/useful_workflows/target_workflow_2/target_workflow_2.wdl"));

    List<MethodVersion> actual = methodVersionDao.getMethodVersionsForMethod(dbMethod);

    assertEquals(methodVersions, actual);
    assertEquals(1, actual.size());
  }
}