This project is a working demonstration of how to set up a project to use the 
hxcpp-debugger with an OpenFL target on Windows.  It was set up following the 
instructions at http://intellij-haxe.org/debugging.  Note that version 2018.1
of IntelliJ IDEA was used.  It may not work with earlier versions.

Requirements:
1) hxcpp haxelib version 3,4,188 installed.  (No particular reason for this 
version.  You simply have to update the dependencies on the module if you 
decide to use a different one.)

2) hxcpp-debugger protocol_v1.1 branch from a git repo.

To use:

You can use this directory in place, but we recommend copying the entire 
directory to another source tree so that you can experiment without getting 
git confused.

Load the project up in a  


Contributing changes:

If you find errors in this project, you may file bugs to the git repo at
https://github.com/HaxeFoundation/intellij-haxe/issues.

If you would like to update this project for other users, please edit it and 
submit a PR will *all* of the .idea files, but *NONE* of Export/... files
(build artifacts).

Note: If you do try to check in any changes, many of the files 
in this directory will be ignored due to the rules in .gitignore in the root
directory of this repo.  You must use `git add -f ExampleProjects/HxcppDebugging/*`
to override.