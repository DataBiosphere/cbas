package bio.terra.cbas.controllers;

import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.util.BearerTokenFilter;
import java.util.Optional;
import javax.servlet.ServletRequest;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ControllerUtils {
  private static final Logger logger = LoggerFactory.getLogger(ControllerUtils.class);
  private final ServletRequest servletRequest;
  private final SamService samService;

  @Autowired
  ControllerUtils(ServletRequest servletRequest, SamService samService) {
    this.servletRequest = servletRequest;
    this.samService = samService;
  }

  Optional<String> getUserToken() {
    Object token = servletRequest.getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN);
    return Optional.ofNullable((String) token);
  }

  Optional<UserStatusInfo> getSamUser() {
    return getUserToken().map(samService::getUserStatusInfo);
  }
}
