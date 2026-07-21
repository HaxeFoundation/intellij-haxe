package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerSupportUtils;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInTarget;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

/**
 * Smart step into for HashLink: on a line with several calls (chained
 * {@code a().b()} or nested {@code a(b())}), lists them so the user picks
 * which one to enter. Both the dedicated action (Shift+F7) and the plain Step
 * Into (F7, via {@link #computeStepIntoVariants}) show the chooser; F7 steps
 * plainly when the line has at most one call. The variants come from the adapter's DAP
 * stepInTargets request — the calls on the stopped line, in execution order —
 * and choosing one sends stepIn with that targetId: the adapter plants a temp
 * breakpoint only at the chosen callee's entry, like a run-to-cursor aimed at
 * the method start. An empty variant list makes the platform fall back to a
 * plain step into.
 *
 * Each variant also carries the text range of its call's name identifier on
 * the stopped line (matched through the Haxe PSI by simple name), which is
 * what makes the platform highlight the calls in the editor and let the user
 * Tab between them, like the Java debugger. A target whose call can't be
 * found in the PSI still works — it just isn't highlighted.
 */
class HashLinkSmartStepIntoHandler extends XSmartStepIntoHandler<HashLinkSmartStepIntoHandler.Variant> {
  private final DapDebugProcess process;

  HashLinkSmartStepIntoHandler(DapDebugProcess process) {
    this.process = process;
  }

  // The platform drives this async entry point; compute on the DAP request
  // thread — the sync computeSmartStepVariants must never block the EDT.
  @Override
  public @NotNull Promise<List<Variant>> computeSmartStepVariantsAsync(@NotNull XSourcePosition position) {
    AsyncPromise<List<Variant>> promise = new AsyncPromise<>();
    process.onRequestThread(() -> {
      try {
        promise.setResult(computeSmartStepVariants(position));
      } catch (Throwable t) {
        promise.setError(t);
      }
    }, () -> promise.setError("Debug session is shutting down"));
    return promise;
  }

  // The single implementation, reached only through the async entry points
  // above (on the request thread, where the blocking DAP round-trip belongs).
  @Override
  public @NotNull List<Variant> computeSmartStepVariants(@NotNull XSourcePosition position) {
    List<StepInTarget> targets = process.requestStepInTargets();
    if (targets.isEmpty()) {
      return List.of();
    }
    Project project = process.getSession().getProject();
    List<TextRange> ranges = ReadAction.nonBlocking(
      () -> matchCallRanges(targets, callNameElementsInExecutionOrder(project, position))).executeSynchronously();
    List<Variant> variants = new ArrayList<>(targets.size());
    for (int i = 0; i < targets.size(); i++) {
      variants.add(new Variant(targets.get(i), ranges.get(i)));
    }
    return variants;
  }

  // The PLAIN Step Into action (F7) consults this — the base implementation
  // returns a rejected promise, meaning "no variants, just step". Returning our
  // variants makes F7 behave like the Java debugger: with more than one call on
  // the line the same highlight/Tab chooser appears; with zero or one the
  // platform performs an ordinary step into.
  @Override
  public @NotNull Promise<List<Variant>> computeStepIntoVariants(@NotNull XSourcePosition position) {
    return computeSmartStepVariantsAsync(position);
  }

  // For each adapter target, the text range of the matching call's NAME
  // identifier on the stopped line, or null when no call with that simple name
  // is (left to) match. BOTH lists are in execution order, and the pairing
  // runs BACK-TO-FRONT: the adapter only reports the calls still AHEAD of the
  // current position — a suffix of the line's execution order — while the PSI
  // list covers the whole line. Aligning from the end pairs each target with
  // its own occurrence even when an already-executed call earlier on the line
  // shares the name (`cfg.test1(1)...test1(2)` after test1(1) ran), and it
  // keeps same-named calls from swapping (`a.reset(b.reset())`).
  // (package-private, static: exercised directly by tests)
  static List<TextRange> matchCallRanges(List<StepInTarget> targets, List<PsiElement> names) {
    TextRange[] result = new TextRange[targets.size()];
    List<PsiElement> remaining = new ArrayList<>(names);
    for (int t = targets.size() - 1; t >= 0; t--) {
      String label = targets.get(t).getLabel();
      String simpleName = label.substring(label.lastIndexOf('.') + 1);
      for (int i = remaining.size() - 1; i >= 0; i--) {
        if (remaining.get(i).getText().equals(simpleName)) {
          result[t] = remaining.get(i).getTextRange();
          remaining.remove(i); // consume, so a repeated callee highlights each occurrence once
          break;
        }
      }
    }
    return Arrays.asList(result);
  }

  // The name identifiers of the call expressions on the position's line, in
  // EXECUTION order — the order the adapter reports targets (bytecode order).
  // (package-private, static: exercised directly by tests)
  static List<PsiElement> callNameElementsInExecutionOrder(Project project, XSourcePosition position) {
    List<PsiElement> names = new ArrayList<>();
    for (HaxeCallExpression call : HaxeDebuggerSupportUtils.callExpressionsOnLine(project, position)) {
      PsiElement name = HaxeDebuggerSupportUtils.callNameElement(call);
      if (name != null) {
        names.add(name);
      }
    }
    return names;
  }

  // The base implementation throws AbstractMethodError, and the frontend/backend
  // debugger split calls this eagerly while creating the session DTO — an
  // unimplemented title breaks session initialization, not just the popup.
  @Override
  public String getPopupTitle() {
    return HaxeDebuggerBundle.message("hashlink.debugger.smart.step.into.title");
  }

  @Override
  public void startStepInto(@NotNull Variant variant) {
    process.stepIntoTarget(variant.target.getId());
  }

  @Override
  public void startStepInto(@NotNull Variant variant, XSuspendContext context) {
    startStepInto(variant);
  }

  static final class Variant extends XSmartStepIntoVariant {
    private final StepInTarget target;
    private final @Nullable TextRange highlightRange;

    Variant(StepInTarget target, @Nullable TextRange highlightRange) {
      this.target = target;
      this.highlightRange = highlightRange;
    }

    @Override
    public String getText() {
      return target.getLabel();
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
