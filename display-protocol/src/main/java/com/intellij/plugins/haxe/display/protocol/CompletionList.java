package com.intellij.plugins.haxe.display.protocol;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A {@code display/completion} result: the items the compiler offers at the
 * position.
 * <ul>
 *   <li>{@link #modeKind} is the completion mode the compiler chose for the
 *   position: 0 Field, 1 StructureField, 2 Toplevel, 3 Metadata, 4 TypeHint,
 *   5 Extends, 6 Implements, 7 StructExtension, 8 Import, 9 Using, 10 New,
 *   11 Pattern, 12 Override, 13 TypeRelation, 14 TypeDeclaration.</li>
 *   <li>{@link #expectedType} is the type the position expects, with
 *   typedefs followed, which a Toplevel position carries when it sits in an
 *   argument, an initializer, an assignment or a return; null elsewhere.</li>
 *   <li>{@link #replaceRange} is the text an item replaces, when the compiler
 *   reports one.</li>
 *   <li>{@link #incomplete} marks a list the compiler cut short, such as a
 *   toplevel list the server had not fully indexed yet.</li>
 * </ul>
 */
public record CompletionList(List<CompletionItem> items,
                             int modeKind,
                             @Nullable JsonTypeRef expectedType,
                             @Nullable Range replaceRange,
                             boolean incomplete) {
}
