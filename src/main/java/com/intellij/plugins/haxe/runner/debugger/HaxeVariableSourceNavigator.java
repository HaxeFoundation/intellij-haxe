package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.runner.debugger.dap.EvaluationPath;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.concurrent.ExecutorService;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XNavigatable;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;

/**
 * "Jump to Source" for a Variables-view node: resolves the node's access-path
 * expression ("this.someObject.someArray[0].myVar", the same path that
 * pre-fills Evaluate Expression) through the ordinary Haxe resolver and
 * navigates to the resolved declaration — the field in its declaring class,
 * or a local/parameter's declaration for a frame root. Mirrors the Java
 * debugger's behavior.
 *
 * Mechanism: the path is parsed into a throwaway {@code
 * HaxeExpressionCodeFragment} whose context is the current frame's source
 * position (so locals and imports resolve exactly as they would in the
 * evaluate dialog), and the OUTERMOST reference — the full chain, whose
 * reference name is the selected member — is resolved. The fragment is
 * discarded afterwards; resolve-cache entries keyed on its elements die with
 * it. No path or no resolution simply means no navigation.
 *
 * Bare {@code this} is special-cased to the enclosing class (it names no
 * member to navigate to); {@code this.member…} paths resolve through the
 * fragment like any other chain — the resolver falls back to the fragment's
 * creation context when the enclosing-class parent walk dead-ends at the
 * fragment file. Every bail-out is debug-logged so a silent non-navigation
 * is diagnosable from idea.log.
 */
public final class HaxeVariableSourceNavigator {
  private static final Logger LOG = Logger.getInstance(HaxeVariableSourceNavigator.class);

  // computeSourcePosition is NOT only the explicit Jump to Source: the
  // frontend/backend-split debugger also routes every node's INLINE-values
  // lookup through it (FrontendXValue.computeInlineDebuggerData), so one
  // re-render of a populated Variables view fires dozens of navigations at
  // once. Unbounded, those tripped the platform's submission tracker ("Too
  // many non-blocking read actions submitted at once"). A small bounded
  // executor queues the resolves instead of stampeding them — still off the
  // EDT, still cancellable/expirable, and the platform's own per-call
  // navigation timeout keeps a queued laggard from being waited on forever.
  private static final ExecutorService RESOLVE_EXECUTOR =
    AppExecutorUtil.createBoundedApplicationPoolExecutor("HaxeVariableSourceNavigator", 2);

  private HaxeVariableSourceNavigator() {
  }

  /**
   * Asynchronous by contract: the platform invokes computeSourcePosition on
   * the EDT, so the PSI resolve runs in a non-blocking read action on the
   * bounded resolver executor (cancelled and retried around write actions)
   * and {@code navigatable} is completed from there — the platform bounds
   * the wait with its own navigation timeout. Only the frame OBJECT is
   * captured on the caller's thread (a plain state read, pinning the frame
   * the user clicked from); its source position resolves lazily through the
   * source resolver's index lookup — a slow operation that must also stay
   * off the EDT.
   */
  public static void navigate(@Nullable XDebugSession session, @Nullable String path,
                              @Nullable String containerTypeName, @Nullable String memberName,
                              XNavigatable navigatable) {
    if (session == null || path == null || path.isBlank()) {
      navigatable.setSourcePosition(null);
      return;
    }
    Project project = session.getProject();
    XStackFrame currentFrame = session.getCurrentStackFrame();

    ReadAction.nonBlocking(() -> {
        XSourcePosition frame = currentFrame != null ? currentFrame.getSourcePosition() : null;
        return resolvePosition(project, frame, path, containerTypeName, memberName);
      })
      // no parent disposable (a Project must not be one in plugin code):
      // expire on the conditions that make the navigation pointless
      .expireWhen(() -> project.isDisposed() || session.isStopped())
      .submit(RESOLVE_EXECUTOR)
      .onSuccess(target -> {
        if (target == null) {
          LOG.debug("jump-to-source: no target for path '" + path + "'");
        }
        navigatable.setSourcePosition(target);
      })
      // expired/cancelled/failed: complete with "no position" rather than
      // leaving the platform to wait out its navigation timeout
      .onError(error -> navigatable.setSourcePosition(null));
  }

  // package-private for tests
  static @Nullable XSourcePosition resolvePosition(Project project, @Nullable XSourcePosition frame, String path,
                                                   @Nullable String containerTypeName, @Nullable String memberName) {
    PsiElement context = frame != null
                         ? HaxeDebuggerSupportUtils.getContextElement(frame.getFile(), frame.getOffset(), project)
                         : null;
    if (context == null) {
      // without a frame context nothing (locals, imports) can resolve
      LOG.debug("jump-to-source: no context element (frame position " + frame + ")");
      return null;
    }

    if (path.equals("this")) {
      HaxeClass enclosing = PsiTreeUtil.getParentOfType(context, HaxeClass.class);
      LOG.debug("jump-to-source: bare this -> enclosing class " + (enclosing != null ? enclosing.getName() : null));
      return enclosing != null ? XDebuggerUtil.getInstance().createPositionByElement(enclosing.getNavigationElement()) : null;
    }

    XSourcePosition byResolve = resolveChain(project, context, path);
    if (byResolve != null) {
      return byResolve;
    }
    // The chain resolves through DECLARED types, but the debugger reports each
    // container's RUNTIME type — `var s:Shape = new Circle()` shows Circle's
    // members, and `s.radius` has no meaning on Shape. The node's direct
    // container's runtime type knows the member the declaration does not.
    return resolveOnRuntimeType(project, containerTypeName, memberName);
  }

  /**
   * Looks the member up on the container's runtime type, as reported by the
   * debug adapter: short or dotted name through the class-name index (dotted
   * names must match the candidate's qualified name), member lookup including
   * inherited ones. Null when the type or member cannot be found — primitives,
   * anonymous structures and VM-internal type names simply miss the index.
   */
  private static @Nullable XSourcePosition resolveOnRuntimeType(Project project,
                                                                @Nullable String typeName,
                                                                @Nullable String memberName) {
    if (typeName == null || memberName == null || !EvaluationPath.isIdentifier(memberName)) {
      return null;
    }
    String bare = typeName;
    int typeParams = bare.indexOf('<');
    if (typeParams >= 0) {
      bare = bare.substring(0, typeParams); // "Array<Circle>" -> "Array"
    }
    bare = bare.trim();
    String shortName = bare.substring(bare.lastIndexOf('.') + 1);
    boolean qualified = bare.indexOf('.') >= 0;
    if (shortName.isBlank()) {
      return null;
    }
    for (HaxeClass candidate : HaxeClassNameUnifiedIndex.getByNameFiltered(
      shortName, project, GlobalSearchScope.allScope(project))) {
      // compare RUNTIME-style names (package + bare name) — PSI's
      // getQualifiedName() inserts the module segment for ancillary classes
      // ("openfl.display.Preloader.DefaultPreloader") and would reject the
      // adapter-reported "openfl.display.DefaultPreloader"
      if (qualified && !bare.equals(HaxeDebuggerSupportUtils.runtimeClassName(candidate))) {
        continue;
      }
      HaxeBaseMemberModel member = candidate.getModel().getMember(memberName, null);
      if (member != null) {
        LOG.debug("jump-to-source: member '" + memberName + "' found on runtime type '" + bare + "'");
        return XDebuggerUtil.getInstance().createPositionByElement(member.getBasePsi().getNavigationElement());
      }
    }
    LOG.debug("jump-to-source: runtime type '" + typeName + "' / member '" + memberName + "' not found");
    return null;
  }

  private static @Nullable XSourcePosition resolveChain(Project project, PsiElement context, String expression) {
    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(project, expression, context, false);
    HaxeReference chain = outermostReference(fragment);
    if (chain == null) {
      LOG.debug("jump-to-source: no reference parsed from '" + expression + "'");
      return null;
    }
    PsiElement resolved = chain.resolve();
    if (resolved == null) {
      LOG.debug("jump-to-source: '" + chain.getText() + "' did not resolve");
      return null;
    }
    return XDebuggerUtil.getInstance().createPositionByElement(resolved.getNavigationElement());
  }

  // The outermost (widest) reference expression in the fragment — the full
  // access chain, whose reference name element is the LAST segment. findChildren
  // walks pre-order, so the first widest hit is the chain's top.
  private static @Nullable HaxeReference outermostReference(PsiFile fragment) {
    HaxeReference outermost = null;
    for (HaxeReference candidate : PsiTreeUtil.findChildrenOfType(fragment, HaxeReference.class)) {
      if (outermost == null || candidate.getTextLength() > outermost.getTextLength()) {
        outermost = candidate;
      }
    }
    return outermost;
  }
}
