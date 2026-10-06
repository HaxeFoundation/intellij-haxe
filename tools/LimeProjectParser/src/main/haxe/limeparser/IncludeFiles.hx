package limeparser;

import haxe.io.Path;
import limeparser.ProjectXmlEvaluator.IncludeFile;
import sys.FileSystem;
import sys.io.File;

/**
	lime's include-file lookup: a directory stands for its include.lime,
	include.nmml or include.xml, whichever exists first in that order. The
	same order serves <include path> references (ProjectXMLParser) and
	library roots (HXProject.fromPath).
**/
class IncludeFiles {
	static final NAMES = ["include.lime", "include.nmml", "include.xml"];

	/**
		The include file's name inside a directory, or null when it ships none.
		TODO: include.hxp, which lime also accepts for a library, is a script and is not evaluated.
	**/
	public static function nameIn(directory:String):Null<String> {
		return Lambda.find(NAMES, name -> FileSystem.exists(Path.join([directory, name])));
	}

	/** The content of the directory's include file, or null when it ships none. **/
	public static function contentIn(directory:String):Null<String> {
		var name = nameIn(directory);
		return name == null ? null : File.getContent(Path.join([directory, name]));
	}

	/**
		Resolves an <include> reference against the project directory: a file
		as it is, a directory to its include file. The returned path keeps the
		reference's form (a relative reference stays relative), so the
		including project can rebase paths against the file's folder.
	**/
	public static function resolve(projectDirectory:String, reference:String):Null<IncludeFile> {
		var location = Path.isAbsolute(reference) ? reference : Path.join([projectDirectory, reference]);
		if (!FileSystem.exists(location)) return null;
		var path = reference;
		if (FileSystem.isDirectory(location)) {
			var name = nameIn(location);
			if (name == null) return null;
			path = Path.join([reference, name]);
			location = Path.join([location, name]);
		}
		return {path: path, content: File.getContent(location)};
	}
}
