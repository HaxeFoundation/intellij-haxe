package com.intellij.plugins.haxe.ide.documentation;

import java.util.List;

/** The haxedoc tags the plugin understands - rendering, highlighting and completion share this list. */
public final class HaxeDocTags {

  public static final String TAG_SINCE = "@since";
  public static final String TAG_SEE = "@see";
  public static final String TAG_PARAM = "@param";
  public static final String TAG_RETURN = "@return";
  public static final String TAG_EVENT = "@event";
  public static final String TAG_THROWS = "@throws";

  // TODO: decide whether @example is a supported haxedoc tag (rendering + completion)
  public static final List<String> ALL = List.of(
    TAG_SINCE,
    TAG_SEE,
    TAG_PARAM,
    TAG_RETURN,
    TAG_EVENT,
    TAG_THROWS
  );

  private HaxeDocTags() {
  }
}
