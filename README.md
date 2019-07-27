Libraries for high-level interaction with issue trackers through their REST APIs, plus tools for issue-tracker migration.

## Status
[![Build Status](https://travis-ci.org/fschopp/issue-tracking.svg?branch=master)](https://travis-ci.org/fschopp/issue-tracking)

## Overview

- requires Java 11
- tool for importing projects into [JetBrains YouTrack](https://www.jetbrains.com/youtrack/)
  - includes issues and their custom fields, links between issues, issue attachments, and tags
  - based on the YouTrack [Import REST API](https://www.jetbrains.com/help/youtrack/standalone/Import-Issues.html)
  - imports [issues](https://www.jetbrains.com/help/youtrack/standalone/Import-Issues.html), [links between issues](https://www.jetbrains.com/help/youtrack/standalone/Import-Links.html), and [attachment metadata](issue-tracking-youtrack/src/main/java/net/florianschoppmann/issuetracking/youtrack/Attachments.java) from import files in XML format
- experimental low-level tool for importing directly into the YouTrack database
  - can be used for importing activity data, which the YouTrack REST interface currently does not support (see [YouTrack issue JT-44862](https://youtrack.jetbrains.com/issue/JT-44862))
  - **Use at your own risk. Only use a YouTrack test setup for this.**
- tool for exporting projects from [Asana](https://asana.com/) (as YouTrack import files, as described above)
  - based on the [Asana REST API](https://asana.com/developers/api-reference)
  - includes tasks, comments, attachments, tags
  - preserves formatted text in tasks and comments
  - converts Asana [@mentions](https://asana.com/guide/help/fundamentals/text) to YouTrack issue IDs and [user mentions](https://www.jetbrains.com/help/youtrack/standalone/Mention-Another-User.html)
- tool for exporting projects from [Atlassian Jira](https://www.atlassian.com/software/jira) (as YouTrack import files)
  - based on the Jira REST API
  - takes care of what the YouTrack built-in import functionality misses
  - imports issue/epic/subtask/etc. links that YouTrack misses ([JT-51941](https://youtrack.jetbrains.com/issue/JT-51941))
  - fixes user mentions in issue descriptions and comments that YouTrack does not properly convert ([JT-45099](https://youtrack.jetbrains.com/issue/JT-45099))

## License

[Revised BSD (3-Clause) License](LICENSE)

## Binary Releases

None currently. This project should be used with care and might need adjustments for different needs.

## Documentation

For current snapshot:

- [API documentation](https://fschopp.github.io/issue-tracking/snapshot/apidocs)
- [Maven-generated project documentation](https://fschopp.github.io/issue-tracking/snapshot)

## Usage

Build the entire project with `mvn install`. This will produce a stand-alone executable at `issue-tracking-tool/target/distribution/bin/issue-tracking-tool.sh`. It can be used as follows:
```bash
issue-tracking-tool.sh ${tool} ${tool_specific_options}
```
where `${tool}` is one of the classes in package [net.florianschoppmann.issuetracking](issue-tracking-tool/src/main/java/net/florianschoppmann/issuetracking) having a `main` method. Use `tool_specific_options=-h` for help.

It might also be a good idea to run any of these classes directly with a debugger.

### Import to YouTrack

```bash
export YOUTRACK_ACCESS_TOKEN=${youtrack_access_token}
issue-tracking-tool.sh YouTrackImport --url ${youtrack_url} \
  --io /path/to/input-output-directory
```
where:
- `youtrack_access_token` contains the [YouTrack Permanent Token](https://www.jetbrains.com/help/youtrack/standalone/Manage-Permanent-Token.html#obtain-permanent-token),
- `youtrack_url` contains the YouTrack base URL (for example, `https://your-name.myjetbrains.com/youtrack/`).

This command import the data from the “import files” (in XML format) in the given directory. Note that creating users, custom fields, and link types is not currently automated. If necessary, you have to do this manually prior to the import. 


### Low-Level Import Directly to YouTrack Database

```bash
issue-tracking-tool.sh --db ${youtrack_xodus_database} \
  --ii /path/to/input-directory
```
where:
- `youtrack_xodus_database` contains the path to the YouTrack [Xodus](https://github.com/JetBrains/xodus) database (this is a directory with `*.xd` files).

This command imports issue activity (event) data from the “import files” (in XML format) in the given directory. Prior to running this command, the YouTrack process needs to be shut down.


### Export Asana Project

```bash
export ASANA_ACCESS_TOKEN=${asana_access_token}
issue-tracking-tool.sh AsanaExport --workspace ${workspace} \
  --project ${project} --abbrev ${abbrev} \
  --user-mapping /path/to/user-mapping.txt --output /path/to/output-directory
```
where:
- `asana_access_token` contains the [Asana personal access token](https://asana.com/developers/documentation/getting-started/auth#personal-access-token),
- `workspace` contains the name of the Asana workspace,
- `project` contains the name of the Asana project,
- `abbrev` contains the project abbreviation in YouTrack (also called project ID or project short name),
- `user-mapping.txt` is a file containing lines of form `${email}=${youtrack_login}`.

This command creates YouTrack import files in `/path/to/output-directory`. These files contain the project data meant to be uploaded to YouTrack in a second step, as described above. Before continuing, check the export warnings XML file in order to make sure that all references to tasks or users could be resolved.

Note that this tool **only reads** from Asana. Only proceed if the created import files look reasonable.


### Export Jira Project

```bash
export JIRA_USER_NAME=${jira_user_name}
export JIRA_PASSWORD=${jira_password}
export YOUTRACK_ACCESS_TOKEN=${youtrack_access_token}
issue-tracking-tool.sh JiraExport --jira-url ${jira_url} \
  --youtrack-url ${youtrack_url} --abbrev ${abbrev} \
  --output /path/to/output-directory
```
where:
- `jira_user_name` contains the JIRA user name (or email address),
- `jira_password` contains the JIRA password or (much better) [API token](https://confluence.atlassian.com/cloud/api-tokens-938839638.html),
- `youtrack_access_token` contains the [YouTrack Permanent Token](https://www.jetbrains.com/help/youtrack/standalone/Manage-Permanent-Token.html#obtain-permanent-token),
- `jira_url` contains the JIRA URL (for example, `https://your-name.atlassian.net/`),
- `youtrack_url` contains the YouTrack base URL,
- `abbrev` contains the project abbreviation in YouTrack.

This command creates YouTrack import files in `/path/to/output-directory`. These files contain the project data meant to be uploaded to YouTrack in a second step, as described above.

Note that this tool **only reads** from JIRA and YouTrack. Only proceed if the created import files look reasonable.
