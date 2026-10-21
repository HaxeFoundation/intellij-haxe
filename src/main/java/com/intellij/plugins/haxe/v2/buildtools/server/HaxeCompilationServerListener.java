package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.util.messages.Topic;

/**
 * Project message bus topic for compilation-server state changes: a server
 * started, stopped, died or moved to a new port. Delivered on the EDT.
 */
@FunctionalInterface
public interface HaxeCompilationServerListener {

  @Topic.ProjectLevel
  Topic<HaxeCompilationServerListener> TOPIC =
    new Topic<>("Haxe compilation server state", HaxeCompilationServerListener.class, Topic.BroadcastDirection.NONE);

  void serverStateChanged();
}
