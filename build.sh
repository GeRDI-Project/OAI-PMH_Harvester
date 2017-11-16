#!/bin/bash
mvn install
cp target/OAIPMH-HarvesterService_5.0.0-SNAPSHOT.war bin/build/harvester.war
cd bin/build
docker build -t op-harvester-container:5.0 .
docker rm -f oaipmh_harvester
docker run --name oaipmh_harvester -d -p 8080:8080 --net elasticsearchmapping_elk op-harvester-container:5.0
docker ps
