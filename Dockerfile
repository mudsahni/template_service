# Use Ubuntu as the base image
FROM ubuntu:20.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Update and install minimal dependencies
RUN apt-get update && apt-get install -y \
    wget \
    ca-certificates \
    tar \
    && rm -rf /var/lib/apt/lists/*

# Install Amazon Corretto 17
RUN wget -O /tmp/corretto.tar.gz https://corretto.aws/downloads/latest/amazon-corretto-17-aarch64-linux-jdk.tar.gz \
    && mkdir -p /opt/java \
    && tar -xzf /tmp/corretto.tar.gz -C /opt/java --strip-components=1 \
    && rm /tmp/corretto.tar.gz

# Set JAVA_HOME
ENV JAVA_HOME=/opt/java
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install Python 3.9
RUN apt-get update && apt-get install -y \
    software-properties-common \
    && add-apt-repository ppa:deadsnakes/ppa \
    && apt-get update \
    && apt-get install -y python3.9 python3.9-venv python3-pip \
    && update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.9 1 \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory in the container
WORKDIR /app

# Create a new Python virtual environment
RUN python3 -m venv /opt/venv/paved-road-template-service

# Activate the virtual environment and install pynacl
RUN . /opt/venv/paved-road-template-service/bin/activate \
    && pip install --no-cache-dir pynacl \
    && deactivate

# Copy the application's jar to the container
COPY build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jdk-slim

# Install Python and necessary dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-venv \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory to /app
WORKDIR /app

# Copy the Gradle wrapper and configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy the application source code
COPY src src

# Give execution permission to the Gradle wrapper
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build

# Copy the built application to the Docker image
COPY build/libs/*.jar app.jar

# Create a Python virtual environment
RUN python3 -m venv /opt/venv

# Activate the virtual environment and install Python dependencies
# Here we assume you have a requirements.txt file
COPY requirements.txt .
RUN /opt/venv/bin/pip install -r requirements.txt

# Set the environment variables for Python and Java
ENV VIRTUAL_ENV=/opt/venv
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Expose port 8080 to the outside world
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
