import requests
import os
import json
import random
import string
import uuid
import time


# TODO: these should be obtained from Github action eventually
# workspace_name = ???


# Commenting out GHS specific variables for now
azure_token = "???"
# bee_name = os.environ['BEE_NAME']
# billing_project_name = os.environ['BILLING_PROJECT_NAME']
# number_of_workspaces = 1
# wds_upload=False
# cbas_submit_workflow=False
# number_of_workflows_to_kick_off = 1

# rawls_url = f"https://rawls.{bee_name}.bee.envs-terra.bio"
# leo_url = f"https://leonardo.{bee_name}.bee.envs-terra.bio"
#
# TODO: remove this eventually
rawls_url = "https://rawls.dsde-dev.broadinstitute.org/"
leo_url = "https://leonardo.dsde-dev.broadinstitute.org/"


# GET WDS or CBAS ENDPOINT URL FROM LEO
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
    print(f"Successfully retrieved details for {app_type} app.")
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

# UPLOAD DATA TO WORSPACE DATA SERVICE IN A WORKSPACE
def upload_wds_data(wds_url, workspace_id, tsv_file_name, record_name):
    #open TSV file in read mode
    tsv_file = open(tsv_file_name, "r")
    request_file = tsv_file.read();
    tsv_file.close()

    uri = f"{wds_url}/{workspace_id}/tsv/v0.2/{record_name}"
    headers = {"Authorization": azure_token}

    response = requests.post(uri, files={'records':request_file}, headers=headers)
    print(response.json())


def create_cbas_method(cbas_url, workspace_id):
    method_name = "test_method_1"
    # TODO: figure out the method for testing
    request_body = {
        "method_name": method_name,
        "method_source": "GitHub",
        "method_url": "https://github.com/broadinstitute/cromwell/blob/develop/wdl/transforms/draft3/src/test/cases/static_value_workflow.wdl",
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
    retrun response['methods'][0]['method_versions'][0]['method_version_id']


def start():
    # steps:
    # - create a workspace
    # - wait for WDS to start up
    # - create WORKFLOWS_APP
    # - create CROMWELL_RUNNER app
    # -DONE fetch WDS url
    # -DONE upload data to WDS app
    # -DONE fetch CBAS url
    # -DONE create a method in CBAS (that doesn't have engine tasks)
    # -DONE get method version id
    # - submit workflow using the method and data from WDS
    # - sleep for X time
    # - check that data was written to WDS (before polling CBAS to ensure callback works)

    # Commenting out the workspace/provisioning steps for now
    # workspace_id = create_workspace()


    # TODO: this should come from create_workspace function
    workspace_id = "45e6cbe5-b2af-4c6d-978e-69d3954f4c05"

    # sleep for 5 minutes to allow workspace to provision and apps to start up
    # time.sleep(5 * 60)

    # Upload data to workspace
    # check that WDS is ready; if not exit the test
    # print(f"Checking to see if WDS app is ready to upload data for workspace {workspace_id}")
    # wds_url = get_app_url(workspace_id, 'WDS', 'wds')
    # if wds_url == "":
    #     print(f"WDS app not ready or errored out for workspace {workspace_id}")
    #     exit(1)
    # upload_wds_data(wds_url, workspace_id, "resources/e2e-testing-sra.tsv", "test2")

    # Submit workflow to CBAS
    print(f"Checking to see if WORKFLOWS app is ready to submit workflow in workspace {workspace_id}")
    cbas_url = get_app_url(workspace_id, 'WORKFLOWS_APP', 'cbas')
    if cbas_url == "":
        print(f"WORKFLOWS app not ready or errored out for workspace {workspace_id}")
        exit(1)

    # create a new method
    method_id = "bed79b6c-037c-4466-9df6-35d051b9394a" # create_cbas_method(cbas_url, workspace_id) TODO -- this should be reverted
    method_version_id = get_method_version_id(cbas_url, method_id)




start()
