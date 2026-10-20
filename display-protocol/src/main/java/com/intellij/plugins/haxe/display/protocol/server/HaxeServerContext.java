package com.intellij.plugins.haxe.display.protocol.server;

import java.util.Map;

/**
 * One cache context of the server ({@code server/contexts}). The
 * {@code signature} identifies the argument set the context was compiled
 * with. The {@code description} names the context's role, such as
 * {@value #TYPED_MODULES_DESCRIPTION}.
 */
public record HaxeServerContext(String description,
                                String signature,
                                String platform,
                                Map<String, String> defines) {

  /** The {@code description} of the context holding the typed program's modules. */
  public static final String TYPED_MODULES_DESCRIPTION = "after_init_macros";

  /**
   * Whether this context holds the typed program's modules. The server also
   * keeps a macro context, whose modules exist only for the macro interpreter.
   */
  public boolean holdsTypedModules() {
    return TYPED_MODULES_DESCRIPTION.equals(description);
  }
}
