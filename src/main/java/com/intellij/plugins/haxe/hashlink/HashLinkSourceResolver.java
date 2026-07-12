package com.intellij.plugins.haxe.hashlink;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

import org.jetbrains.annotations.Nullable;

/**
 * Maps a source path reported by the debug adapter (taken from the .hl
 * bytecode's debug tables — usually project-relative like {@code src/Main.hx},
 * sometimes absolute) to an IDE source position.
 */
final class HashLinkSourceResolver {
  private HashLinkSourceResolver() {
  }

  /** Resolves to a position, or null when the file cannot be found. 1-based line. */
  static @Nullable XSourcePosition resolve(Project project, @Nullable String path, StackFrame frame) {
    if (path == null || path.isBlank()) {
      return null;
    }
    int line = frame.getLine();
    String normalized = FileUtil.toSystemIndependentName(path);
    return ReadAction.compute(() -> {
      VirtualFile file = findFile(project, normalized, frame);
      return file != null ? XDebuggerUtil.getInstance().createPosition(file, Math.max(0, line - 1)) : null;
    });
  }

  private static @Nullable VirtualFile findFile(Project project, String normalized, StackFrame frame) {
    if (isAbsolute(normalized)) {
      VirtualFile absolute = LocalFileSystem.getInstance().findFileByPath(normalized);
      if (absolute != null) {
        return absolute;
      }
    }

    // lets first try to use indexes to find our file
    // hashlink frames contains the following formats (we try to match on class + method for now)
    // "ClassName.methodName" for class methods
    // "ClassName.~methodName.index" for closure/ref methods
    // "fun$index" for standalone functions

    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    String[] nameParts = frame.getName().split("\\.");
    if(nameParts.length > 1) {
      String className = nameParts[0];
      String methodName = nameParts[1];
      Collection<HaxeClass> haxeClasses = HaxeClassNameUnifiedIndex.getByNameFiltered(className, project,scope);
      for (HaxeClass aClass : haxeClasses) {
        HaxeBaseMemberModel member = aClass.getModel().getMember(methodName, null);
        if(member != null) {
          return aClass.getContainingFile().getVirtualFile();
        }
      }
    }

    //if index search fails, we'll fallback to searching for file

    // relative (or stale absolute): find candidates by file name, prefer the one
    // whose full path ends with the reported path
    String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
    if (fileName.isBlank()) {
      return null;
    }

    Collection<VirtualFile> candidates =
      FilenameIndex.getVirtualFilesByName(fileName, scope);
    VirtualFile byName = null;
    if(candidates.size() == 1) {
      return candidates.iterator().next();
    }else {
      var matchingPaths = new ArrayList<VirtualFile>();
      for (VirtualFile candidate : candidates) {
        if (candidate.getPath().endsWith("/" + normalized) || candidate.getPath().equals(normalized)) {
          matchingPaths.add(candidate);
        }
      }
        return findMostFileApplicable(project, matchingPaths, frame);
    }
  }

  private static @Nullable VirtualFile findMostFileApplicable(Project project, ArrayList<VirtualFile> paths, StackFrame frame) {
    PsiManager psiManager = PsiManager.getInstance(project);
    List<String> nameParts = Arrays.asList(frame.getName().split("\\.")).reversed();
    if (!nameParts.isEmpty()) {
      Map<VirtualFile, Integer> stats = new HashMap<>();
      for (VirtualFile path : paths) {
        PsiFile file = psiManager.findFile(path);
        if (file instanceof HaxeFile haxeFile) {
          Collection<HaxeNamedComponent> components = PsiTreeUtil.findChildrenOfType(haxeFile, HaxeNamedComponent.class);
          for (HaxeNamedComponent namedComponent : components) {
            int score = calculateScore(namedComponent, nameParts);
            stats.put(path, score);
          }
        }
      }
      Optional<VirtualFile> file = stats.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
      if(file.isPresent()){
        return file.get();
      }
    }

    return paths.getFirst();
  }

  private static int calculateScore(HaxeNamedComponent namedComponent, List<String> nameParts) {
    int index = 0;
    int score = 0;
    String name = nameParts.get(index);
    int namePartCount = nameParts.size();
    HaxeNamedComponent component = namedComponent;

    while (component != null && namePartCount > ++index) {
      if (name.equals(component.getName())) {
        score++;
        component = PsiTreeUtil.getParentOfType(component, HaxeNamedComponent.class);
        name = nameParts.get(index);
      }else {
        return score;
      }
    }
    return score;
  }

  // Path.of throws InvalidPathException on adapter-supplied strings that are
  // not paths at all (older adapters sent "?" for synthesized code); anything
  // unparseable is simply not absolute.
  private static boolean isAbsolute(String normalized) {
    try {
      return Path.of(normalized).isAbsolute();
    } catch (InvalidPathException e) {
      return false;
    }
  }
}
