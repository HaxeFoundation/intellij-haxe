package com.intellij.plugins.haxe.display.protocol;

import java.util.List;

/**
 * A {@code haxe.display.JsonModuleTypes.JsonTypePath}: where a type is
 * declared. {@code importStatus} is set on a completion item's type only.
 */
public record JsonTypePath(List<String> pack, String moduleName, String typeName, ImportStatus importStatus) {

  /** The path of a wire entry that names none. */
  public static final JsonTypePath NONE = new JsonTypePath(List.of(), "", "", null);

  /** The package plus the type name, as Haxe prints a type ({@code haxe.ds.StringMap}, {@code Void}); null without a name. */
  public String dotPath() {
    if (typeName.isEmpty()) return null;
    return packagePrefix() + typeName;
  }

  /**
   * The qualified name the plugin's class-name indexes know the type by. A
   * module's main type is {@code pack.Module}, and any other type the module
   * declares is {@code pack.Module.SubType}. For example, {@code Void} is
   * declared in {@code StdTypes}, so its name is {@code StdTypes.Void}. A
   * module-private type is named like any other, although the compiler files
   * it under an extra package segment. Empty when the type name is missing.
   */
  public String qualifiedName() {
    if (typeName.isEmpty()) return "";
    String module = moduleName.isEmpty() ? typeName : moduleName;
    String moduleSegment = module.equals(typeName) ? "" : module + ".";
    return withoutPrivateSegment(packagePrefix(), module) + moduleSegment + typeName;
  }

  // the compiler files a module-private type under an extra last package segment, "_" plus the
  // module name (hxparse._ParserBuilderImpl.ParserCase); no real package looks like that, as a
  // package name must start lowercase after its underscores and a module name never does
  private static String withoutPrivateSegment(String packagePrefix, String moduleName) {
    String segment = "_" + moduleName + ".";
    boolean marked = packagePrefix.equals(segment) || packagePrefix.endsWith("." + segment);
    return marked ? packagePrefix.substring(0, packagePrefix.length() - segment.length()) : packagePrefix;
  }

  /** The package segments, each followed by a dot. */
  private String packagePrefix() {
    return pack.isEmpty() ? "" : String.join(".", pack) + ".";
  }
}
