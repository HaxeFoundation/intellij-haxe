package com.intellij.plugins.haxe.ide;

import lombok.Getter;
import lombok.Setter;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Visitor;


@Getter
@Setter
public class ReferenceCodeLink extends CustomNode {
    private String linkText;
    private String psiReference;

    public ReferenceCodeLink() {
    }

    public ReferenceCodeLink(String linkText, String psiReference) {
        this.linkText = linkText;
        this.psiReference = psiReference;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
