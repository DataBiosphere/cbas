package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.common.DateUtils;
import bio.terra.cbas.dao.util.ContainerizedDatabaseTest;
import bio.terra.cbas.models.GithubMethodDetails;
import bio.terra.cbas.models.Method;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TestGithubMethodDetailsDao extends ContainerizedDatabaseTest {
  @Autowired GithubMethodDetailsDao githubMethodDetailsDao;
  @Autowired MethodDao methodDao;

  UUID methodId1 = UUID.randomUUID();
  String methodName = "test method";
  String methodDesc = "test method description";
  String methodSource = "GitHub";

  UUID workspaceId = UUID.randomUUID();

  Method method1 =
      new Method(
          methodId1,
          methodName,
          methodDesc,
          DateUtils.currentTimeInUTC(),
          null,
          methodSource,
          workspaceId);
  GithubMethodDetails details =
      new GithubMethodDetails("cromwell", "broadinstitute", "cbas/dao/test.py", true, methodId1);

  @BeforeEach
  void init() {
    int recordsCreated1 = methodDao.createMethod(method1);
    int githubMethodDetails = githubMethodDetailsDao.createGithubMethodSourceDetails(details);

    assertEquals(1, recordsCreated1);
    assertEquals(1, githubMethodDetails);
  }

  @Test
  void retrievesGithubMethodDetails() {
    GithubMethodDetails methodDetails = githubMethodDetailsDao.getMethodSourceDetails(methodId1);
    assertEquals(methodId1, methodDetails.methodId());
    assertEquals("cromwell", methodDetails.repository());
    assertEquals("cbas/dao/test.py", methodDetails.path());
    assertEquals("broadinstitute", methodDetails.organization());
    assertEquals(true, methodDetails.isPrivate());
  }
}
