package com.intellij.plugins.haxe.display.protocol.server;

import java.util.List;

/**
 * The part of a {@code server/module} answer the type catalog uses.
 * {@code sign} is a signature that changes whenever the module is retyped.
 * {@code types} lists the qualified names of the types the module declares,
 * in the form {@code JsonTypeRef.qualifiedNameOf} produces.
 * {@code dependencies} lists the paths of the modules it depends on.
 */
public record ModuleInfo(String sign,
                         List<String> types,
                         List<String> dependencies) {
}
