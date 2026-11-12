# Full project package

CBDVentures_Full_Project_With_Documents_Menu_Update.zip contains the complete CBDVentures project folder and its documents, with the current photo-first menu directory update and the NxG logo integrated.

## First run after unzipping

Open CBDVentures_Final_Project_Updated, the folder containing pom.xml and mvnw.cmd.

    bash tools/setup-jdk.sh
    ./mvnw spring-boot:run

The application opens at http://localhost:8080. Java 25 is required; the Maven wrapper downloads Maven on its first run. On Windows, use mvnw.cmd spring-boot:run from Command Prompt or PowerShell.

## Included

- Complete Java/Spring source, frontend, styles, assets and Maven wrapper.
- The shared 24-place student directory, with 39 menu entries (37 priced and 2 marked “Check menu”).
- Project documentation, API and database references, reports, SQL material, panel questions, screenshots and setup notes.
- The supporting map files and the black-and-white NxG monogram used by the app header.

The ZIP omits .git, generated target/ output, local database files and IDE-specific settings. To update an existing GitHub Desktop checkout, copy the extracted project contents into the folder containing pom.xml, replace matching files, and keep the existing .git folder. Copying files does not push them to GitHub or deploy the app.
