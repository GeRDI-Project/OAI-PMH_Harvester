# An OAI-PMH Harvester
========================

This is an OAI-PMH capable harvester build by Jan Froemberg ([jan.froemberg@tu-dresden.de](jan.froemberg@tu-dresden.de)). It supports the metadata standards DublinCore and DataCite3.
This harvester was build on top of a RESTful-Harvester Library provided by the University of Kiel (Robin Weiss [row@informatik.uni-kiel.de]).

## Prerequisites
--------------

Docker if you plan to build the harvester within a Docker-Image.
Otherwise you need a Java Application Server like Glassfish, Tomcat or Jetty.
There you have to deploy the created war-File.

## How to build?
--------------

You can easily build a docker image by using the terminal and typing:
`mvn clean verify -PdockerBuild`

There is also a utility script for building and running a Jetty docker container via:
`mvn clean verify -PdockerRun`


## How to run?
--------------

Base-URL: [http://localhost:8080/oaipmh](http://localhost:8080/oaipmh)

Requests on Resource : /harvest

    * GET       Overview
    * POST			Starts the harvest
    * POST/abort		Aborts an ongoing harvest
    * POST/submit		Submits harvested documents to a DataBase
    * POST/save		Saves harvested documents to disk

Request on Resource : /harvest/config

    * GET     Overview
    * POST		Saves the current configuration to disk.
    * PUT 		Sets x-www-form-urlencoded parameters for the harvester.
			Valid values: harvestFrom, harvestTo, from, until, hostUrl, metadataPrefix, autoSave, autoSubmit, submissionUrl, submissionUserName, submissionPassword, submissionSize, readFromDisk, writeToDisk, keepCachedDocuments, deleteFailedSaves.

All libraries and bundles included in this build are
released under the Apache license.

Enjoy!
