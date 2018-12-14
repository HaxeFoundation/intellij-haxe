This project is a working demonstration of how to set up a project to use Heaps
library with a HashLink (HL) target on Windows.  Note that version 2018.1
of IntelliJ IDEA was used.  It may not work with earlier versions.

_Note that debugging inside of IntelliJ for HashLink targets is not yet implemented._

**Requirements:**
- Haxe 4 (latest preview), including correct environment variables set up. 
- HashLink
- Heaps haxelib

**To use:**

You can use this directory in place, but we recommend copying the entire 
directory to another source tree so that you can experiment without getting 
git confused.

You will need to visit the Project Structure dialogs (File->Project Structure...)
and change the paths to match your installation.  This includes:
- SDK paths, including classpath, and source path, which should both have
only one entry and it must point
to the Std library directory (e.g. `D:\tools\haxe\std`).
- Library path for Heaps.
- Module path for the build.xml file.

Additionally, you will have to modify the Run configuration 
(Run->Edit Configurations...), enabling both check boxes, and changing the
run file to point to the built application in the Export directory. 
(e.g. `C:\sandbox\HeapsExample\Export\hl\bin\MyApp.hl`)  
The alternative executable to `hl` or a complete path to `hl.exe` if you don't
have it in your path.
 
Lastly, depending upon your preference for DirectX or SDL, (un)comment the
appropriate lines in the `build.xml` file in this project.


**Contributing changes:**

If you find errors in this project, you may file bugs to the git repo at
https://github.com/HaxeFoundation/intellij-haxe/issues.

If you would like to update this project for other users, please edit it and 
submit a PR will *all* of the .idea files, but *NONE* of Export/... files
(build artifacts).

Note: If you do try to check in any changes, many of the files 
in this directory will be ignored due to the rules in .gitignore in the root
directory of this repo.  You must use `git add -f ExampleProjects/HxcppDebugging/*`
to override.