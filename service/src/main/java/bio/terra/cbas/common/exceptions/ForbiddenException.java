package bio.terra.cbas.common.exceptions;

import bio.terra.common.exception.ErrorReportException;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends ErrorReportException {

  public ForbiddenException(String samResourceType, String samActionType) {
    super(
        "User doesn't have '%s' permission on '%s' resource"
            .formatted(samActionType, samResourceType),
        new ArrayList<>(),
        HttpStatus.FORBIDDEN);
  }
}
