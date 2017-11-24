#!/bin/bash
mvn install
cp target/OAIPMH-HarvesterService_5.0.0-SNAPSHOT.war bin/build/harvester.war
cd bin/build
/usr/local/opt/glassfish/libexec/bin/asadmin redeploy --contextroot "/harvester" harvester.war
