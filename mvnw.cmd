@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM ... (standard mvnw script)
@REM ----------------------------------------------------------------------------

set MAVEN_DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip

if not exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin" (
    echo Descargando Maven 3.9.9...
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_DOWNLOAD_URL%' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin'"
)
set MVN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn
%MVN_CMD% %*
