@Echo Off
if "%1"=="" (
echo 'This script must be called with the version of IDEA to build'
echo 'example: build.bat 13.1.6'
echo 'example: build.bat 2017.2.4'
Exit /B 1
)

::call the build script along with the path to a code package
::#specific to the intellij version which we build against
call ant -f prepare.xml -Dversion="%1"
call ant -f build.xml -Dversion="%1"

::rm -rf idea-IU
