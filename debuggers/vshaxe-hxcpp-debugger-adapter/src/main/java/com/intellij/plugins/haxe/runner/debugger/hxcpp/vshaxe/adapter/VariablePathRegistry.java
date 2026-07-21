package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * Maps live variablesReferences to the expression path of their container
 * and the stack frame they belong to.
 *
 * DAP's {@code setVariable} names a child of a variablesReference, but the
 * server's {@code setVariable} takes an expression string and works against
 * the server's CURRENT frame (selected via {@code switchFrame} — the method
 * has no frame parameter). So every time a reference passes through the
 * adapter we record how to spell its contents (a scope's children are bare
 * names, an object's are {@code container.name}, an array's are
 * {@code container[index]}) and which frame they live in.
 *
 * References are per-stop (the server clears its own on every stop), so the
 * adapter clears this registry whenever the debuggee resumes or stops — a
 * stale reference must fail, not alias another stop's values.
 */
class VariablePathRegistry {
  private final Map<Integer, String> containerExprByRef = new ConcurrentHashMap<>();
  private final Map<Integer, Integer> frameByRef = new ConcurrentHashMap<>();

  /** A scope of {@code frameId}: children are addressed by their bare name. */
  void registerScope(int reference, int frameId) {
    containerExprByRef.put(reference, "");
    frameByRef.put(reference, frameId);
  }

  /** A reference in {@code frameId} whose children are addressed relative to {@code expression}. */
  void registerExpression(int reference, String expression, @Nullable Integer frameId) {
    if (reference > 0) {
      containerExprByRef.put(reference, expression);
      if (frameId != null) {
        frameByRef.put(reference, frameId);
      }
    }
  }

  /** Registers an expandable child of a known container under its own path (same frame). */
  void registerChild(int parentReference, String childName, int childReference) {
    if (childReference > 0) {
      String expression = childExpression(parentReference, childName);
      if (expression != null) {
        registerExpression(childReference, expression, frameByRef.get(parentReference));
      }
    }
  }

  /**
   * Spells the expression for {@code name} inside the container behind
   * {@code reference}; null when the reference is unknown (stale).
   */
  @Nullable String childExpression(int reference, String name) {
    String container = containerExprByRef.get(reference);
    if (container == null) {
      return null;
    }
    if (container.isEmpty()) {
      return name;
    }
    if (isIndex(name)) {
      return container + "[" + name + "]";
    }
    return container + "." + name;
  }

  /** The frame the reference belongs to; null when unknown. */
  @Nullable Integer frameOf(int reference) {
    return frameByRef.get(reference);
  }

  void clear() {
    containerExprByRef.clear();
    frameByRef.clear();
  }

  private static boolean isIndex(String name) {
    if (name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      if (!Character.isDigit(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
