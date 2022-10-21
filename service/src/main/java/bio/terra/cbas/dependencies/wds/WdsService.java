package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.cbas.model.RunSetRequest;
import java.util.List;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.AttributeSchema;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.springframework.stereotype.Component;

@Component
public class WdsService {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;

  public WdsService(WdsClient wdsClient, WdsServerConfiguration wdsServerConfiguration) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
  }

  public RecordResponse getRecord(String recordType, String recordId) throws ApiException {
    return wdsClient
        .recordsApi()
        .getRecord(
            wdsServerConfiguration.instanceId(),
            wdsServerConfiguration.apiV(),
            recordType,
            recordId);
  }

  public RecordResponse updateRecord(RecordRequest request, String type, String id)
      throws ApiException {
    return wdsClient
        .recordsApi()
        .createOrReplaceRecord(
            request, wdsServerConfiguration.instanceId(), wdsServerConfiguration.apiV(), type, id);
  }

  public boolean checkIfAttributeExists(RunSetRequest request) throws ApiException {

    boolean exists = true;
    List<AttributeSchema> wdsAttributes =
        wdsClient
            .schemaApi()
            .describeRecordType(
                wdsServerConfiguration.instanceId(),
                wdsServerConfiguration.apiV(),
                request.getWdsRecords().getRecordType())
            .getAttributes();

    //    for (WorkflowInputDefinition singleInputAttribute : request.getWorkflowInputDefinitions())
    // {
    //      if (wdsAttributes.contains(singleInputAttribute)) {
    //        exists = false;
    //      }
    //    }
    return exists;
  }
}
