package bio.terra.cbas.models;

import bio.terra.cbas.model.RunStateResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordResponse;

public record RunSetSubmissionParameters(
    RunSet runSet,
    String rawMethodUrl,
    String workflowOptionsJson,
    List<Map<UUID, String>> batchedWorkflowInputMaps,
    List<Map<UUID, RunAndRecordDetails>> batchedRunAndRecordMaps,
    List<List<RecordResponse>> batches,
    List<RunStateResponse> runStateErrors) {}
