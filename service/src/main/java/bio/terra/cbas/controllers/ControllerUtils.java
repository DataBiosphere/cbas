package bio.terra.cbas.controllers;

import bio.terra.cbas.dependencies.sam.SamService;
import bio.terra.cbas.util.BearerTokenFilter;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ControllerUtils {
  private final SamService samService;

  @Autowired
  ControllerUtils(SamService samService) {
    this.samService = samService;
  }

  Optional<String> getUserToken() {
    Object token =
        RequestContextHolder.currentRequestAttributes()
            .getAttribute(BearerTokenFilter.ATTRIBUTE_NAME_TOKEN, RequestAttributes.SCOPE_REQUEST);
    return Optional.ofNullable((String) token);
  }

  Optional<UserStatusInfo> getSamUser() {
    return getUserToken().map(samService::getUserStatusInfo);
  }
}
