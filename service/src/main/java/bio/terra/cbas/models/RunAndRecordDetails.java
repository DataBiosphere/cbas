package bio.terra.cbas.models;

import java.util.UUID;
import org.databiosphere.workspacedata.model.RecordResponse;

public record RunAndRecordDetails(UUID runId, RecordResponse recordResponse) {}
