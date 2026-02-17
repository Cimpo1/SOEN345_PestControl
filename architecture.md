# Architecture
The architecture of the project consists of a layered MVC structure with the following components:
- **Backend**: The backend is a Spring Boot application that handles the business logic and data management. It exposes RESTful APIs for the frontend to interact with.
- **Frontend**: The frontend is an Android application. It provides a user interface for users to interact with the application. The frontend communicates with the backend through the RESTful APIs.

> Only the backend is deployed to the cloud.

## Backend
The backend is a REST API built using Spring Boot that does not render any views. It is responsible for handling all the business logic and data management. The backend interacts with a database to store and retrieve data as needed. The backend is designed to be scalable and can handle multiple requests simultaneously. It is deployed to the cloud. It uses a layered architecture with the following layers:
- **Controller Layer**: This layer is responsible for handling incoming HTTP requests and sending responses back to the client. It acts as an interface between the frontend and the backend.
- **Service Layer**: This layer contains the business logic of the application. It processes the data received from the controller layer and interacts with the data access layer to perform CRUD operations on the database.
- **Domain Layer**: This layer contains the domain models that represent the data structures used in the application. It defines the entities and their relationships.
- **Infrastructure Layer**: This layer is responsible for interacting with external systems such as databases, file storage, and other services.

### Planned Features
- **Authentication and Authorization**: Implementing user authentication and authorization to secure the application.
- **Event Management**: Adding features to manage events, including creating, updating, and deleting events.
- **Notification System**: Implementing a notification system to send confirmations via email or SMS when users book an event.
- **Concurrency Handling**: Implementing mechanisms to handle concurrent bookings and prevent overbooking of events.
- **Tests + CI/CD**: unit + integration + concurrency tests

## Frontend


## Communication
Done via REST API, HTTP and JSON.
