# Developing BatchAnalysis

TODO: Complete me! Should be of similar quality to the ECM documentation.

## Contributing

For compliance reasons, all pull requests **must** be submitted with a Jira ID in the pull request title.
You should include the Jira ID near the beginning of the title for better readability.
For example: "[WM-1992] add statement to README.md and DEVELOPMENT.md about including Jira IDs in PR titles" 

If there is more than one relevant ticket, include all of their Jira IDs.
For example: "WM-1997, WM-2002, WM-2005: Fix for many bugs with the same root cause"

## Setup

Please note: These setup steps were copied directly from [ECM](https://github.com/DataBiosphere/terra-external-credentials-manager/blob/dev/DEVELOPMENT.md). Anything
that seems "off" is probably a result of that. Feel free to submit a PR to fix this up!

### Prerequisites:

- Verify that your checked-out repo is protected by `git-secrets`
  - Run `git-secrets --list` from the repo root. A long list of configured rules should print out.
    - DSP-issued workstations should have everything pre-configured
  - If the command is not found, run `brew install git-secrets`
  - If there are no rules, use the script [here](https://github.com/broadinstitute/dsp-appsec-gitsecrets-client#setup) to add them
- Install Postgres 13: https://www.postgresql.org/download/
  - [The app](https://postgresapp.com/downloads.html) may be easier, just make sure to download the right version. It'll manage things for you and has a useful menulet where the server can be turned on and off. Don't forget to create a server if you go this route.
- Install Adoptium Java 17 (Temurin). Here's an easy way on Mac, using [jEnv](https://www.jenv.be/) to manage the active version:

    ```sh
    # if on Mac, you may need to install Terminal developer tools first
    xcode-select --install

    brew install jenv
    # follow postinstall instructions to activate jenv...

    # to add previously installed versions of Java to jEnv, list them:
    # /usr/libexec/java_home -V
    # and then add them:
    # jenv add /Library/Java/JavaVirtualMachines/<JAVA VERSION HERE>/Contents/Home

    brew install homebrew/cask-versions/temurin17

    jenv add /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
    ```

**NOTE**: You may encounter issues with the application when running an unexpected version of Java. So make sure you are running `Temurin-17` as specified above.


### Database Configuration
The Batch Analysis service relies on a Postgresql database server. There are two options for running the Postgres server:

- Manual setup:
  Setup Postgres using whatever method you like.
- Convenient app setup:
  Install [the convenient app](https://postgresapp.com/), and create a database called `bio.terra.batchanalysis`.

Make sure to use the correct version, as noted above.

#### Initialize your database:
```sh
psql -h 127.0.0.1 -U postgres -f ./common/postgres-init.sql
```
***N.B.*** If you used **the convenient app**, you should run `psql` as `/Applications/Postgres.app/Contents/Versions/13/bin/psql`. To add Postgres commands to your path, run the following command:
```sh
sudo mkdir -p /etc/paths.d && echo /Applications/Postgres.app/Contents/Versions/13/bin | sudo tee /etc/paths.d/postgresapp
```


### Dependencies

*this section is a work-in-progress*

#### Workspace Data Service (WDS)

Create an empty WDS database in the PostgreSQL server you created above.
```sql
CREATE DATABASE wds;
```

Start a WDS container with the following command:
Replace [YOUR_USERNAME] with something like mspector

```sh
docker run \
  --name "WDS_6786552" \
  -e WDS_DB_HOST='host.docker.internal' \
  -e WDS_DB_USER='[YOUR_USERNAME]' \
  -e SAM_URL=https://sam.dsde-dev.broadinstitute.org/ \
  -p 8001:8080 us.gcr.io/broad-dsp-gcr-public/terra-workspace-data-service:0.2.57
```

A few notes:
- The `--name` flag is optional, but recommended for easier container management.
- At the time of this writing, `us.gcr.io/broad-dsp-gcr-public/terra-workspace-data-service` does not have an image with the `latest` tag. Take care to specify the intended tag!


With the container running, initialize an instance with any UUID, e.g. UUID `00000000-0000-0000-0000-000000000000`.
Note that CBAS UI expects an instance with UUID `15f36863-30a5-4cab-91f7-52be439f1175`.
```sh
curl -X 'POST' \
  'http://localhost:8001/instances/v0.2/15f36863-30a5-4cab-91f7-52be439f1175/' \
  -H 'accept: */*' \
  -d ''
```

Then add a record `FOO1` of type `FOO` to the instance you just created. Make sure to use the same UUID:
```sh
curl -X 'PUT' \
  'http://localhost:8001/15f36863-30a5-4cab-91f7-52be439f1175/records/v0.2/FOO/FOO1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "attributes": {
    "foo_rating": 1000
  }
}'
```

In the `wds` database, the record will be written to a schema with the same name as the instance ID.
Select that schema with the following SQL command (for e.g. the instance ID `15f36863-30a5-4cab-91f7-52be439f1175`):

```sql
SET search_path TO "15f36863-30a5-4cab-91f7-52be439f1175";
```

This command needs to be run each time you wish to directly query the database. To set it
permanently, you can run the following command with your instance ID:

```sql
ALTER DATABASE wds SET search_path TO "15f36863-30a5-4cab-91f7-52be439f1175";
```

Then, run the Postgres command `\dt` to show the existing tables.
You can then use psql commands to view items in the table `FOO`:

```
SELECT * FROM "FOO";
```

You should see a row with name `FOO1` and `foo_rating` 1000.

## Running

### Running Tests

All unit tests can be run with a Gradle configuration that runs `:service:unitTest`. If you get a message
`Test events were not received`, this means nothing has changed since the previous test run.
Integration tests can be run by following the instructions in the [integration README](./integration/README.md).

### Running The Service Locally

Run in IntelliJ (recommended) or use the command line:

```sh
cd service
../gradlew bootRun
```

Then navigate to the Swagger: `http://localhost:8080/swagger-ui.html`

### IntelliJ Setup

1. Open the repo normally (File > New > Project From Existing Sources). Select the folder, and then select Gradle as the external model.
2. In project structure (the folder icon with a little tetromino over it in the upper
   right corner), make sure the project SDK is set to Java 17 (>= 17.0.3). If not, IntelliJ should
   detect it on your system in the dropdown, otherwise click "Add JDK..." and navigate to
   the folder from the last step.
3. Set up [google-java-format](https://github.com/google/google-java-format). We use the
   spotless checker to force code to a standard format. Installing the IntelliJ plug-in
   and library makes it easier to get it in the right format from the start.
4. See some optional tips below in the ["Tips"](#tips) section.

## Tips
- Check out [gng](https://github.com/gdubw/gng), it'll save you typing `./gradlew` over
  and over, and also takes care of knowing when you're not in the root directory so you
  don't have to figure out the appropriate number of `../`s.
- In IntelliJ, instead of running the local server with `bootRun`, use the Spring
  Boot configuration that IntelliJ auto-generates (`-cp cbas.service.main`, main class
  `bio.terra.cbas.App`). To edit it, click on it (in the upper
  right of the window), and click `Edit Configurations`.
    - For readable logs, put `human-readable-logging` in the `Active Profiles` field.

### Pushing a new image

New images are built and pushed automatically each time a PR is merged. Developers may
want to do this manually in order to test in-development CBAS versions in non-local
environments. 

First, log into GCP using `gcloud auth login`. You need this to authenticate to push the image.

```
TAG="my-custom-image-name"
cd service/
../gradlew :service:jibDockerBuild --image=us.gcr.io/broad-dsp-gcr-public/cbas:$TAG -Djib.console=plain
docker push us.gcr.io/broad-dsp-gcr-public/cbas:$TAG
```