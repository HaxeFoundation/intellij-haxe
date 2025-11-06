package com.intellij.plugins.haxe.ide.refactoring.move;

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.*;

public class HaxeMoveStatementHandler extends HaxeLineMover {

    private enum ScopeChangeDirection {
        OUT,
        IN,
        JUMP,
        NONE,
        PROHIBIT
    }
    private record ScopeInfo(ScopeChangeDirection direction, HaxePsiCompositeElement targetScope, HaxePsiCompositeElement sourceScope){
        static ScopeInfo none(){return new ScopeInfo(ScopeChangeDirection.NONE,null ,null);}
        static ScopeInfo prohibit(){return new ScopeInfo(ScopeChangeDirection.PROHIBIT,null ,null);}
    }
    private record connectedCodeBocks(HaxeCodeBlock previous, HaxeCodeBlock current, HaxeCodeBlock next){}


    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull StatementUpDownMover.MoveInfo info, boolean down) {
        boolean available = super.checkAvailable(editor, file, info, down);
        if (!available) return false;

        // TODO might want to add support for selection
        if (editor.getSelectionModel().hasSelection()) return false;

        if (file instanceof HaxeFile haxeFile) {
            //TODO use this as parameter ?
            LineRange firstRange = info.toMove;
            LineRange secondRange = info.toMove2;

            TreeUtil.ensureParsed(file.getNode());

            firstRange = expandRangeToExpression(firstRange, editor, haxeFile);

            if(!validRange(firstRange)) return false;

            PsiElement expression = findExpression(firstRange, editor, haxeFile, true);
            if (PsiTreeUtil.findChildOfType(expression, PsiErrorElement.class, false)!= null) {
                return false;
            }
            if (expression == null  || isComment(expression)) {
                return false;
            }


            LineRange lineRange = down ? getLineAfter(firstRange) : getLineBefore(firstRange);
            if (lineRange == null) return false;

            PsiElement neighborElement = findExpression(lineRange, editor, haxeFile, !down);
            if (isTopLevelElement(neighborElement)) {
                return info.prohibitMove();
            } else {
                ScopeInfo scope = scopeChange(expression, neighborElement, down);

                switch (scope.direction) {
                    case OUT -> secondRange = outOfScope(scope.targetScope, scope.sourceScope, editor, lineRange, down);
                    case IN -> secondRange = intoScope(scope.targetScope,expression ,editor, haxeFile, lineRange, down);
                    case NONE -> secondRange = expandRangeToExpression(lineRange, editor, haxeFile);
                    case JUMP -> secondRange = moveFromOneScopeToAnother(scope.targetScope, editor, lineRange, down);
                    case PROHIBIT -> {
                        info.prohibitMove();
                        return true;
                    }
                }
            }


            if (!validateMove(firstRange, secondRange)) {
                return false;
            }else {
                info.toMove = firstRange;
                info.toMove2 = secondRange;
            }

            return true;
        }
        return false;
    }

    private LineRange moveFromOneScopeToAnother(HaxePsiCompositeElement targetScope, @NotNull Editor editor, LineRange lineRange, boolean down) {
        if (targetScope instanceof HaxeCodeBlock codeBlock) {
            if (down) {
                int line = editor.offsetToLogicalPosition(codeBlock.getTextOffset()).getLine();
                if (targetScope instanceof HaxeSwitchCaseBlock caseBlock) {
                    return new LineRange(lineRange.endLine - 1, line);
                } else {
                    return new LineRange(lineRange.endLine - 1, line + 1);
                }
            } else {
                int line = editor.offsetToLogicalPosition(codeBlock.getTextOffset() + codeBlock.getTextLength()).getLine();
                return new LineRange(line, lineRange.startLine + 1);
            }
        }
        return null;
    }

    private boolean isPartOfSwitchStatment(HaxeCodeBlock codeBlock) {
        return codeBlock. getParent() instanceof HaxeSwitchCaseBlock;
    }

    private static @Nullable HaxeCodeBlock getIfElseCodeBlock(@NotNull PsiElement element) {
        PsiElement child = element.getChildren()[0];
        if(child instanceof HaxeBlockStatement blockStatement) return blockStatement;
        return PsiTreeUtil.getChildOfType(child, HaxeCodeBlock.class);
    }
    private static @Nullable HaxeCodeBlock getTryCatchCodeBlock(@NotNull PsiElement element) {
        if(element instanceof HaxeBlockStatement blockStatement) return blockStatement;
        return PsiTreeUtil.getChildOfType(element, HaxeCodeBlock.class);
    }


    private LineRange intoScope(HaxePsiCompositeElement targetScope, PsiElement expression, @NotNull Editor editor, HaxeFile haxeFile, LineRange lineRange, boolean down) {
        if(down) {
            PsiElement parent = getScopeOwner(targetScope);
            int textOffset = parent.getTextOffset();
            List<@NotNull PsiElement> children = Arrays.asList(parent.getChildren());
            for (PsiElement child : children) {
                if (PsiTreeUtil.isAncestor(child, expression, false)) {
                        textOffset = child.getTextOffset() + child.getTextLength() +1;
                        break;
                }
            }
            int startLine = editor.offsetToLogicalPosition(textOffset).getLine();

            PsiElement elementInScope = targetScope.getFirstChild();
            int endLine = editor.offsetToLogicalPosition(elementInScope.getTextOffset()).getLine();
            return new LineRange(startLine, endLine+1);

        }else {
            int offset = targetScope.getTextOffset() + targetScope.getTextLength();
            if(targetScope instanceof HaxeSwitchCase) offset +=1;
            int start = editor.offsetToLogicalPosition(offset).getLine();
            int endLine = lineRange.startLine + 1;
            start = Math.min(start, endLine);
            return new LineRange(start, endLine);
        }

    }

    private PsiElement getScopeOwner(HaxePsiCompositeElement scope) {
        // workaround for blocks that are in expressions with multiple levels of children
        PsiElement parent = scope.getParent();
        if(parent instanceof HaxeDoWhileBody whileBody) {
            return whileBody.getParent();
        }
        if(parent instanceof HaxeGuardedStatement guardedStatement) {
            return guardedStatement.getParent();
        }
        return parent;
    }

    private LineRange outOfScope(HaxePsiCompositeElement targetScope, HaxePsiCompositeElement sourceScope, @NotNull Editor editor, LineRange fromRange, boolean down) {
        @NotNull PsiElement[] children = targetScope.getChildren();
        if (down) {
            for (PsiElement child : children) {
                if (PsiTreeUtil.isAncestor(child, sourceScope, false)) {
                    PsiElement psiElement = child.getNextSibling();
                    if(psiElement != null) {
                        int lineafterScope = editor.offsetToLogicalPosition(psiElement.getTextOffset()).getLine();
                        return new LineRange(fromRange.endLine - 1, lineafterScope + 1);
                    }
                }
            }
            if (targetScope instanceof HaxeSwitchCaseBlock  caseBlock) {
                PsiElement parent = caseBlock.getParent().getParent();
                if(parent instanceof  HaxeSwitchBlock switchBlock) {
                   return outOfScope(switchBlock, sourceScope, editor, fromRange, down);
                }
            }
        }else {
            for (PsiElement child : children) {
                if (PsiTreeUtil.isAncestor(child, sourceScope, false)) {
                    int lineBeforeScope = editor.offsetToLogicalPosition(child.getTextOffset()).getLine();
                    return new LineRange(lineBeforeScope, fromRange.startLine+1);
                }
            }
        }
        return null;
    }

    private ScopeInfo scopeChange(PsiElement fromScope, PsiElement toScope, boolean down) {
//        if(fromScope == null  || toScope == null) return  ScopeInfo.prohibit();
        if(toScope == null) return  ScopeInfo.none();
        if(fromScope == null) return  ScopeInfo.prohibit();
        if(fromScope == toScope ) return  new ScopeInfo(ScopeChangeDirection.NONE, null, null);
        boolean findParent = PsiTreeUtil.isAncestor(toScope, fromScope,true);

        HaxePsiCompositeElement scopeA = findScope(fromScope, true, down, null);
        HaxePsiCompositeElement scopeB = findScope(toScope, findParent, down, fromScope);
        if(scopeA == null  || scopeB == null)  return  ScopeInfo.none();
        if(isCaseAttemptingTOLeaveSwitchExpression(fromScope, scopeB)) return ScopeInfo.prohibit();

        if(PsiTreeUtil.isAncestor(scopeA, scopeB, true)) return new ScopeInfo(ScopeChangeDirection.IN, scopeB, scopeA);
        if(PsiTreeUtil.isAncestor(scopeB,scopeA, true)) return new ScopeInfo(ScopeChangeDirection.OUT, scopeB, scopeA);

        if(scopeA == scopeB) return  ScopeInfo.none();

        return new ScopeInfo(ScopeChangeDirection.JUMP, scopeB, scopeA);
    }

    private static boolean isCaseAttemptingTOLeaveSwitchExpression(PsiElement fromScope, HaxePsiCompositeElement toScope) {
        if(fromScope instanceof HaxeSwitchCase) {
            return PsiTreeUtil.isAncestor(toScope,fromScope, true);
        }
        return false;
    }

    private static @Nullable HaxePsiCompositeElement findScope(PsiElement scope, boolean parent, boolean down, PsiElement fromScope) {
//        if( scope instanceof HaxeBlockStatement  statement) return statement;
//        if( scope instanceof HaxeSwitchCaseBlock statement) return statement;
        if( scope instanceof HaxeSwitchBlock switchBlock) {
            List<HaxeSwitchCase> switchCaseList = switchBlock.getSwitchCaseList();
            if(switchCaseList.isEmpty())return switchBlock;
            if(down) {
                return switchCaseList.getFirst();
            }else {
                return switchCaseList.getLast();
            }

        }
        // check if we are in last case block, and if se jump out of switch
        if(parent && scope instanceof HaxeSwitchCase switchCase) {
            if(switchCase.getParent() instanceof  HaxeSwitchBlock switchBlock) {
                List<HaxeSwitchCase> switchCaseList = switchBlock.getSwitchCaseList();
                if(switchCase == switchCaseList.getFirst() || switchBlock == switchCaseList.getLast()) {
                    scope = scope.getParent();
                }
            }
        }
        // if inside if/else-if/else block (find related surrounding blocks)
        connectedCodeBocks connectedCodeBocks = tryFindSoundingIfElseBlocks(scope);
        if(connectedCodeBocks == null) {
            // if inside try/catch block (find related surrounding blocks)
            if(!(fromScope instanceof HaxeTryStatement) && !(fromScope instanceof HaxeCatchStatement)) {
                connectedCodeBocks = tryFindSoundingTryCatchBlocks(scope);
            }
        }
        if(connectedCodeBocks != null) {
            if(!parent) {
                if (connectedCodeBocks.current != null) return connectedCodeBocks.current;
            }else {
                //
                if(fromScope != null && PsiTreeUtil.isAncestor(connectedCodeBocks.current, fromScope, false)) {
                    if (down) {
                        if (connectedCodeBocks.next != null) return connectedCodeBocks.next;
                    } else {
                        if (connectedCodeBocks.previous != null) return connectedCodeBocks.previous;
                    }
                }
            }
        }

        if (scope instanceof HaxeGuard guard) {
            scope = guard.getParent();
        }

        if(parent) {
            return PsiTreeUtil.getParentOfType(scope, HaxeBlockStatement.class,HaxeSwitchBlock.class, HaxeSwitchCaseBlock.class);
        } else {
            if (scope instanceof HaxeBlockStatement statement) return statement;
            if (scope instanceof HaxeSwitchCaseBlock statement) return statement;
            return PsiTreeUtil.findChildOfAnyType(scope, HaxeBlockStatement.class, HaxeSwitchBlock.class, HaxeSwitchCaseBlock.class);
        }
    }




    private static connectedCodeBocks tryFindSoundingTryCatchBlocks(PsiElement scope) {
        if(scope instanceof HaxeTryStatement tryStatement) {
            @NotNull PsiElement[] children = tryStatement.getChildren();
            HaxeCodeBlock current = getTryCatchCodeBlock(children[0]);
            HaxeCodeBlock next = null;
            if(children.length>1) {
                next = getTryCatchCodeBlock(children[1]);
            }
            return new connectedCodeBocks(null, current, next);
        }else  if(scope instanceof HaxeCatchStatement statement) {
            if( statement.getParent() instanceof HaxeTryStatement tryStatement) {
                @NotNull PsiElement[] children = tryStatement.getChildren();
                for (int i = 0; i < children.length; i++) {
                    PsiElement child = children[i];
                    if(PsiTreeUtil.isAncestor(child, scope, false)) {
                        HaxeCodeBlock current = getTryCatchCodeBlock(child);
                        HaxeCodeBlock prev = null;
                        HaxeCodeBlock next = null;
                        if(i-1 >= 0) {
                            prev = getTryCatchCodeBlock(children[i-1]);
                        }
                        if(i+1 < children.length) {
                            next = getTryCatchCodeBlock(children[i+1]);
                        }
                        if(current != null) {
                            return new connectedCodeBocks(prev, current, next);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static connectedCodeBocks tryFindSoundingIfElseBlocks(PsiElement scope) {
        if (scope instanceof HaxeGuard guard) scope = guard.getParent();
        if (scope instanceof HaxeGuardedStatement statement) scope = statement.getParent();

        HaxeCodeBlock prev = null;
        HaxeCodeBlock next = null;
        HaxeCodeBlock currentBlock = tryFindSoundingIfElseBlock(scope);

        if (currentBlock == null) return null;

        if (currentBlock.getParent() instanceof HaxeElseStatement elseStatement) {
            prev = tryFindSoundingIfElseBlock(elseStatement.getParent());
        }

        if (currentBlock.getParent() instanceof HaxeIfStatement ifStatement) {
            next = tryFindSoundingIfElseBlock(ifStatement.getElseStatement());
            if(ifStatement.getParent() instanceof HaxeElseStatement elseStatement) {
                prev = tryFindSoundingIfElseBlock(elseStatement.getParent());
            }
        }

        return new connectedCodeBocks(prev, currentBlock, next);
    }
    private static HaxeCodeBlock tryFindSoundingIfElseBlock(PsiElement scope) {
        if(scope instanceof HaxeGuard guard) scope = guard.getParent();
        if(scope instanceof HaxeGuardedStatement statement) scope = statement.getParent();

        if(scope == null) return null;
        return switch (scope) {
            case HaxeIfStatement ifStatement-> ifStatement.getGuardedStatement();
            case HaxeElseStatement elseStatement ->  getIfElseCodeBlock(elseStatement);
            default -> null;
        };
    }

    private boolean isTopLevelElement(PsiElement element) {
        if(element == null) return false;
        if(element.getParent() == null) return false;
        return isRootType(element.getParent());
    }


    private boolean isRootType(PsiElement parent) {
        return switch (parent) {
            case HaxeModule ignored -> true;
            case HaxeClass ignored -> true;
            case HaxeAbstractBody ignored -> true;
            case HaxeClassBody ignored -> true;
            case HaxeInterfaceBody ignored -> true;
            case HaxeExternClassDeclarationBody ignored -> true;
            case null, default -> false;
        };
    }
}
