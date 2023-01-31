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
class TestMethodVersionDao {

  @Autowired MethodVersionDao methodVersionDao;

  UUID methodVersionId = UUID.fromString("50000000-0000-0000-0000-000000000005");

  UUID methodId = UUID.fromString("00000000-0000-0000-0000-000000000005");
  String time = "2023-01-27T19:21:24.542692Z";

  String name = "assemble_refbased";
  String description = "assemble_refbased";

  Method dbMethod =
      new Method(methodId, name, description, OffsetDateTime.parse(time), null, "Github");

  @Test
  void retrievesSingleMethodVersion() {

    MethodVersion methodVersion =
        new MethodVersion(
            methodVersionId,
            dbMethod,
            "1.0",
            "assemble_refbased sample submission",
            OffsetDateTime.parse("2023-01-27T19:21:24.542692Z"),
            null,
            "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/assemble_refbased.wdl");

    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    assertEquals(methodVersion.methodVersionId(), actual.methodVersionId());
    assertEquals(methodVersion.name(), actual.name());
    assertEquals(methodVersion.description(), actual.description());
    assertEquals(methodVersion.lastRunSetId(), actual.lastRunSetId());
    assertEquals(methodVersion.url(), actual.url());
  }

  @Test
  void retrievesMethodVersionsForMethod() {

    List<MethodVersion> methodVersions = new ArrayList<>();

    methodVersions.add(
        new MethodVersion(
            UUID.fromString("50000000-0000-0000-0000-000000000005"),
            dbMethod,
            "1.0",
            "assemble_refbased sample submission",
            OffsetDateTime.parse("2023-01-27T19:21:24.542692Z"),
            null,
            "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/assemble_refbased.wdl"));

    List<MethodVersion> actual = methodVersionDao.getMethodVersionsForMethod(dbMethod);

    assertEquals(methodVersions.get(0).methodVersionId(), actual.get(0).methodVersionId());
    assertEquals(methodVersions.get(0).method().method_id(), actual.get(0).method().method_id());
    assertEquals(methodVersions.get(0).name(), actual.get(0).name());
    assertEquals(methodVersions.get(0).description(), actual.get(0).description());
    assertEquals(methodVersions.get(0).lastRunSetId(), actual.get(0).lastRunSetId());
    assertEquals(methodVersions.get(0).url(), actual.get(0).url());

    assertEquals(1, actual.size());
  }
}
