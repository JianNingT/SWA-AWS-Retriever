# SWA-AWS-Retriever
Retrieves data from the AWS cloud-server, and inserts them into the data-server's MySQL database  
  
AWS_Retriever.jar is the executable (Exported Runnable JAR):  
`java -jar AWS_Retriever.jar {AWS_RESTful_API} {SQL_HOST} {SQL_USERNAME} {SQL_USERPASS} {SQL_DB} {SQL_PORT}`
- `AWS_RESTful_API`: cloud-server RESTful API, i.e. https://api.smartwater.ml
- `SQL_HOST`: the data-server, likely _localhost_
- `SQL_USERNAME`: the _MySQL_ user name
- `SQL_USERPASS`: the _MySQL_ user password
- `SQL_DB`: the _MySQL_ database name
- `SQL_PORT`: the _MySQL_ interface port

Performs data retrieval hourly, specifically flow sensor, flush sensor and health report entries.  
  
_**Will expand on other details soon**_
