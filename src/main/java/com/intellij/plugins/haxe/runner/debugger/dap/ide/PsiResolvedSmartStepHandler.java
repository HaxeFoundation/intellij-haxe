package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerSupportUtils;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

/**
 * Smart step into for DAP backends with IDE-resolved targets: on a line
 * with several calls (chained
 * {@code a().b()} or nested {@code a(b())}), lists them so the user picks
 * which one to enter. Both the dedicated action (Shift+F7) and the plain Step
 * Into (F7, via {@link #computeStepIntoVariants}) show the chooser; F7 steps
 * plainly when the line has at most one call.
 *
 * One of two smart-step flavours: here the variants are computed ENTIRELY
 * from the Haxe PSI — the in-debuggee server has no line→calls knowledge
 * (there is no bytecode to mine on hxcpp) — by resolving each call on the
 * stopped line to its declaring class. Choosing one sends the custom
 * {@code intellij/stepIntoFunction} request with (className, functionName);
 * the server races a temporary entry breakpoint against a step-over, so a
 * variant whose call never executes degrades safely to a step over.
 * Backends whose ADAPTER can report the calls itself use
 * {@link AdapterTargetsSmartStepHandler} instead — adapter-reported targets
 * are preferred wherever the DAP {@code stepInTargets} capability exists.
 *
 * Filtered out: calls that do not resolve to a class method (closures, local
 * functions — no runtime class/function name exists for the entry breakpoint
 * to match), {@code inline} methods (no runtime function at all) and externs
 * (no instrumentation). Virtual dispatch caveat: the breakpoint is planted on
 * the DECLARED class, so entering an override called through a base-typed
 * reference lands as a step over instead.
 */
class PsiResolvedSmartStepHandler extends XSmartStepIntoHandler<PsiResolvedSmartStepHandler.Variant> {
  private final DapDebugProcess process;

  PsiResolvedSmartStepHandler(DapDebugProcess process) {
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

  @Override
  public @NotNull List<Variant> computeSmartStepVariants(@NotNull XSourcePosition position) {
    return ReadAction.nonBlocking(() -> resolveVariants(position)).executeSynchronously();
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

  // Every call on the stopped line whose callee resolves to a steppable class
  // method, in EXECUTION order (matching the HashLink chooser and the order a
  // plain step would reach them), with the call name's text range for
  // highlighting.
  private List<Variant> resolveVariants(XSourcePosition position) {
    List<Variant> variants = new ArrayList<>();
    Project project = process.getSession().getProject();
    // per-callee invocation counter: the calls come in EXECUTION order, so the
    // Nth same-callee call on the line is its Nth runtime invocation
    Map<String, Integer> invocations = new HashMap<>();
    for (HaxeCallExpression call : HaxeDebuggerSupportUtils.callExpressionsOnLine(project, position)) {
      if (!(call.getExpression() instanceof HaxeReference reference)) {
        continue;
      }
      PsiElement name = reference.getReferenceNameElement();
      if (name == null || !(reference.resolve() instanceof HaxeMethod method)) {
        continue;
      }
      HaxeMethodModel model = method.getModel();
      if (model == null || model.isInline() || model.isExtern()) {
        continue; // no runtime function to enter
      }
      HaxeClassModel declaringClass = model.getDeclaringClass();
      // runtime name, not PSI's getQualifiedName(): a mismatched name means
      // the entry breakpoint never fires and every choice degrades to step over
      String className = declaringClass != null
                         ? HaxeDebuggerSupportUtils.runtimeClassName(declaringClass.haxeClass) : null;
      if (className == null || className.isEmpty()) {
        continue; // closures/local functions: no class-function name to break on
      }
      int occurrence = invocations.merge(className + "#" + model.getName(), 1, Integer::sum);
      variants.add(new Variant(className, model.getName(), occurrence, name.getTextRange()));
    }
    return variants;
  }

  // The base implementation throws AbstractMethodError, and the frontend/backend
  // debugger split calls this eagerly while creating the session DTO — an
  // unimplemented title breaks session initialization, not just the popup.
  @Override
  public String getPopupTitle() {
    return HaxeDebuggerBundle.message("hxcpp.debugger.smart.step.into.title");
  }

  @Override
  public void startStepInto(@NotNull Variant variant) {
    process.stepIntoFunction(variant.className, variant.functionName, variant.occurrence);
  }

  @Override
  public void startStepInto(@NotNull Variant variant, XSuspendContext context) {
    startStepInto(variant);
  }

  static final class Variant extends XSmartStepIntoVariant {
    private final String className;
    private final String functionName;
    // which invocation of this callee on the line (1-based): the server's
    // entry breakpoint hits the FIRST invocation, so choosing a later one
    // (cfg.test1(1)...test1(2)) must tell it how many entries to skip
    private final int occurrence;
    private final @Nullable TextRange highlightRange;

    Variant(String className, String functionName, int occurrence, @Nullable TextRange highlightRange) {
      this.className = className;
      this.functionName = functionName;
      this.occurrence = occurrence;
      this.highlightRange = highlightRange;
    }

    @Override
    public String getText() {
      // short label: "Target.combine", not the full dotted package path
      String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
      return simpleClassName + "." + functionName;
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
