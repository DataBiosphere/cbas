# Composite Batch Analysis Service (C-BAS)

## Contributing

For compliance reasons, all pull requests **must** be submitted with a Jira ID in the pull request title.
You should include the Jira ID near the beginning of the title for better readability.
For example: "[WM-1992] add statement to README.md and DEVELOPMENT.md about including Jira IDs in PR titles"

If there is more than one relevant ticket, include all of their Jira IDs.
For example: "WM-1997, WM-2002, WM-2005: Fix for many bugs with the same root cause"

## Helpful Links

* [Design Doc](https://docs.google.com/document/d/1Gs73JFcujoEDNRfj8s6rp8XFsqEF2syEQeZa7y9e7JY/edit)
* [Developer Setup Guide](/DEVELOPMENT.md)

## Versioning

The C-BAS service follows the Google Cloud API versioning scheme of [semantic versioning](https://semver.org/). For more information, please visit this [document](https://docs.google.com/document/d/1qXNHTijdPn9ApYrznSkTFnxkt0g-o-Uh0SjqQlYd-ZA/edit).

## Tag and Publish Workflows

The following workflows are executed in tandem

* build-and-test.yml
* tag-and-publish.yml

to support consistent versioning among all published `CBAS` artifacts which includes

* `cbas-client` published to artifactory and dockerhub
* `CBAS` consumer pact published to Pact Broker
* `CBAS` provider verification test results published to Pact Broker

Here's the process we follow to maintain versioning consistency.

#### When pushing commits to a pull request branch, the following workflows will be triggered

  * `build-and-test.yml` to run unit or integration tests
  * `consumer-contract-tests.yml` to publish `CBAS` pact to Pact Broker
  * `verify_consumer_pacts.yml` to verify all consumers dependent on `CBAS` and publish the results to Pact Broker
  * Both `CBAS` pact and provider verification results will be published to Pact Broker with a version in the format `<semver>-<7-digit commit hash>`. The `semver` will be the next tag over the current release tag recorded in GitHub. The combined `<semver>-<7-digit commit hash>` is published to Pact Broker for recording purpose. It is important to note that this tag will not be dispatched to GitHub

#### When merging a pull request to the `main` branch, the following workflows will be triggered

  * `build-and-test.yml` to run unit or integration tests
  * `consumer-contract-tests.yml` to publish `CBAS` pact to Pact Broker
  * `verify_consumer_pacts.yml` to verify all consumers dependent on cbas and publish the results to Pact Broker
  * `tag-and-publish.yml` to dispatch the new tag along with publishing cbas images to artifactory and dockerhub
  * Both `CBAS` pact and provider verification results will be published to Pact Broker with the new tag
  * The new tag is going to be the next GitHub release tag over the release tag right before merge and does not carry the 7-digit commit hash
  * All artifacts will use the new tag consistently when publishing during merge.
  * In addition, the `bumptagbot` will bypass `tag-and-publish.yml` so tagging won't happen again right after merge

