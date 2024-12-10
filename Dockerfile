FROM maven:3.9.9-eclipse-temurin-21 AS build

COPY pom.xml pom.xml
RUN mvn dependency:go-offline

COPY src src
RUN mvn package

FROM eclipse-temurin:21-jdk

RUN mkdir schemas
RUN mkdir generated_files

RUN chmod a+rw /generated_files

COPY --from=build /target/json-doc-1.0.7-jar-with-dependencies.jar jsondoc.jar
