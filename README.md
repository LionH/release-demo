# Spring Boot HelloWorld Showcase

This repository demonstrates a basic Spring Boot HelloWorld application with automated CI/CD using GitHub Actions. The workflow includes building, versioning, publishing to Maven Central, and deploying to a free hosting solution (e.g., Render).

## Features
- Automated build and test with Gradle.
- Versioning using tags (e.g., `v1.0.0`).
- Publishing to Maven Central.
- Deployment to free hosting solutions with separate environments (dev and prod).

## Project Structure
```
.
├── .github/workflows/ci-cd.yml  # GitHub Actions workflow file
├── src/                         # Source code for the Spring Boot application
├── build.gradle                 # Gradle build file
├── settings.gradle              # Gradle settings file
└── README.md                    # Project documentation
```

## Prerequisites
- Java 21
- Gradle
- GitHub account
- Accounts for hosting and Maven Central (e.g., Render, Sonatype)

## Secrets Configuration

### Required Secrets
Set the following secrets in your GitHub repository:
- `MAVEN_USERNAME`: Maven Central username.
- `MAVEN_PASSWORD`: Maven Central password.
- `RENDER_API_KEY`: Render API key.
- `RENDER_SERVICE_ID`: Render service ID.

### Adding Secrets
1. Go to your repository on GitHub.
2. Navigate to **Settings > Secrets and variables > Actions**.
3. Add the required secrets under **Repository secrets** or **Environment secrets** as per your setup.

## Usage

### Setting Up
1. Clone this repository.
2. Modify the source code in `src/` to customize your HelloWorld app.
3. Commit and push changes to the main branch.

### Releasing
1. Create a new release by tagging the commit:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub Actions will automatically trigger the workflow.

### Deployment
The application will be deployed to Render. For dev and prod environments, configure secrets for each environment.

## Workflow Overview
The GitHub Actions workflow (`ci-cd.yml`) includes the following jobs:

### Build
- Checks out the code.
- Sets up JDK 21.
- Builds the project using Gradle.

### Publish
- Publishes the built artifact to Maven Central.

### Deploy
- Deploys the application to Render using the API key and service ID.

## License
This project is licensed under the MIT License. See `LICENSE` for details.


