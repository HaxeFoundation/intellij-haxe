package limeparser;

import haxe.io.Path;

/**
	A haxelib as `haxelib path` reports it.

	- `classpaths` belong to this one library.
	- `includeXml` is the content of the library's include.xml, if it ships
	  one; lime merges it as a nested project.
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
**/
class ProjectXmlEvaluator {
	/** An include resolver that finds no files. **/
	public static final NO_INCLUDES:String->Null<String> = path -> null;

	static final NO_HAXELIBS:(String, String) -> Null<Array<ResolvedHaxelib>> = (name, version) -> null;

	// ${name} variable reference; the name group excludes closing braces
	static final VAR_REFERENCE = ~/\$\{([^}]+)\}/;

	public final defines:Map<String, String> = [];
	public final haxedefs:Map<String, String> = [];
	public final haxelibs:Array<{name:String, version:String}> = [];
	public final sources:Array<String> = [];
	// The <app> attributes: path is the export root (lime's default is
	// "bin"), file the executable name.
	public var appPath:String = "bin";
	public var appFile:String = "";

	final environment:Map<String, String>;
	final command:String;
	/** Resolves an <include path="..."/> reference to file content, or null when unreadable. **/
	final includeResolver:String->Null<String>;
	/** Resolves a haxelib and its transitive dependencies in order, or null when unresolvable. **/
	final haxelibResolver:(String, String) -> Null<Array<ResolvedHaxelib>>;
	final visitedIncludes:Array<String> = [];
	// asset type -> the haxelib that handles it (<library handler="swf"
	// type="swf"/>, usually registered by a library's include.xml)
	final libraryHandlers:Map<String, String> = [];
	final declaredAssetTypes:Array<String> = [];
	// paths in a library's include.xml resolve against the library root, not the project
	var pathBase:String = "";

	public function new(seedDefines:Map<String, String>, command:String,
			environment:Map<String, String>, includeResolver:String->Null<String>,
			?haxelibResolver:(String, String) -> Null<Array<ResolvedHaxelib>>) {
		for (name => value in seedDefines) {
			defines.set(name, value);
		}
		this.command = command;
		this.environment = environment;
		this.includeResolver = includeResolver;
		this.haxelibResolver = haxelibResolver != null ? haxelibResolver : NO_HAXELIBS;
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
				resolveAndRegister(handler, "");
			}
		}
	}

	function parseElements(parent:Xml, section:String):Void {
		for (element in parent.elements()) {
			if (!isElementActive(element, section)) continue;

			switch (element.nodeName) {
				case "section": parseElements(element, "");
				case "include": parseInclude(element);
				case "set", "setenv": assign(element, false);
				case "define": assign(element, true);
				case "unset": unassign(element, false);
				case "undefine": unassign(element, true);
				case "haxedef": haxedefs.set(attribute(element, "name"), attribute(element, "value"));
				case "haxelib": parseHaxelib(element);
				case "source", "classpath": parseSource(element);
				case "app": parseApp(element);
				case "library": parseLibrary(element);
				default:
					// window, meta, assets, icon and the like carry no build configuration
			}
		}
	}

	/** <set>/<setenv> set a define and an environment variable; <define> also sets a haxedef. **/
	function assign(element:Xml, alsoHaxedef:Bool):Void {
		var name = element.get("name");
		var value = attribute(element, "value");
		defines.set(name, value);
		environment.set(name, value);
		if (alsoHaxedef) {
			haxedefs.set(name, value);
		}
	}

	/** The inverse of `assign`. **/
	function unassign(element:Xml, alsoHaxedef:Bool):Void {
		var name = element.get("name");
		defines.remove(name);
		environment.remove(name);
		if (alsoHaxedef) {
			haxedefs.remove(name);
		}
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
		if (element.exists("path")) appPath = attribute(element, "path");
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
		var name = attribute(element, "name");
		if (name == "") return;
		resolveAndRegister(name, attribute(element, "version"));
	}

	/**
		Registers the haxelib and its transitive dependencies. Each library's
		include.xml merges as a nested project and can add more haxelibs,
		haxedefs and sources.
	**/
	function resolveAndRegister(name:String, version:String):Void {
		if (isRegistered(name)) return;

		var resolved = haxelibResolver(name, version);
		if (resolved == null) {
			registerHaxelib(name, version, version);
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
				parseLibraryInclude(library.includeXml, library.root);
			}
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

	function parseLibraryInclude(content:String, libraryRoot:String):Void {
		var xml = try Xml.parse(content) catch (e:Dynamic) null;
		if (xml == null) return;
		var previousBase = pathBase;
		pathBase = libraryRoot;
		for (root in xml.elements()) {
			parseElements(root, "");
		}
		pathBase = previousBase;
	}

	function rebase(path:String):String {
		if (pathBase == "" || Path.isAbsolute(path)) return path;
		return Path.join([pathBase, path]);
	}

	function parseInclude(element:Xml):Void {
		var path = element.exists("path") ? element.get("path") : element.get("name");
		if (path == null) return;
		path = substitute(path);
		if (visitedIncludes.contains(path)) return;
		visitedIncludes.push(path);

		var content = includeResolver(path);
		if (content == null) return;
		var xml = try Xml.parse(content) catch (e:Dynamic) null;
		if (xml == null) return;
		var section = orEmpty(element.get("section"));
		for (root in xml.elements()) {
			parseElements(root, section);
		}
	}

	/**
		Whether the element takes effect: its if/unless conditions pass, and,
		inside an include that names a section, it is the <section> element
		with that id.
	**/
	function isElementActive(element:Xml, section:String):Bool {
		var ifValue = element.get("if");
		if (ifValue != null && !matchesConditions(ifValue)) return false;
		var unlessValue = element.get("unless");
		if (unlessValue != null && matchesConditions(unlessValue)) return false;
		if (section == "") return true;
		return element.nodeName == "section" && element.exists("id") && attribute(element, "id") == section;
	}

	/** OR over "||" segments, each segment an AND over space-separated tokens. **/
	function matchesConditions(value:String):Bool {
		for (segment in substitute(value).split("||")) {
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
		var result = value;
		while (VAR_REFERENCE.match(result)) {
			var replacement = variable(VAR_REFERENCE.matched(1));
			result = VAR_REFERENCE.matchedLeft() + replacement + VAR_REFERENCE.matchedRight();
		}
		return result;
	}

	/** A define first, then an environment variable; unknown names read as empty. **/
	function variable(name:String):String {
		if (defines.exists(name)) return defines.get(name);
		if (environment.exists(name)) return environment.get(name);
		return "";
	}

	static function orEmpty(value:Null<String>):String {
		return value == null ? "" : value;
	}
}
