# Harvester Image
 
FROM glassfish:4.1
 
COPY harvester.war /
COPY start.sh /
 
EXPOSE 8080
EXPOSE 8686
EXPOSE 4848
 
ENTRYPOINT ["/start.sh"]