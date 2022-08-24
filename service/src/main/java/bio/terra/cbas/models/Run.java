package bio.terra.cbas.models;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Run(
    UUID id,
    String engineId,
    UUID runSetId,
    String entityId,
    OffsetDateTime submissionTimestamp,
    String status) {}
