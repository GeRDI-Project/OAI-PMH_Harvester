# An OAI-PMH Harvester

This is an OAI-PMH capable harvester build by Jan Frömberg ([jan.froemberg@tu-dresden.de](jan.froemberg@tu-dresden.de)).
It supports the metadata standards DublinCore and DataCite3.1.
This harvester was build on top of a [RESTful-Harvester Library][5] provided by the University of Kiel ([Robin Weiss](row@informatik.uni-kiel.de)).
It is a first build-release to support a harvesting use case for the [PANGAEA Metadata-Repository][6].

## Prerequisites

[Docker][1] if you plan to build the harvester within a Docker-Image.
Otherwise you need a Java Application Server like [Glassfish][2], [Tomcat][3] or [Jetty][4]
There you have to deploy the created war-File.

## How to build?

You can easily build a docker image by using the terminal and typing:

    $ mvn clean verify -PdockerBuild

There is also a utility script for building and running a Jetty docker container via:

    $ mvn clean verify -PdockerRun

## How to run?

Base-URL: [http://localhost:8080/oaipmh](http://localhost:8080/oaipmh)

Requests on Resource : /harvest

    * GET			Overview
    * POST			Starts the harvest
    * POST/abort		Aborts an ongoing harvest
    * POST/submit		Submits harvested documents to a DataBase
    * POST/save		Saves harvested documents to disk

Request on Resource : /harvest/config

    * GET		Overview
    * POST		Saves the current configuration to disk.
    * PUT 		Sets x-www-form-urlencoded parameters for the harvester.
    (PUT) Valid values: harvestFrom, harvestTo, from, until, hostUrl, metadataPrefix, autoSave, autoSubmit, submissionUrl,
    submissionUserName, submissionPassword, submissionSize, readFromDisk, writeToDisk, keepCachedDocuments, deleteFailedSaves.

All libraries and bundles included in this build are
released under the Apache license.

Enjoy!

[1]: www.docker.com
[2]: https://javaee.github.io/glassfish/
[3]: https://tomcat.apache.org/
[4]: https://www.eclipse.org/jetty/
[5]: https://code.gerdi-project.de/projects/HAR/repos/harvesterbaselibrary
[6]: http://ws.pangaea.de/oai/provider