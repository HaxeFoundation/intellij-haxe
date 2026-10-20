package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.ReferenceCopyPasteProcessor;
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.java.JavaBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.haxe.editor.HaxeRestoreReferencesDialog;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LightweightHint;
import com.intellij.util.ArrayUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 *  Mostly a Copy of jetbrains com.intellij.codeInsight.editorActions.CopyPasteReferenceProcessor but with Haxe specific types
 */

@CustomLog
public abstract class HaxeBaseCopyPasteReferenceProcessor <TRef extends PsiElement>
        extends CopyPastePostProcessor<HaxeReferenceTransferableData> implements ReferenceCopyPasteProcessor {

    @Override
    public @NotNull List<HaxeReferenceTransferableData> collectTransferableData(@NotNull PsiFile file, @NotNull Editor editor, int @NotNull [] startOffsets, int @NotNull [] endOffsets) {
        if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.NO) {
            return Collections.emptyList();
        }

        if (!(file instanceof HaxeFile)) {
            return Collections.emptyList();
        }

        ArrayList<HaxeReferenceData> array = new ArrayList<>();
        int refOffset = 0; // this is an offset delta for conversion from absolute offset to an offset inside clipboard contents
        for (int j = 0; j < startOffsets.length; j++) {
            refOffset += startOffsets[j];
            List<PsiElement> elements = CollectHighlightsUtil.getElementsInRange(file, startOffsets[j], endOffsets[j]);
            addReferenceData(file, refOffset, elements, array);
            refOffset -= endOffsets[j] + 1; // 1 accounts for line break inserted between contents corresponding to different carets
        }

        if (array.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new HaxeReferenceTransferableData(array.toArray(new HaxeReferenceData[0])));
    }

    /** Records the import-relevant references among one copied range's elements; called once per caret range. */
    protected abstract void addReferenceData(PsiFile file, int startOffset, List<PsiElement> elements,
                                             ArrayList<HaxeReferenceData> to);

    @Override
    public @NotNull List<HaxeReferenceTransferableData> extractTransferableData(@NotNull Transferable content) {
        HaxeReferenceTransferableData referenceData = null;
        if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE != CodeInsightSettings.NO) {
            try {
                DataFlavor flavor = HaxeReferenceData.getDataFlavor();
                if (flavor != null) {
                    referenceData = (HaxeReferenceTransferableData)content.getTransferData(flavor);
                }
            }
            catch (UnsupportedFlavorException | IOException ignored) {
            }
        }

        if (referenceData != null) { // copy to prevent changing of original by convertLineSeparators
            return Collections.singletonList(referenceData.clone());
        }

        return Collections.emptyList();
    }

    @Override
    public void processTransferableData(@NotNull Project project,
                                        @NotNull Editor editor,
                                        @NotNull RangeMarker bounds,
                                        int caretOffset,
                                        @NotNull Ref<? super Boolean> indented, @NotNull List<? extends HaxeReferenceTransferableData> values) {
        if (DumbService.getInstance(project).isDumb()) {
            return;
        }
        Document document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

        if (!(file instanceof HaxeFile)) {
            return;
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments();
        assert values.size() == 1;
        HaxeReferenceData[] referenceData = values.getFirst().getData();
        TRef[] refs = findReferencesToRestore(file, bounds, referenceData);
        if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.ASK) {
            askReferencesToRestore(project, refs, referenceData);
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        Consumer<ProgressIndicator> consumer = indicator -> {
            Set<String> imported = new TreeSet<>();
            restoreReferences(referenceData, refs, imported);
            if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.YES && !imported.isEmpty()) {
                String notificationText = JavaBundle.message("copy.paste.reference.notification", imported.size());
                app.invokeLater(
                        () -> showHint(editor, notificationText, e -> {
                            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                reviewImports(project, file, imported);
                            }
                        }), ModalityState.nonModal(), __ -> editor.isDisposed());
            }
        };

        app.runWriteActionWithCancellableProgressInDispatchThread(JavaBundle.message("progress.title.restore.references"), project, null, consumer);
    }

    protected abstract void removeImports(@NotNull PsiFile file, @NotNull Set<String> imports);

    private void reviewImports(@NotNull Project project, @NotNull PsiFile file, @NotNull Set<String> importedClasses) {
        HaxeRestoreReferencesDialog dialog = new HaxeRestoreReferencesDialog(project, importedClasses.toArray(String[]::new));
        dialog.setTitle(JavaBundle.message("dialog.import.on.paste.title3"));
        if (dialog.showAndGet()) {
            List<String> selectedElements = dialog.getSelectedElements();
            if (!selectedElements.isEmpty()) {
                WriteCommandAction.runWriteCommandAction(project, "", null, () ->
                        removeImports(file, new HashSet<>(selectedElements)));
            }
        }
    }



    protected abstract TRef @NotNull [] findReferencesToRestore(@NotNull PsiFile file,
                                                                @NotNull RangeMarker bounds,
                                                                HaxeReferenceData @NotNull [] referenceData);


    protected abstract void restoreReferences(HaxeReferenceData @NotNull [] referenceData,
                                              TRef @NotNull [] refs,
                                              @NotNull Set<? super String> imported);

    private static void askReferencesToRestore(@NotNull Project project, PsiElement @NotNull [] refs,
                                               HaxeReferenceData @NotNull [] referenceData) {
        PsiManager manager = PsiManager.getInstance(project);

        ArrayList<Object> array = new ArrayList<>();
        Object[] refObjects = new Object[refs.length];
        for (int i = 0; i < referenceData.length; i++) {
            PsiElement ref = refs[i];
            if (ref != null) {
                log.assertTrue(ref.isValid());
                HaxeReferenceData data = referenceData[i];
                PsiClass refClass = JavaPsiFacade.getInstance(manager.getProject()).findClass(data.qClassName, ref.getResolveScope());
                if (refClass == null) continue;

                Object refObject = refClass;
                if (data.staticMemberName != null) {
                    //Show static members as Strings
                    refObject = refClass.getQualifiedName() + "." + data.staticMemberName;
                }
                refObjects[i] = refObject;

                if (!array.contains(refObject)) {
                    array.add(refObject);
                }
            }
        }
        if (array.isEmpty()) return;

        Object[] selectedObjects = ArrayUtil.toObjectArray(array);
        Arrays.sort(selectedObjects, (o1, o2) -> getFQName(o1).compareToIgnoreCase(getFQName(o2)));
        String[] strings = Arrays.stream(selectedObjects).map(Object::toString).toArray(String[]::new);
        HaxeRestoreReferencesDialog dialog = new HaxeRestoreReferencesDialog(project, strings);
        dialog.show();
        selectedObjects = dialog.getSelectedElements().toArray();

        for (int i = 0; i < referenceData.length; i++) {
            PsiElement ref = refs[i];
            if (ref != null) {
                PsiUtilCore.ensureValid(ref);
                Object refObject = refObjects[i];
                boolean found = false;
                for (Object selected : selectedObjects) {
                    if (Comparing.equal(refObject, selected)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    refs[i] = null;
                }
            }
        }
    }

    private static void showHint(@NotNull Editor editor, @NotNull @NlsContexts.HintText String info, @Nullable HyperlinkListener hyperlinkListener) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        LightweightHint hint = new LightweightHint(HintUtil.createInformationLabel(info, hyperlinkListener, null, null));

        int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE;
        HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER, flags, 0, false);
    }

    private static String getFQName(@NotNull Object element) {
        return element instanceof PsiClass ? ((PsiClass)element).getQualifiedName() : (String)element;
    }
}