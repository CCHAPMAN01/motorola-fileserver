# Motorola File Server

A simple Spring Boot file server that exposes RESTful endpoints for **Upload**, **Download**, **Delete**, and **List** file operations.

---

## Prerequisites

- Java Development Kit (JDK) 17 or newer

---

## Running the Application

This project uses Gradle as the build tool. 
A Gradle Wrapper is included in the repository so to run the application, execute the following command from the terminal:

``./gradlew bootRun``

This will start the file server using port 8080 by default (port can be configured in application.yaml)

---

## Testing

To run the unit tests, execute the following command from the terminal:

`./gradlew test`

