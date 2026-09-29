package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeResolveResult;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import icons.HaxeIcons;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Adds macro-generated members from the compiler's post-macro blueprints to
 * identifier completion. The static contributors only see PSI declarations;
 * members that exist solely in generated code (fields a macro builds from a
 * layout file, for example) come from the hydrated blueprint cache, with no
 * server round trip per popup.
 *
 * Unqualified positions complete the enclosing class's blueprint; qualified
 * ones ({@code this.}, {@code ClassName.}, {@code view.}) complete the
 * receiver's statically-resolved class — statics for a class receiver,
 * instance members otherwise.
 */
public class HaxeBlueprintCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    HaxeReference reference = PsiTreeUtil.getParentOfType(position, HaxeReference.class);
    if (reference == null) return;

    HaxeClass targetClass;
    boolean staticsOnly = false;
    HaxeReference receiver = HaxeResolveUtil.getLeftReference(reference);
    if (receiver == null) {
      targetClass = PsiTreeUtil.getParentOfType(position, HaxeClass.class);
    } else {
      HaxeResolveResult receiverResult = receiver.resolveHaxeClass();
      targetClass = receiverResult != null ? receiverResult.getHaxeClass() : null;
      staticsOnly = receiver.resolve() instanceof HaxeClass;
    }
    if (targetClass == null) return;

    VirtualFile contextFile = HaxeCompilerDisplayService.physicalFileOf(position);
    if (contextFile == null) return;
    TypeBlueprint blueprint = HaxeCompilerResolveService.getInstance(position.getProject())
      .blueprintForType(contextFile, targetClass.getQualifiedName());
    if (blueprint == null) return;

    HaxeClassModel model = targetClass.getModel();
    List<TypeBlueprint.Member> members = staticsOnly ? blueprint.statics() : blueprint.fields();
    for (TypeBlueprint.Member member : members) {
      addGeneratedMember(result, model, member);
    }
    if (receiver == null) {
      // unqualified code sees the enclosing class's statics too
      for (TypeBlueprint.Member member : blueprint.statics()) {
        addGeneratedMember(result, model, member);
      }
    }
  }

  private static void addGeneratedMember(@NotNull CompletionResultSet result,
                                         @NotNull HaxeClassModel model,
                                         @NotNull TypeBlueprint.Member member) {
    // members with a real declaration are already suggested by the static contributors
    if (model.getMember(member.name(), null) != null) return;
    LookupElementBuilder element = LookupElementBuilder.create(member.name())
      .withIcon(member.isMethod() ? HaxeIcons.Method : HaxeIcons.Field)
      .withTailText(" " + HaxeBundle.message("haxe.generated.completion.tail"), true);
    if (member.type() != null) {
      element = element.withTypeText(member.type().presentable());
    }
    result.addElement(element);
  }
}
