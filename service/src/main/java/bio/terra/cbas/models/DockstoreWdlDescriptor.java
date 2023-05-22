package bio.terra.cbas.models;

import java.io.Serializable;

public class DockstoreWdlDescriptor implements Serializable {

  private String descriptor;
  private String type;
  private String url;

  public String getUrl() {
    return url;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public String getType() {
    return type;
  }
}
