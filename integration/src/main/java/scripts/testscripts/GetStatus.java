package scripts.testscripts;

import static org.hamcrest.MatcherAssert.assertThat;

import bio.terra.testrunner.runner.TestScript;
import bio.terra.testrunner.runner.config.TestUserSpecification;

public class GetStatus extends TestScript {
  @Override
  public void userJourney(TestUserSpecification testUser) throws Exception {
    assertThat("It works", true);
  }
}
