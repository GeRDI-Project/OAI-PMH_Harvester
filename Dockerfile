# GeRDI Harvester Image for OAI-PMH Harvesters

FROM jetty:9.4.7-alpine

COPY \/target\/*.war $JETTY_BASE\/webapps\/oaipmh.war

EXPOSE 8080