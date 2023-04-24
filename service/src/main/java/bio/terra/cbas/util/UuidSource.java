package bio.terra.cbas.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidSource {
  public UUID generateUUID() {
    return UUID.randomUUID();
  }
}
