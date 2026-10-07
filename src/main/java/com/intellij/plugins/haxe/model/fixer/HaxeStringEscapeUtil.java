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

    /** Double to single quotes keeping the TEXT: a {@code $} becomes {@code $$}, so nothing starts interpolating. */
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
    /** Double to single quotes keeping the {@code $}: {@code "Hi $name"} becomes {@code 'Hi $name'} and starts interpolating. */
    public static final CharSequenceTranslator TO_SINGLE_QUOTE_INTERPOLATING_TRANSLATOR;
    static {
        final Map<CharSequence, CharSequence> mapping = Map.ofEntries(
                Map.entry( "\\\"", "\""),
                Map.entry( "'", "\'")
        );
        TO_SINGLE_QUOTE_INTERPOLATING_TRANSLATOR = new AggregateTranslator(new LookupTranslator(mapping));
    }
}
