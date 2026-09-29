# Why is this folder called `j2ee`?

The name is the IntelliJ Platform's, not ours — nothing here has anything to
do with Java EE.

`FileTemplateManager` only loads templates from its five fixed folders
(`fileTemplates`, `internal`, `code`, `includes`, `j2ee`); a custom folder
name would simply not be scanned. Of those, `j2ee` is the designated
location for plugin template GROUPS — the storage behind Settings | Editor |
File and Code Templates | **Other** — and the platform docs state the name
is "historical and not specifically tied to J2EE technology":
https://plugins.jetbrains.com/docs/intellij/providing-file-templates.html#other

The `Haxe Project *.ft` templates here are the files the "Haxe Template"
project generator scaffolds (see `v2/wizard/`), surfaced as the
"Haxe project" group in the Other tab by `HaxeProjectTemplatesFactory`,
which is registered on the `com.intellij.fileTemplateGroup` extension
point. They use plain `${VAR}` substitution (no Velocity — see
`HaxeTemplateFiles.render`), so `#` lines in hxml templates stay literal.
