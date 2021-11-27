# Server Setup

Install Joindesk using Docker

### Copy required files
    - docker-compose.yaml
    - .env
    - .init-db.sh (Not required if custom postgres is used)

Edit .env file and update the required details
- POSTGRES_USER and POSTGRES_PASSWORD is as root credentials to setup Postgres with docker
- POSTGRES_NON_ROOT_USER and POSTGRES_NON_ROOT_PASSWORD is used to create a user with limited access for application access
- APP_DOMAIN to the domain / IP Joindesk is hosted at
- JD_EMAIL set to true if email notification is required and provide required other email server details

### Docker build and up
Build the docker images
```bash
    docker-compose build
```
Run the application
```bash
    docker-compose up
```

The application should be accessible at port 80 and 443

### Use Custom Postgres
Also choose to use a postgres server hosted elsewhere
- Replace POSTGRES_HOST and POSTGRES_PORT

Either use below command
```
    docker-compose -f docker-compose-custom-db.yaml up
```

or 
Replace content of docker-compose.yaml with content of docker-compose-custom-db.yaml and run below command
```bash
    docker-compose up
```

### Update nginx config / default SSL
The default nginx.conf and ssl certificates (jd.cert / jd.key) is available under the Frontend directory

If required customize the content and mount them via docker-compose volumes
Example
```
volumes:
    - nginx.conf:/etc/nginx/nginx.conf
    - ssl.cert:/etc/nginx/ssl.cert
    - ssl.key:/etc/nginx/ssl.key
```

### Limit exposed ports
By default both port 80 and 443 are exposed

remove port 80 if required from docker-compose.yaml

!> Its always recommended to use **https**
