package com.intellij.plugins.haxe.model.fixer;

import org.apache.commons.text.translate.*;

import java.util.Map;


public class HaxeStringEscapeUtil {

    public static final CharSequenceTranslator TO_DOUBLE_QUOTE_TRANSLATOR;

    static {
        final Map<CharSequence, CharSequence> mapping = Map.ofEntries(
                Map.entry("\"", "\\\""),
                Map.entry("\\'", "'"),
                Map.entry( "$$", "$")
        );
        LookupTranslator lookupTranslator = new LookupTranslator(mapping);
        TO_DOUBLE_QUOTE_TRANSLATOR = new AggregateTranslator(lookupTranslator);
    }

    public static final CharSequenceTranslator TO_SINGLE_QUOTE_TRANSLATOR;

    static {
        final Map<CharSequence, CharSequence> mapping = Map.ofEntries(
                Map.entry( "\\\"", "\""),
                Map.entry( "'", "\\'"),
                Map.entry( "$", "$$")
        );
        LookupTranslator lookupTranslator = new LookupTranslator(mapping);
        TO_SINGLE_QUOTE_TRANSLATOR = new AggregateTranslator(lookupTranslator);
    }
}
