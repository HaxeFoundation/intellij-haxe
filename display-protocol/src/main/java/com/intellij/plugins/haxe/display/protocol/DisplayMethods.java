package com.intellij.plugins.haxe.display.protocol;

/**
 * JSON-RPC method names of the haxe display protocol, as the std
 * {@code haxe.display} classes DisplayMethods, ServerMethods and Methods
 * define them. Support varies by compiler version, so check a method against
 * the list {@link #INITIALIZE} returns before using it.
 */
public final class DisplayMethods {

  public static final String INITIALIZE = "initialize";

  public static final String DIAGNOSTICS = "display/diagnostics";
  public static final String COMPLETION = "display/completion";
  public static final String COMPLETION_ITEM_RESOLVE = "display/completionItem/resolve";
  public static final String FIND_REFERENCES = "display/references";
  public static final String GOTO_DEFINITION = "display/definition";
  public static final String GOTO_IMPLEMENTATION = "display/implementation";
  public static final String GOTO_TYPE_DEFINITION = "display/typeDefinition";
  public static final String HOVER = "display/hover";
  public static final String DETERMINE_PACKAGE = "display/package";
  public static final String SIGNATURE_HELP = "display/signatureHelp";
  public static final String METADATA = "display/metadata";
  public static final String DEFINES = "display/defines";

  public static final String SERVER_READ_CLASS_PATHS = "server/readClassPaths";
  public static final String SERVER_CONFIGURE = "server/configure";
  public static final String SERVER_INVALIDATE = "server/invalidate";
  public static final String SERVER_CONTEXTS = "server/contexts";
  public static final String SERVER_MODULES = "server/modules";
  public static final String SERVER_MODULE = "server/module";
  public static final String SERVER_TYPE = "server/type";
  public static final String SERVER_FILES = "server/files";
  public static final String SERVER_MODULE_CREATED = "server/moduleCreated";
  public static final String SERVER_MEMORY = "server/memory";

  private DisplayMethods() {
  }
}
