#Configuration for development current microservice

##To start development:

1. Add to hosts file:
    ```
    127.0.0.1 ecos-registry
    ```
2. Remove/comment current microservice section from docker-compose file in ecos-dev
3. Standard start alfresco-ecos on _8080_ port
4. Start docker-compose from ecos-dev repo (see readme in ecos-dev) using version of docker-compose from step 2
4. Run microservice to develop by spring boot app.
