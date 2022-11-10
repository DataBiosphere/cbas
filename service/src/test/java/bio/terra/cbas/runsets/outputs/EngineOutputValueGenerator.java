package bio.terra.cbas.runsets.outputs;

import cromwell.client.model.RunLog;
import java.io.IOException;
import java.util.Map;

public final class EngineOutputValueGenerator {
  private EngineOutputValueGenerator() {
    // Never used for static utility class.
  }

  public static Object singleCromwellOutput(String key, String rawJsonValue) throws IOException {
    String rawJson =
        """
        {
          "outputs": {
            "%s": %s
          }
        }
        """
            .formatted(key, rawJsonValue);

    return RunLog.fromJson(rawJson).getOutputs();
  }

  public static Object multipleCromwellOutputs(Map<String, String> rawJsonValues)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    var entries = rawJsonValues.entrySet().iterator();

    sb.append("{ \"outputs\": { ");

    while (entries.hasNext()) {
      Map.Entry<String, String> mapEntry = entries.next();

      sb.append('"');
      sb.append(mapEntry.getKey());
      sb.append('"');
      sb.append(": ");
      sb.append(mapEntry.getValue());
      if (entries.hasNext()) {
        sb.append(", ");
      }
    }

    sb.append("} }");

    return RunLog.fromJson(sb.toString()).getOutputs();
  }
}
