package bio.terra.cbas.dao;

import bio.terra.cbas.models.MethodVersion;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TestMethodVersionDao {

  MethodVersionDao methodVersionDao;

  @Test
  void retrievesSingleMethodVersion() {
    UUID methodVersionId = UUID.randomUUID();

    MethodVersion actual = methodVersionDao.getMethodVersion(methodVersionId);

    // Same as db?
    // MethodVersion expected = new MethodVersion(...);

    // assertEquals(expected, actual);
  }

  @Test
  void retrievesAllMethodVersions() {
    //    Method method = new MethodDetails()
    //        .method_id(UUID.randomUUID())
    //        .name("methodVersionName")
    //
    //
    //        String name,
    //        String description,
    //        OffsetDateTime created,
    //        UUID lastRunSetId,
    //        String methodSource

    // List<MethodVersion> actual = methodVersionDao.getMethodVersionsForMethod(method);

    // Same as db?
    // MethodVersion expected = new MethodVersion(...);

    // assertEquals(expected, actual);
  }
}
