@echo off
setlocal
set JAVA_HOME=C:\Users\npach\.jdks\openjdk-25.0.1
cd /d C:\Users\npach\IdeaProjects\sicc\sicc-api
call mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
endlocal

