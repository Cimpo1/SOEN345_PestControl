# Architecture
The architecture of the project consists of a layered MVC structure with the following components:
- **Backend**: The backend is a Spring Boot application that handles the business logic and data management. It exposes RESTful APIs for the frontend to interact with.
- **Frontend**: The frontend is an Android application. It provides a user interface for users to interact with the application. The frontend communicates with the backend through the RESTful APIs.

> Both the backend and frontend projects are deployed on a linux server to ensure accessibility and scalability.

## Backend
The backend is a REST API built using Spring Boot that does not render any views. It is responsible for handling all the business logic and data management. The backend interacts with a database to store and retrieve data as needed. The backend is designed to be scalable and can handle multiple requests simultaneously. It is deployed to the cloud. It uses a layered architecture with the following layers:
- **Controller Layer**: This layer is responsible for handling incoming HTTP requests and sending responses back to the client. It acts as an interface between the frontend and the backend.
- **Service Layer**: This layer contains the business logic of the application. It processes the data received from the controller layer and interacts with the data access layer to perform CRUD operations on the database.
- **Domain Layer**: This layer contains the domain models that represent the data structures used in the application. It defines the entities and their relationships.
- **Infrastructure Layer**: This layer is responsible for interacting with external systems.

## Frontend
The frontend is a React Native mobile application built with **Expo** that provides users with an interface to browse, create, and manage events. It uses a component-based architecture with the following key elements:
- **Framework**: React Native with Expo for cross-platform iOS and Android development.
- **Navigation**: Tab-based and stack-based navigation with separate flows for general users (Home, Events, My Events) and administrators (Admin Events).
- **State Management**: Context API (AuthContext) for managing user authentication and session state.
- **API Integration**: Service layer (authApi, eventsApi) that handles communication with the backend REST APIs.

## Communication
Done via REST API, HTTP and JSON.
