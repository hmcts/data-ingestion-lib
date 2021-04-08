# data-ingestion-lib
Data ingestion lib creates reusable framework library with Spring boot and Apache Camel Integration to read CSV files from Azure Blob
and transform/converts and stores it in spring microservice project database.

This library has been published in bin tray with git push actions on new release (https://jitpack.io/#hmcts/data-ingestion-lib).
It can be used in respective projects as gradle dependency like below
compile group: 'uk.gov.hmcts.reform', name: 'data-ingestion-lib', version: '0.2.5'

# To build the project in local execute the following command
./gradlew build 

# How to use library
Common library properties like email settings configured in library and customized properties should be configured with specific 
microservices eg. rd-judicial-data-load (https://github.com/hmcts/rd-judicial-data-load)





