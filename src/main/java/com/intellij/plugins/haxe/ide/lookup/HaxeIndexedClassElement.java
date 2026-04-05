package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.*;


public class HaxeIndexedClassElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  @Getter private final String name;
  @Getter private final  String path;
  @Getter private final HaxeComponentType type;
  @Getter private String qname;


  // we need a psi element  when resolving qname (making sure we get data from the right project etc)
  private final PsiElement helperPsi;
  // findClassByQName is too slow for "normal" use, so we delay  the resolve so we can get  docs lookup
  private PsiElement myElement = null;

  private String presentableText;
  private String tailText;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon = null;

  public HaxeIndexedClassElement(String name, String path, HaxeComponentType componentType, PsiElement helperPsi) {
    this.name  = name;
    this.path  = path;
    qname = createQname(name, path);

    this.helperPsi = helperPsi;

    presentableText = name + " ";
    tailText = path;
    type = componentType;
    icon = componentType.getCompletionIcon();

  }

  @Override
  public void handleInsert(InsertionContext context) {
    PsiFile file = context.getFile();
    PsiElement element = file.findElementAt(context.getStartOffset());
    addImportIfNecessary(context, element, qname);
  }




  @Override
  public @Nullable PsiElement getPsiElement() {
    if (myElement == null) {
      myElement = HaxeResolveUtil.findClassByQName(qname, helperPsi);
    }
    return myElement;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return name;
  }

  /**
   * Returns the fully-qualified class name so that {@code getDedupeName()} in
   * {@code HaxeControllingCompletionContributor} uses the qname as the dedup key.
   * This ensures two classes with the same simple name from different packages are
   * kept, while two identical entries (same qname) added by different contributor
   * registrations are properly collapsed to one.
   */
  @NotNull
  @Override
  public String deduplicateKey() {
    return qname;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(presentableText);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTailText(tailText, true);
  }

  @Override
  public PrioritizedLookupElement<LookupElement> toPrioritized() {
    return (PrioritizedLookupElement<LookupElement>)PrioritizedLookupElement.withPriority(this,priority.calculate());
  }
}
