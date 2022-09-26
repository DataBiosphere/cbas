package bio.terra.cbas.models;

import java.util.UUID;

public record Method(
    UUID id,
    String methodUrl,
    String inputDefinition,
    String outputDefinition,
    String entityType) {}
