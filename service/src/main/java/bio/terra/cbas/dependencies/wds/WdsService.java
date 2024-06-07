package bio.terra.cbas.dependencies.wds;

import bio.terra.cbas.common.exceptions.DependencyNotAvailableException;
import bio.terra.cbas.config.WdsServerConfiguration;
import bio.terra.common.iam.BearerToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.databiosphere.workspacedata.client.ApiException;
import org.databiosphere.workspacedata.model.RecordQueryResponse;
import org.databiosphere.workspacedata.model.RecordRequest;
import org.databiosphere.workspacedata.model.RecordResponse;
import org.databiosphere.workspacedata.model.SearchFilter;
import org.databiosphere.workspacedata.model.SearchRequest;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class WdsService {

  private final WdsClient wdsClient;
  private final WdsServerConfiguration wdsServerConfiguration;
  private final RetryTemplate listenerResetRetryTemplate;

  public record WdsRecordResponseDetails(
      List<RecordResponse> recordResponseList, Map<String, String> recordIdsWithError) {
    WdsRecordResponseDetails addAll(WdsRecordResponseDetails moreDetails) {
      List<RecordResponse> merged = new ArrayList<>(recordResponseList);
      merged.addAll(moreDetails.recordResponseList());

      Map<String, String> mergedErrors = new HashMap<>(recordIdsWithError);
      mergedErrors.putAll(moreDetails.recordIdsWithError());

      return new WdsRecordResponseDetails(merged, mergedErrors);
    }
  }

  public WdsService(
      WdsClient wdsClient,
      WdsServerConfiguration wdsServerConfiguration,
      RetryTemplate listenerResetRetryTemplate) {
    this.wdsClient = wdsClient;
    this.wdsServerConfiguration = wdsServerConfiguration;
    this.listenerResetRetryTemplate = listenerResetRetryTemplate;
  }

  public RecordResponse getRecord(String recordType, String recordId, BearerToken userToken)
      throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () ->
            wdsClient
                .recordsApi(userToken)
                .getRecord(
                    wdsServerConfiguration.instanceId(),
                    wdsServerConfiguration.apiV(),
                    recordType,
                    recordId));
  }

  public RecordResponse updateRecord(
      RecordRequest request, String type, String id, BearerToken userToken)
      throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () -> {
          wdsClient
              .recordsApi(userToken)
              .updateRecord(
                  request,
                  wdsServerConfiguration.instanceId(),
                  wdsServerConfiguration.apiV(),
                  type,
                  id);
          return null;
        });
  }

  public WdsRecordResponseDetails getRecords(
      String recordType, List<String> recordIds, BearerToken userToken) {

    int batches = (recordIds.size() / wdsServerConfiguration.queryWindowSize()) + 1;
    WdsRecordResponseDetails responseDetails =
        new WdsRecordResponseDetails(new ArrayList<>(), Map.of());

    for (int i = 0; i < batches; i++) {
      int startIndex = i * wdsServerConfiguration.queryWindowSize();
      int endIndex = Math.min((i + 1) * wdsServerConfiguration.queryWindowSize(), recordIds.size());

      List<String> batch = recordIds.subList(startIndex, endIndex);

      WdsRecordResponseDetails batchResponse = getRecordsBatch(recordType, batch, userToken);
      responseDetails = responseDetails.addAll(batchResponse);
    }

    return responseDetails;
  }

  private WdsRecordResponseDetails getRecordsBatch(
      String recordType, List<String> recordIds, BearerToken userToken) {

    try {
      RecordQueryResponse queryResponse =
          queryRecords(
              new SearchRequest()
                  .filter(new SearchFilter().ids(recordIds))
                  .offset(0)
                  .limit(wdsServerConfiguration.queryWindowSize()),
              recordType,
              userToken);
      return interpretQueryResponse(recordIds, queryResponse);
    } catch (WdsServiceException e) {
      Map<String, String> allRecordIdsAreErrors =
          recordIds.stream()
              .map(id -> Map.entry(id, e.getMessage()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      return new WdsRecordResponseDetails(new ArrayList<>(), allRecordIdsAreErrors);
    }
  }

  private RecordQueryResponse queryRecords(
      SearchRequest request, String recordType, BearerToken userToken) throws WdsServiceException {
    return executionWithRetryTemplate(
        listenerResetRetryTemplate,
        () ->
            wdsClient
                .recordsApi(userToken)
                .queryRecords(
                    request,
                    wdsServerConfiguration.instanceId(),
                    wdsServerConfiguration.apiV(),
                    recordType));
  }

  private WdsRecordResponseDetails interpretQueryResponse(
      List<String> expectedIds, RecordQueryResponse recordQueryResponse) {
    ArrayList<RecordResponse> recordResponseList = new ArrayList<>();
    Map<String, String> recordIdsWithError = new HashMap<>();

    Map<String, RecordResponse> foundResponses =
        recordQueryResponse.getRecords().stream()
            .collect(Collectors.toMap(RecordResponse::getId, r -> r));
    for (String expectedId : expectedIds) {
      RecordResponse recordResponse = foundResponses.get(expectedId);
      if (recordResponse != null) {
        recordResponseList.add(recordResponse);
      } else {
        recordIdsWithError.put(expectedId, "Record not found");
      }
    }

    return new WdsRecordResponseDetails(recordResponseList, recordIdsWithError);
  }

  interface WdsAction<T> {
    T execute() throws ApiException, DependencyNotAvailableException;
  }

  @SuppressWarnings("java:S125") // The comment here isn't "commented code"
  static <T> T executionWithRetryTemplate(RetryTemplate retryTemplate, WdsAction<T> action)
      throws WdsServiceException {

    // Why all this song and dance to catch exceptions and map them to almost identical exceptions?
    // Because the RetryTemplate's execute function only allows us to declare one Throwable type.
    // So we have a top-level WdsServiceException that we can catch and handle, and then we have
    // subclasses of that exception representing the types of exception that can be thrown. This
    // way, we can keep well typed exceptions (no "catch (Exception e)") and still make use of the
    // retry framework.
    return retryTemplate.execute(
        context -> {
          try {
            return action.execute();
          } catch (ApiException e) {
            throw new WdsServiceApiException(e);
          } catch (DependencyNotAvailableException e) {
            throw new WdsServiceNotAvailableException(e);
          }
        });
  }
}
