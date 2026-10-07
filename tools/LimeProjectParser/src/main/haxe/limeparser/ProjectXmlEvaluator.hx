package limeparser;

import haxe.io.Path;

/**
	A haxelib as the resolver reports it.

	- `root` is the library folder: the one holding its haxelib.json.
	- `classpaths` belong to this one library.
	- `includeXml` is the content of the library's include file
	  (include.lime, include.nmml or include.xml), if it ships one; lime
	  merges it as a nested project.
	- `extraDefines` are the -D entries of the library's extraParams.hxml.
	- `extraArgs` are its other compiler arguments (--macro lines and the
	  like). They cannot be evaluated statically and are kept for reference
	  only.
**/
typedef ResolvedHaxelib = {
	name:String,
	version:String,
	root:String,
	classpaths:Array<String>,
	includeXml:Null<String>,
	extraDefines:Array<String>,
	extraArgs:Array<String>
}

/** An include file as found: its path (relative when referenced relatively) and its content. **/
typedef IncludeFile = {path:String, content:String}

/**
	The lookups the evaluator delegates, so tests can feed it inline content.
	Each returns null when it finds nothing.

	- `include`: an <include> reference - a file, or a directory standing for
	  its include file.
	- `haxelib`: a haxelib (name, version) with its transitive dependencies,
	  in dependency order.
	- `localHaxelib`: a library checked out at a folder (name, root), as
	  `<haxelib path>` declares it.
**/
typedef Resolvers = {
	include:String->Null<IncludeFile>,
	haxelib:(String, String) -> Null<Array<ResolvedHaxelib>>,
	localHaxelib:(String, String) -> Null<ResolvedHaxelib>
}

/**
	Evaluates a lime/openfl project.xml the way lime's own parser does, but
	collects only the build configuration the IDE needs: defines, haxedefs,
	haxelibs (with versions), source classpaths and the app's export layout.
	It honours if/unless attributes, <section> grouping, <include> files and
	${} variable substitution, and ignores window, asset and other elements.

	An `if` value is an OR ("||") of AND groups of space-separated tokens. A
	token passes when it is "true", a known define, a known environment
	variable or the current command; it fails when it is "false" or unknown.
	`unless` is evaluated the same way and excludes the element on a match.

	`${name}` resolves like lime's ProjectHelper.replaceVariable: a haxelib
	root (`${haxelib:openfl}`), a define, an environment variable, the project
	folder (`${projectDirectory}`), else a comparison (`${haxe_ver >= 4.3}`,
	compared as strings) yielding "true"/"false"; an unknown name stays as
	it is, so `if="${unknown}"` fails.
**/
class ProjectXmlEvaluator {
	// $${name}: an escaped reference - lime resolves the name and wraps the
	// result in ${} again, so it is resolved once more
	static final ESCAPED_VAR_REFERENCE = ~/\$\$\{(.*?)\}/;
	// ${name} reference, shortest match: a nested reference resolves over two
	// rounds (${${haxe_ver} < 4} first yields ${haxe_ver < 4})
	static final VAR_REFERENCE = ~/\$\{(.*?)\}/;
	// the comparison operators of a ${} expression, in lime's probe order
	static final COMPARISONS = ["==", "!=", "<=", "<", ">=", ">"];
	static final HAXELIB_PREFIX = "haxelib:";
	static final DEFINE_FLAG = "-D ";

	static final CONFIG_PREFIX = "config:";
	// <config> attributes that steer the parse instead of holding a value
	static final CONFIG_META_ATTRIBUTES = ["type", "if", "unless"];

	public final defines:Map<String, String> = [];
	public final haxedefs:Map<String, String> = [];
	public final haxelibs:Array<{name:String, version:String}> = [];
	public final sources:Array<String> = [];
	/** <config> values flattened to the dot keys lime's ConfigData reads (`air.output-directory`). **/
	public final config:Map<String, String> = [];
	// The <app> attributes: path is the export root (lime's default is
	// "bin"), file the executable name.
	public var appPath:String = "bin";
	public var appFile:String = "";

	final environment:Map<String, String>;
	final command:String;
	final projectDirectory:String;
	final resolvers:Resolvers;
	final visitedIncludes:Array<String> = [];
	// asset type -> the haxelib that handles it (<library handler="swf"
	// type="swf"/>, usually registered by a library's include.xml)
	final libraryHandlers:Map<String, String> = [];
	final declaredAssetTypes:Array<String> = [];
	// libraries declared with <haxelib path>, by name: lime's path overrides,
	// consulted before haxelib by ${haxelib:x} and <include haxelib>
	final localLibraries:Map<String, ResolvedHaxelib> = [];
	// relative paths in an included file resolve against that file's folder
	var pathBase:String = "";

	public function new(seedDefines:Map<String, String>, command:String, environment:Map<String, String>,
			projectDirectory:String, resolvers:Resolvers) {
		for (name => value in seedDefines) {
			defines.set(name, value);
		}
		this.command = command;
		this.environment = environment;
		this.projectDirectory = projectDirectory;
		this.resolvers = resolvers;
	}

	/** Resolvers with the given lookups; a missing one finds nothing. **/
	public static function resolversOf(?include:String->Null<IncludeFile>,
			?haxelib:(String, String) -> Null<Array<ResolvedHaxelib>>,
			?localHaxelib:(String, String) -> Null<ResolvedHaxelib>):Resolvers {
		return {
			include: include != null ? include : path -> null,
			haxelib: haxelib != null ? haxelib : (name, version) -> null,
			localHaxelib: localHaxelib != null ? localHaxelib : (name, root) -> null
		};
	}

	/** Splits a `name=value` define; a bare name gets an empty value. **/
	public static function splitDefine(text:String):{name:String, value:String} {
		var pair = text.split("=");
		return {name: pair[0], value: pair.slice(1).join("=")};
	}

	/** Lime accepts both <project> and legacy <xml> roots. **/
	public function parse(content:String):Void {
		var xml = try Xml.parse(content) catch (e:Dynamic) null;
		if (xml == null) return;
		for (element in xml.elements()) {
			parseElements(element, "");
		}
		resolveAssetHandlers();
	}

	/** The running tool's own version, which lime defines for the compiler as tools=<lime version>. **/
	public function defineToolsVersion():Void {
		var lime = library("lime");
		if (lime != null) {
			haxedefs.set("tools", lime.version);
		}
	}

	/**
		Adds the handler haxelib of every declared asset library type that has
		one, as lime does when it runs the handler to process the assets (the
		swf library for .swf assets, for example). The handler is usually
		registered by a library's include.xml, so this runs after the whole
		project is parsed.
	**/
	function resolveAssetHandlers():Void {
		for (type in declaredAssetTypes) {
			var handler = libraryHandlers.get(type);
			if (handler != null && !isRegistered(handler)) {
				resolveAndRegister(handler, "", false);
			}
		}
	}

	function parseElements(parent:Xml, section:String):Void {
		for (element in parent.elements()) {
			if (!isElementActive(element, section)) continue;

			switch (element.nodeName) {
				case "section": parseElements(element, "");
				case "include": parseInclude(element);
				case "set": parseSet(element);
				case "setenv": parseSetenv(element);
				case "define": assign(element.get("name"), attribute(element, "value"), true);
				case "unset": unassign(element.get("name"), false);
				case "undefine": unassign(element.get("name"), true);
				case "haxedef": parseHaxedef(element);
				case "haxeflag", "compilerflag": parseHaxeflag(element);
				case "haxelib": parseHaxelib(element);
				case "source", "classpath": parseSource(element);
				case "app": parseApp(element);
				case "library": parseLibrary(element);
				case "config": parseConfig(element, "");
				case name if (StringTools.startsWith(name, CONFIG_PREFIX)): parseConfig(element, name.substr(CONFIG_PREFIX.length));
				default:
					// window, meta, assets, icon and the like carry no build configuration
			}
		}
	}

	/**
		Collects a <config> element the way lime's ConfigData does, flattened
		to dot keys: `<config:air output-directory="x"/>` and
		`<config><air output-directory="x"/></config>` both yield
		`air.output-directory`. A `type` attribute names the bucket like the
		`config:` prefix does; attributes and text-only children are leaves,
		other children nest one level deeper.
	**/
	function parseConfig(element:Xml, bucket:String):Void {
		parseConfigBucket(element, element.exists("type") ? element.get("type") : bucket);
	}

	function parseConfigBucket(element:Xml, bucket:String):Void {
		for (name in element.attributes()) {
			if (!CONFIG_META_ATTRIBUTES.contains(name)) {
				config.set(configKey(bucket, name), attribute(element, name));
			}
		}
		for (child in element.elements()) {
			var key = configKey(bucket, child.nodeName);
			if (child.elements().hasNext() || child.attributes().hasNext()) {
				parseConfigBucket(child, key);
			} else if (child.firstChild() != null) {
				config.set(key, substitute(child.firstChild().nodeValue));
			}
		}
	}

	static function configKey(bucket:String, name:String):String {
		return bucket == "" ? name : bucket + "." + name;
	}

	/** <set> is a condition define and an environment variable; BUILD_DIR also moves the export root. **/
	function parseSet(element:Xml):Void {
		var name = element.get("name");
		var value = attribute(element, "value");
		if (name == "BUILD_DIR") appPath = value;
		assign(name, value, false);
	}

	/** <setenv> without a value sets "1", and its name is substituted, unlike <set>'s. **/
	function parseSetenv(element:Xml):Void {
		var value = element.exists("value") ? attribute(element, "value") : "1";
		assign(attribute(element, "name"), value, false);
	}

	/** Sets a define and an environment variable; <define> also sets a haxedef. **/
	function assign(name:String, value:String, alsoHaxedef:Bool):Void {
		defines.set(name, value);
		environment.set(name, value);
		if (alsoHaxedef) {
			haxedefs.set(name, value);
		}
	}

	/** The inverse of `assign`. **/
	function unassign(name:String, alsoHaxedef:Bool):Void {
		defines.remove(name);
		environment.remove(name);
		if (alsoHaxedef) {
			haxedefs.remove(name);
		}
	}

	/** <haxedef remove="x"/> drops only the haxedef; a condition define of the same name (set by <define>) stays. **/
	function parseHaxedef(element:Xml):Void {
		if (element.exists("remove")) {
			haxedefs.remove(attribute(element, "remove"));
			return;
		}
		haxedefs.set(attribute(element, "name"), attribute(element, "value"));
	}

	/**
		A raw compiler flag: the name, then " " + value, substituted once more
		as a whole. A `-D name[=value]` flag is a haxedef.
		TODO: other flags (--remap, --macro) are not reported.
	**/
	function parseHaxeflag(element:Xml):Void {
		var flag = attribute(element, "name");
		if (element.exists("value")) {
			flag += " " + attribute(element, "value");
		}
		flag = substitute(flag);
		if (!StringTools.startsWith(flag, DEFINE_FLAG)) return;
		var define = splitDefine(StringTools.trim(flag.substr(DEFINE_FLAG.length)));
		haxedefs.set(define.name, define.value);
	}

	/** The attribute with its variables substituted; empty when absent. **/
	function attribute(element:Xml, name:String):String {
		return substitute(element.get(name));
	}

	function parseSource(element:Xml):Void {
		var path = element.exists("path") ? element.get("path") : element.get("name");
		if (path != null) {
			sources.push(rebase(substitute(path)));
		}
	}

	function parseApp(element:Xml):Void {
		if (element.exists("path")) appPath = rebase(attribute(element, "path"));
		if (element.exists("file")) appFile = attribute(element, "file");
	}

	/** Either registers an asset handler (both handler and type given) or declares an asset library. **/
	function parseLibrary(element:Xml):Void {
		if (element.exists("handler") && element.exists("type")) {
			libraryHandlers.set(attribute(element, "type"), attribute(element, "handler"));
			return;
		}
		var type = assetTypeOf(element);
		if (type != "" && !declaredAssetTypes.contains(type)) {
			declaredAssetTypes.push(type);
		}
	}

	/** The declared type, else the library file's extension. **/
	function assetTypeOf(element:Xml):String {
		if (element.exists("type")) return attribute(element, "type");
		if (element.exists("path")) return Path.extension(attribute(element, "path")).toLowerCase();
		return "";
	}

	function parseHaxelib(element:Xml):Void {
		if (element.exists("repository")) {
			// TODO: <haxelib repository> (a project-local HAXELIB_PATH) is not honoured; lime registers no library for it either
			return;
		}
		var name = attribute(element, "name");
		if (name == "") return;
		var version = attribute(element, "version");
		if (element.exists("path")) {
			registerLocal(name, version, rebase(attribute(element, "path")));
			return;
		}
		resolveAndRegister(name, version, attribute(element, "optional") == "true");
	}

	/**
		Registers the haxelib and its transitive dependencies. Each library's
		include file merges as a nested project and can add more haxelibs,
		haxedefs and sources. A library haxelib cannot resolve still counts as
		declared, unless the project marks it optional.
	**/
	function resolveAndRegister(name:String, version:String, optional:Bool):Void {
		if (isRegistered(name)) return;

		var resolved = resolvers.haxelib(name, version);
		if (resolved == null) {
			if (!optional) registerHaxelib(name, version, version);
			return;
		}
		for (library in resolved) {
			if (isRegistered(library.name)) continue;
			// Only the version the project itself declares is a pin. A
			// dependency's resolved version comes from its checkout; listing it
			// as a pin would make a later `haxelib path name:version` pick that
			// release over the repository's current (git or dev) selection.
			var pinnedVersion = library.name == name ? version : "";
			registerHaxelib(library.name, pinnedVersion, library.version);
			mergeLibrary(library);
		}
	}

	/**
		<haxelib path>: a library checked out at a project folder, read
		without a haxelib lookup; its version comes from its haxelib.json
		unless the project pins one. A folder that does not exist still counts
		as declared.
		TODO: the IDE resolves every listed haxelib through `haxelib path`, so a local-path library shows as missing there.
	**/
	function registerLocal(name:String, version:String, root:String):Void {
		if (isRegistered(name)) return;

		var library = resolvers.localHaxelib(name, root);
		if (library == null) {
			registerHaxelib(name, version, version);
			return;
		}
		localLibraries.set(name, library);
		registerHaxelib(name, version, version != "" ? version : library.version);
		mergeLibrary(library);
	}

	/**
		Merges one library's classpaths, extraParams defines and include file.
		TODO: lime merges an include file's defines and haxedefs as NEW keys only (an include cannot override a value the including project already holds); this evaluator shares one map.
	**/
	function mergeLibrary(library:ResolvedHaxelib):Void {
		for (classpath in library.classpaths) {
			if (!sources.contains(classpath)) {
				sources.push(classpath);
			}
		}
		for (extraDefine in library.extraDefines) {
			var define = splitDefine(extraDefine);
			defines.set(define.name, define.value);
			haxedefs.set(define.name, define.value);
		}
		if (library.includeXml != null) {
			parseIncluded(library.includeXml, library.root, "");
		}
	}

	function isRegistered(name:String):Bool {
		return Lambda.exists(haxelibs, lib -> lib.name == name);
	}

	/**
		Lists the haxelib with the version the project pins (empty when none).
		Like lime, it also defines the library's name with its resolved
		version, which is why `if="openfl"` works below a
		`<haxelib name="openfl"/>` line.
	**/
	function registerHaxelib(name:String, pinnedVersion:String, resolvedVersion:String):Void {
		haxelibs.push({name: name, version: pinnedVersion});
		if (!defines.exists(name)) {
			defines.set(name, resolvedVersion);
		}
	}

	/** A library by name: a <haxelib path> declaration first, then haxelib. **/
	function library(name:String):Null<ResolvedHaxelib> {
		if (localLibraries.exists(name)) return localLibraries.get(name);
		var resolved = resolvers.haxelib(name, "");
		return resolved == null ? null : Lambda.find(resolved, candidate -> candidate.name == name);
	}

	/** Parses included content with its folder as the base for relative paths. **/
	function parseIncluded(content:String, folder:String, section:String):Void {
		var xml = try Xml.parse(content) catch (e:Dynamic) null;
		if (xml == null) return;
		var previousBase = pathBase;
		pathBase = folder;
		for (root in xml.elements()) {
			parseElements(root, section);
		}
		pathBase = previousBase;
	}

	function rebase(path:String):String {
		if (pathBase == "" || Path.isAbsolute(path)) return path;
		return Path.join([pathBase, path]);
	}

	/**
		<include haxelib="x"/> merges the library's include file without
		registering the library; <include path|name> loads a file (a directory
		stands for its include file) and, when the file sits in a folder, adds
		that folder as a source path.
	**/
	function parseInclude(element:Xml):Void {
		if (element.exists("haxelib")) {
			includeLibrary(attribute(element, "haxelib"));
			return;
		}
		var reference = element.exists("path") ? attribute(element, "path") : attribute(element, "name");
		if (reference == "") return;

		var included = resolvers.include(rebase(reference));
		if (included == null || visitedIncludes.contains(included.path)) return;
		visitedIncludes.push(included.path);

		var folder = Path.directory(included.path);
		if (folder != "" && !sources.contains(folder)) {
			sources.push(folder);
		}
		parseIncluded(included.content, folder, orEmpty(element.get("section")));
	}

	function includeLibrary(name:String):Void {
		var included = library(name);
		if (included != null && included.includeXml != null) {
			parseIncluded(included.includeXml, included.root, "");
		}
	}

	/**
		Whether the element takes effect: its if/unless conditions pass, and,
		inside an include that names a section, it is the <section> element
		with that id. Like lime, `if` splits its raw text on "||" and `unless`
		the substituted text.
	**/
	function isElementActive(element:Xml, section:String):Bool {
		var ifValue = element.get("if");
		if (ifValue != null && !matchesConditions(ifValue)) return false;
		var unlessValue = element.get("unless");
		if (unlessValue != null && matchesConditions(substitute(unlessValue))) return false;
		if (section == "") return true;
		return element.nodeName == "section" && element.exists("id") && attribute(element, "id") == section;
	}

	/** OR over "||" segments, each segment an AND over space-separated tokens. **/
	function matchesConditions(value:String):Bool {
		for (segment in value.split("||")) {
			var tokens = substitute(segment).split(" ").map(token -> StringTools.trim(substitute(token)));
			if (Lambda.foreach(tokens, tokenPasses)) {
				return true;
			}
		}
		return false;
	}

	function tokenPasses(token:String):Bool {
		if (token == "" || token == "true") return true;
		if (token == "false") return false;
		return defines.exists(token) || environment.exists(token) || token == command;
	}

	function substitute(value:Null<String>):String {
		if (value == null) return "";
		var unescaped = expand(value, ESCAPED_VAR_REFERENCE, "${", "}");
		return expand(unescaped, VAR_REFERENCE, "", "");
	}

	/** Replaces every `reference` match with its resolved variable wrapped in open/close, until none is left. **/
	function expand(text:String, reference:EReg, open:String, close:String):String {
		var result = text;
		while (reference.match(result)) {
			var resolved = open + variable(reference.matched(1)) + close;
			result = reference.matchedLeft() + resolved + reference.matchedRight();
		}
		return result;
	}

	/**
		lime's replaceVariable: a haxelib root, a define, an environment
		variable, the project folder, else a comparison on the whitespace-free
		text - the left side resolved recursively, both sides compared as
		strings - yielding "true"/"false", else the name itself.
	**/
	function variable(name:String):String {
		if (StringTools.startsWith(name, HAXELIB_PREFIX)) return libraryRoot(name.substr(HAXELIB_PREFIX.length));
		if (defines.exists(name)) return defines.get(name);
		if (environment.exists(name)) return environment.get(name);
		if (name == "projectDirectory") return projectDirectory;

		var expression = StringTools.replace(name, " ", "");
		for (comparison in COMPARISONS) {
			var index = expression.indexOf(comparison);
			if (index < 0) continue;
			var left = variable(expression.substr(0, index));
			var right = expression.substr(index + comparison.length);
			return Std.string(compare(comparison, left, right));
		}
		// TODO: dotted project field access (${app.file}) is not resolved; the name stays literal
		return name;
	}

	static function compare(comparison:String, left:String, right:String):Bool {
		return switch (comparison) {
			case "==": left == right;
			case "!=": left != right;
			case "<=": left <= right;
			case "<": left < right;
			case ">=": left >= right;
			default: left > right;
		}
	}

	/** `${haxelib:x}`: the library root with forward slashes; empty when the library is unknown. **/
	function libraryRoot(name:String):String {
		var resolved = library(name);
		return resolved == null ? "" : standardize(resolved.root);
	}

	/** lime's Path.standardize: forward slashes and no trailing slash. **/
	static function standardize(path:String):String {
		return Path.removeTrailingSlashes(StringTools.replace(path, "\\", "/"));
	}

	static function orEmpty(value:Null<String>):String {
		return value == null ? "" : value;
	}
}
