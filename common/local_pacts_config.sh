#!/bin/bash

PACT_DIR="$(git rev-parse --show-toplevel)/pacts"
CONSUMER_NAME="$(basename `git rev-parse --show-toplevel`)-consumer"
GIT_SHA=$(git rev-parse --verify HEAD)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# write out contract payloads
for PACT_FILE in $PACT_DIR/*.json;
  do jq \
    "{ consumerName: .consumer.name, \
       providerName: .provider.name, \
       specification: \"pact\", \
       contentType: \"application/json\", \
       content: \"$(cat $PACT_FILE  | base64 -w 0)\" \
     }" \
  < $PACT_FILE > $PACT_FILE.payload
done

CONTRACTS=$(jq -n '[{ list: inputs } | add]' $PACT_DIR/*.payload)

PAYLOAD=$(echo '{
  "pacticipantName": "'${CONSUMER_NAME}'",
  "pacticipantVersionNumber": "'${GIT_SHA}'",
  "branch": "'${GIT_BRANCH}'",
  "buildUrl": "'${BUILD_URL}'",
  "contracts": '${CONTRACTS}'
}' | jq)

echo $PAYLOAD

curl -v \
  --header "Content-Type: application/json; charset=utf-8" \
  --request POST \
  -d "$PAYLOAD" \
  localhost:9292/contracts/publish
