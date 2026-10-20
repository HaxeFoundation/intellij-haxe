package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import java.util.List;
import javax.swing.Icon;

/**
 * What both smart-step flavours ({@link AdapterTargetsSmartStepHandler},
 * {@link PsiResolvedSmartStepHandler}) share: the async entry point the
 * platform drives (the variants are computed on the DAP request thread, never
 * on the EDT), the plain Step Into (F7) showing the same chooser, one popup
 * title, and the variants' presentation (the method icon and the highlight of
 * the call's name on the stopped line). A flavour computes its variants and
 * performs the chosen step.
 */
abstract class DapSmartStepHandlerBase<V extends DapSmartStepHandlerBase.Variant> extends XSmartStepIntoHandler<V> {
  protected final DapDebugProcess process;

  protected DapSmartStepHandlerBase(DapDebugProcess process) {
    this.process = process;
  }

  @Override
  public @NotNull Promise<List<V>> computeSmartStepVariantsAsync(@NotNull XSourcePosition position) {
    AsyncPromise<List<V>> promise = new AsyncPromise<>();
    process.onRequestThread(() -> {
      try {
        promise.setResult(computeSmartStepVariants(position));
      } catch (Throwable t) {
        promise.setError(t);
      }
    }, () -> promise.setError("Debug session is shutting down"));
    return promise;
  }

  // The PLAIN Step Into action (F7) consults this - the base implementation
  // returns a rejected promise, meaning "no variants, just step". Returning the
  // variants makes F7 behave like the Java debugger: with more than one call on
  // the line the same highlight/Tab chooser appears; with zero or one the
  // platform performs an ordinary step into.
  @Override
  public @NotNull Promise<List<V>> computeStepIntoVariants(@NotNull XSourcePosition position) {
    return computeSmartStepVariantsAsync(position);
  }

  // The base implementation throws AbstractMethodError, and the frontend/backend
  // debugger split calls this eagerly while creating the session DTO - an
  // unimplemented title breaks session initialization, not just the popup.
  @Override
  public String getPopupTitle() {
    return HaxeDebuggerBundle.message("dap.debugger.smart.step.into.title");
  }

  @Override
  public void startStepInto(@NotNull V variant, XSuspendContext context) {
    startStepInto(variant);
  }

  /** A call on the stopped line the user may enter, with the range of its name for the editor highlight (null: not found in the PSI). */
  abstract static class Variant extends XSmartStepIntoVariant {
    private final @Nullable TextRange highlightRange;

    Variant(@Nullable TextRange highlightRange) {
      this.highlightRange = highlightRange;
    }

    @Override
    public Icon getIcon() {
      return AllIcons.Nodes.Method;
    }

    @Override
    public @Nullable TextRange getHighlightRange() {
      return highlightRange;
    }
  }
}
