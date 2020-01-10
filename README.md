# SWA-AWS-Retriever
Retrieves data from the AWS cloud-server, and inserts them into the data-server's MySQL database  
  
AWS_Retriever.jar is the executable (Exported Runnable JAR):  
`java -jar AWS_Retriever.jar {AWS_RESTful_API} {SQL_HOST} {SQL_USERNAME} {SQL_USERPASS} {SQL_DB} {SQL_PORT}`
- `AWS_RESTful_API`: Cloud-server RESTful API, i.e. https://api.smartwater.ml/
- `SQL_HOST`: The data-server, likely _localhost_
- `SQL_USERNAME`: The _MySQL_ user name
- `SQL_USERPASS`: The _MySQL_ user password
- `SQL_DB`: The _MySQL_ database name
- `SQL_PORT`: The _MySQL_ database name

Performs data retrieval hourly, specifically flow sensor, flush sensor and health report entries.  
  
_**Will expand on other details soon**_
