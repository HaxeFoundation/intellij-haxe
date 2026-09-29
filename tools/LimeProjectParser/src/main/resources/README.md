# Why Haxe source sits in `resources`

`HxpRunner.hx` is **not** compiled into the LimeProjectParser jar. It is
embedded as a resource (`-resource` in `build.hxml`/`test.hxml`) and compiled
later — by the **user's** Haxe toolchain, at `.hxp` evaluation time:

1. `HxpEvaluator` extracts it (`haxe.Resource.getString("HxpRunner.hx")`) into
   a temp directory next to a copy of the user's `.hxp` script.
2. It then runs the user's compiler:
   `haxe <ScriptClass> -lib lime -lib hxp -cp <tempDir> --run HxpRunner …` —
   HxpRunner instantiates the script class inside the user's lime/hxp library
   context and prints the evaluated project as JSON on stdout.

It cannot live in `src/main/haxe`: everything there is compiled into the tool
by the pinned toolchain, while HxpRunner must compile against whatever
lime/hxp versions the user's project has installed. To this artifact it is
data — a template shipped for someone else's compiler. That also means it
follows the user-compiled language-level floor, not the tool's own Haxe level.

The gradle tasks list this directory as an input; the `-resource` embedding is
otherwise invisible to Gradle and an edit here would leave the jar stale.
