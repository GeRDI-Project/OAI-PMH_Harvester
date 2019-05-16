# GeRDI Harvester Image for OAI-PMH Harvesters

FROM jetty:9.4.7-alpine
ENV JETTY_BASE=/var/lib/jetty
# copy war file
COPY target/*.war $JETTY_BASE/webapps/oaipmh.war
COPY config.json $JETTY_BASE/config/OaiPmhHarvesterService/config.json

# create log file folder with sufficient permissions
USER root
RUN mkdir -p /var/log/harvester
RUN chown -R jetty:jetty /var/log/harvester $JETTY_BASE
USER jetty

EXPOSE 8080
