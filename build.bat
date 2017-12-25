@Echo Off
if "%1"=="" (
echo 'This script must be called with the version of IDEA to build'
echo 'example: build.bat 13.1.6'
echo 'example: build.bat 2017.2.4'
Exit /B 1
)

gradlew.bat buildPlugin -PideaVersion="%1"
