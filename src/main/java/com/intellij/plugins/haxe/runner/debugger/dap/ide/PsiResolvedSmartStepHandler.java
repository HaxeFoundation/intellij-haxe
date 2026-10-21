package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerSupportUtils;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XSourcePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * {@code custom/stepIntoFunction} request with (className, functionName);
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
class PsiResolvedSmartStepHandler extends DapSmartStepHandlerBase<PsiResolvedSmartStepHandler.Variant> {

  PsiResolvedSmartStepHandler(DapDebugProcess process) {
    super(process);
  }

  @Override
  public @NotNull List<Variant> computeSmartStepVariants(@NotNull XSourcePosition position) {
    return ReadAction.nonBlocking(() -> resolveVariants(position)).executeSynchronously();
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
                         ? HaxeQnameResolveUtil.runtimeClassName(declaringClass.haxeClass) : null;
      if (className == null || className.isEmpty()) {
        continue; // closures/local functions: no class-function name to break on
      }
      int occurrence = invocations.merge(className + "#" + model.getName(), 1, Integer::sum);
      variants.add(new Variant(className, model.getName(), occurrence, name.getTextRange()));
    }
    return variants;
  }

  @Override
  public void startStepInto(@NotNull Variant variant) {
    process.stepIntoFunction(variant.className, variant.functionName, variant.occurrence);
  }

  static final class Variant extends DapSmartStepHandlerBase.Variant {
    private final String className;
    private final String functionName;
    // which invocation of this callee on the line (1-based): the server's
    // entry breakpoint hits the FIRST invocation, so choosing a later one
    // (cfg.test1(1)...test1(2)) must tell it how many entries to skip
    private final int occurrence;

    Variant(String className, String functionName, int occurrence, @Nullable TextRange highlightRange) {
      super(highlightRange);
      this.className = className;
      this.functionName = functionName;
      this.occurrence = occurrence;
    }

    @Override
    public String getText() {
      // short label: "Target.combine", not the full dotted package path
      String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
      return simpleClassName + "." + functionName;
    }
  }
}
