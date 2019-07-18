# ![](screenshots/sweaglelogo.png)         Jenkins Plugin   

SWEAGLE is a config data management platform supporting all types of config data from applications, to cloud and legacy infrastructure.  This plugin allows for checking in and upload config data into SWEAGLE so it can be versioned, validated, and made consumable in many different formats. 

The plugin supports for jenkins build steps ay for stages of a config data lifecycle. Click on the links below for more details on configuring each action

#### 			Upload --> Validate --> Snapshot --> Export

## Global Configuration
![](screenshots/globalConfigure.png)

| SWEAGLE Tenant URL | SaaS or On-Premise URL of your SWEAGLE instance              |
| ------------------ | :----------------------------------------------------------- |
| SWEAGLE API Key    | API key from API type user with appropriate permssions required for automating SWEAGLE tasks |

Test the connection before saving.  A successful connection will show the amount of Metadata Sets available to the API Key.
## Upload Build Step

![upload](screenshots/upload.png)

| File Location | Location of the file in the workspace to be uploaded.        |
| ------------- | ------------------------------------------------------------ |
| Node Path     | Enter the path where the data should be uploaded to. seperate the nodenames in the path with a comma. In case the nodes do not yet exist, they will be created automatically. |
| File format   | json \| yaml \| xml \| txt \| properties \| ini Any of the supported formats |



## Validate Build Step

![validate](screenshots/validate.png)

| MDS Name                      | The exact name of the incoming metadata set which should be validated. |
| ----------------------------- | ------------------------------------------------------------ |
| Fail build on validate errors | A threshold used to determine if the build should be failed based on the number of validation errors or warnings.  Use -1 to ignore either threshold. |



## Snapshot Build Step

![snapshot](screenshots/snapshot.png)

| MDS Name               | Exact name of pending metadata set for which the snapshot should be taken |
| ---------------------- | ------------------------------------------------------------ |
| Description            | Description of the snapshot                                  |
| Tag                    | A single string as TAG for that snapshot - cannot contain spaces but can utilize Jenkins variable4s ex "release1.2.${BUILD_ID}" |
| Fail Build on Snapshot | If the snapshot should fail for any reason, fail the build.  |



## Get Config Build Step

![export](screenshots/export.png)

| MDS Name      | The stored Metadata set to use for retrieving the config data. |
| ------------- | ------------------------------------------------------------ |
| File Location | The location and filename in the Jenkins workspace to save the retrieved config data. |
| Exporter      | The SWEAGLE exporter to use when retrieving the config data. |