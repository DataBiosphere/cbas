package bio.terra.cbas.models;

import bio.terra.cbas.model.RunStateResponse;
import java.util.List;

public record SubmitRunSetResponse(
    List<RunStateResponse> runStateResponseList, List<String> successfullyInitializedWorkflowIds) {}
