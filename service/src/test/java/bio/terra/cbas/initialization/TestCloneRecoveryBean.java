package bio.terra.cbas.initialization;

import bio.terra.cbas.dao.MethodDao;
import bio.terra.cbas.dao.MethodVersionDao;
import bio.terra.cbas.dao.RunDao;
import bio.terra.cbas.dao.RunSetDao;
import bio.terra.cbas.models.Method;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {CloneRecoveryBean.class})
public class TestCloneRecoveryBean {
  @MockBean RunDao runDao;
  @MockBean RunSetDao runSetDao;
  @MockBean MethodDao methodDao;
  @MockBean MethodVersionDao methodVersionDao;
  @Autowired CloneRecoveryBean cloneRecoveryBean;

  @Test
  void initialTest() {
    cloneRecoveryBean.cloneRecovery();
  }

  @Test
  private static final Method neverRunMethod1 =
      new Method(
          UUID.randomUUID(),
          "method1",
          "method one",
          OffsetDateTime.now(),
          null,
          "method 1 source",
          UUID.randomUUID());
}
