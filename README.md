# ST Project

# Initial Setup

## Setting up IntelliJ
Plugins:
- JavaScript
- TypeScript
- Prettier
- Tailwind CSS

## Starting the Development Environment

For the development of the application, a local development environment is required that includes both the backend and frontend toolchain.  
The project structure is divided into a Java and Spring Boot backend and a React frontend.

### Backend

The backend is based on Java and Spring Boot and requires:

- JDK 25
- Apache Maven
- An IDE such as IntelliJ IDEA

The project is imported as a Maven project into the IDE. Maven is responsible for dependency management and the build lifecycle.

#### Run Backend API

- run inside the project root: `mvn clean install`
- run `mvn -pl barbu-api spring-boot:run "-Dspring-boot.run.profiles=dev"`
- the Backend runs in: http://localhost:8080/

#### Run Backend Engine Tests
- run `mvn test`

### Frontend

The frontend is based on React and requires:

- Node.js
- npm

All required dependencies are installed via npm.  
The frontend can also be developed using IntelliJ IDEA or another suitable editor.

#### Run Frontend

- run inside `barbu-frontend`: `npm run dev`
- frontend URL: http://localhost:5173/

## Setup for multiple devices (multiplayer)

- inside barbu-frontend/
    - create .env file with `VITE_API_BASE_URL=IP-address:8080` (your IP address)
    - update package.json dev script to `npm install && vite --host`
- inside barbu-api/src/main/resources/ 
    - update application-dev.properties: add `http://IP-adress:5173/` to the cors.allowed-origins property
- run backend
- run frontend 

## Swagger

The API is documented with springdoc-openapi, which serves the docs at runtime.

- run the backend
- interactive UI: http://localhost:8080/swagger-ui/index.html

