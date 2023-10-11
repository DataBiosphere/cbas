import requests
import os
import json
import random
import string
import uuid
import time


# TODO: remove this eventually
# rawls_url = "https://rawls.dsde-dev.broadinstitute.org/"
# leo_url = "https://leonardo.dsde-dev.broadinstitute.org/"


# Setup configuration
# These values should be injected into the environment before setup
azure_token = os.environ.get("AZURE_TOKEN")
bee_name = os.environ.get("BEE_NAME")
billing_project_name = os.environ.get("BILLING_PROJECT_NAME")

rawls_url = f"https://rawls.{bee_name}.bee.envs-terra.bio"
leo_url = f"https://leonardo.{bee_name}.bee.envs-terra.bio"


def create_workspace():
    headers = {
        "Authorization": azure_token,
        "accept": "application/json"
    }

    # create a new workspace, need to have attributes or api call doesn't work
    uri = f"{rawls_url}/api/workspaces";
    workspace_name = f"sshah-e2e-api-workspace-{''.join(random.choices(string.ascii_lowercase, k=5))}"
    request_body= {
      "namespace": billing_project_name,
      "name": workspace_name,
      "attributes": {}}

    response = requests.post(url=uri, json=request_body, headers=headers)
    status_code = response.status_code

    if status_code != 201:
        print(response.text)
        exit(1)

    response = json.loads(response.text)
    workspace_id = response['workspaceId']
    print(f"Successfully started workspace creation for '{workspace_name}' in billing project '{billing_project_name}'. Workspace ID returned: {workspace_id}")

    return workspace_id


def create_app(workspace_id, app_type, access_scope):
    print(f"\nCreating {app_type} in workspace {workspace_id}...")
    uri = f"{leo_url}/api/apps/v2/{workspace_id}/terra-app-{str(uuid.uuid4())}"
    body = {
        "appType": f"{app_type}",
        "accessScope": f"{access_scope}"
    }
    headers = {
        "Authorization": azure_token,
        "accept": "application/json"
    }

    response = requests.post(url=uri, json=body, headers=headers)
    # will return 202 or error
    print(response.text)


# Get WDS or WORKFLOWS app proxy url from Leo
def get_app_url(workspace_id, app_type, proxy_url_name):
    """"Get url for wds/cbas."""
    uri = f"{leo_url}/api/apps/v2/{workspace_id}?includeDeleted=false"

    headers = {"Authorization": azure_token,
               "accept": "application/json"}

    response = requests.get(uri, headers=headers)
    status_code = response.status_code

    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully retrieved details for {app_type} app")
    response = json.loads(response.text)

    app_url = ""

    for entries in response:
        if entries['appType'] == app_type and entries['proxyUrls'][proxy_url_name] is not None:
            if(entries['status'] == "PROVISIONING"):
                print(f"{app_type} is still provisioning")
                break
            print(f"App status: {entries['status']}")
            app_url = entries['proxyUrls'][proxy_url_name]
            break

    if app_url is None:
        print(f"{app_type} is missing in current workspace")
    else:
        print(f"'{proxy_url_name}' url in {app_type} app: {app_url}")

    return app_url

# Upload data to WDS
def upload_wds_data(wds_url, workspace_id, tsv_file_name, record_name):
    #open TSV file in read mode
    tsv_file = open(tsv_file_name, "r")
    request_file = tsv_file.read();
    tsv_file.close()

    uri = f"{wds_url}/{workspace_id}/tsv/v0.2/{record_name}"
    headers = {"Authorization": azure_token}

    response = requests.post(uri, files={'records':request_file}, headers=headers)

    status_code = response.status_code

    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully uploaded data to WDS. Response: {response.json()}")


def create_cbas_method(cbas_url, workspace_id):
    method_name = "no-tasks-workflow"
    request_body = {
        "method_name": method_name,
        "method_source": "GitHub",
        "method_url": "https://raw.githubusercontent.com/DataBiosphere/cbas/sps_azure_e2e_test/e2e-test/resources/no-tasks-workflow.wdl",
        "method_version": "develop"
    }

    uri = f"{cbas_url}/api/batch/v1/methods"
    headers = {
        "Authorization": azure_token,
        "accept": "application/json",
        "Content-Type": "application/json"
    }

    response = requests.post(uri, json=request_body, headers=headers)
    status_code = response.status_code

    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully created method {method_name} for workspace {workspace_id}")
    response = json.loads(response.text)

    return response['method_id']


def get_method_version_id(cbas_url, method_id):
    uri = f"{cbas_url}/api/batch/v1/methods?method_id={method_id}"
    headers = {
        "Authorization": azure_token,
        "accept": "application/json"
    }

    response = requests.get(uri, headers=headers)
    status_code = response.status_code

    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully retrieved method details for method ID {method_id}")
    response = json.loads(response.text)

    # the method version we want should be the first element in the array
    return response['methods'][0]['method_versions'][0]['method_version_id']


def submit_no_tasks_workflow(cbas_url, method_version_id):
    uri = f"{cbas_url}/api/batch/v1/run_sets"
    headers = {
        "Authorization": azure_token,
        "accept": "application/json",
        "Content-Type": "application/json"
    }

    #open text file in read mode
    request_body_file = open("resources/submit_workflow_body.json", "r")
    request_body = request_body_file.read().replace("{METHOD_VERSION_ID}", method_version_id);
    request_body_file.close()

    print(f"Submitting workflow to CBAS...")

    response = requests.post(uri, data=request_body, headers=headers)

    status_code = response.status_code
    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully submitted workflow. Response: {response.json()}")

    response = json.loads(response.text)
    return response['run_set_id']


def check_outputs_data(wds_url, workspace_id, record_type, record_name):
    uri = f"{wds_url}/{workspace_id}/records/v0.2/{record_type}/{record_name}"
    headers = {"Authorization": azure_token}

    response = requests.get(uri, headers=headers)

    status_code = response.status_code
    if status_code != 200:
        print(response.text)
        exit(1)
    print(f"Successfully retrieved record details for record '{record_name}' of type '{record_type}'")

    response = json.loads(response.text)

    attributes = response['attributes']

    print("Checking that output attributes exist in record...")
    if 'team' in attributes and 'rank' in attributes:
        print("Outputs were successfully written back to WDS")
    else:
        print("Outputs were not written back to WDS")
        exit(1)


def check_submission_status(cbas_url, method_id, run_set_id):
    uri = f"{cbas_url}/api/batch/v1/run_sets?method_id={method_id}"
    headers = {
        "Authorization": azure_token,
        "accept": "application/json"
    }

    response = requests.get(uri, headers=headers)
    status_code = response.status_code

    if status_code != 200:
        print(response.text)
        exit(1)

    response = json.loads(response.text)
    if response['run_sets'][0]['state'] != 'COMPLETE':
        print(f"Submission '{run_set_id}' not in 'COMPLETE' state. Current state: {response['run_sets'][0]['state']}.")
        exit(1)

    print(f"Submission '{run_set_id}' status: COMPLETE.")


print("Starting Workflows Azure E2E test...")

# Create workspace
print("\nCreating workspace...")
workspace_id = create_workspace()

# Create WORKFLOWS_APP and CROMWELL_RUNNER apps in workspace
create_app(workspace_id, 'WORKFLOWS_APP', 'WORKSPACE_SHARED')
create_app(workspace_id, 'CROMWELL_RUNNER_APP', 'USER_PRIVATE')

# TODO: this should come from create_workspace function
# workspace_id = "c4784c60-34a9-4145-8fd6-f7e2dd6e732d"

# sleep for 5 minutes to allow workspace to provision and apps to start up
print("\nSleeping for 5 minutes to allow workspace to provision and apps to start up...")
time.sleep(5 * 60)

# Upload data to workspace
# check that WDS is ready; if not exit the test
print(f"\nChecking to see if WDS app is ready to upload data for workspace {workspace_id}...")
wds_url = get_app_url(workspace_id, 'WDS', 'wds')
if wds_url == "":
    print(f"WDS app not ready or errored out for workspace {workspace_id}")
    exit(1)
upload_wds_data(wds_url, workspace_id, "resources/cbas-e2e-test-data.tsv", "test-data")

# Submit workflow to CBAS
print(f"\nChecking to see if WORKFLOWS app is ready to submit workflow in workspace {workspace_id}...")
cbas_url = get_app_url(workspace_id, 'WORKFLOWS_APP', 'cbas')
if cbas_url == "":
    print(f"WORKFLOWS app not ready or errored out for workspace {workspace_id}")
    exit(1)

# create a new method
method_id = create_cbas_method(cbas_url, workspace_id)
method_version_id = get_method_version_id(cbas_url, method_id)
run_set_id = submit_no_tasks_workflow(cbas_url, method_version_id)

# sleep for 2 minutes to allow submission to finish
print("\nSleeping for 2 minutes to allow submission to finish and outputs to be written to WDS...")
time.sleep(2 * 60)

# without polling CBAS, check if outputs were written back to WDS
# we don't poll CBAS first to check that the callback API is working
print("\nChecking to see outputs were successfully written back to WDS...")
check_outputs_data(wds_url, workspace_id, 'test-data', '89P13')

# check submission status
print("\nChecking submission status...")
check_submission_status(cbas_url, method_id, run_set_id)

print("\nTest successfully completed. Exiting.")
