package bio.terra.cbas.models;

import bio.terra.cbas.model.RunStateResponse;
import java.util.List;

public record SubmitRunResponse(
    List<RunStateResponse> runStateResponseList, List<String> successfullyInitializedWorkflowIds) {}
