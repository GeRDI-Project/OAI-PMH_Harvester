#!/bin/bash
mvn install
cp target/OAIPMH-HarvesterService_5.0.0-SNAPSHOT.war bin/build/harvester.war
cd bin/build
asadmin deploy --contextroot "/harvester" harvester.war
