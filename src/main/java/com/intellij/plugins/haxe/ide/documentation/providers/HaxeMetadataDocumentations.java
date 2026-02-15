package com.intellij.plugins.haxe.ide.documentation.providers;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class HaxeMetadataDocumentations {

    private static final Map<String, MetadataInfo> docsMap = new HashMap<>();

    private static @Nullable VirtualFile getDocsFile() {
        URL resource = HaxeMetadataDocumentations.class.getResource("/haxe/docs/MetadataDocs.json");
        return VfsUtil.findFileByURL(resource);
    }

    private static void parseAndLoadDocs() {
        VirtualFile docsFile = getDocsFile();
        ObjectMapper mapper = new ObjectMapper();

        try (
                MappingIterator<DocFormatFormat> iterator = mapper
                        .readerFor(DocFormatFormat.class)
                        .readValues(docsFile.getInputStream())
        ) {
            iterator.readAll().forEach(HaxeMetadataDocumentations::createDocs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createDocs(DocFormatFormat doc) {
        MetadataInfo.builder()
                .metadata(doc.metadata.replace("@:", ""))
                .description(doc.description)
                .arguments(doc.arguments)
                .platforms(doc.platforms)
                .build()
                .addToDocs(docsMap);
    }

    public static MetadataInfo getDocsFor(String metadataName) {
        if(docsMap.isEmpty()) {
            parseAndLoadDocs();
        }

        return docsMap.getOrDefault(metadataName, null);
    }
    public static Collection<MetadataInfo> getDocs() {
        if(docsMap.isEmpty()) {
            parseAndLoadDocs();
        }

        return docsMap.values();
    }

    private record DocFormatFormat(String metadata, List<String> arguments, String description, List<String> platforms) {}

    @Value
    @Builder
    public static class MetadataInfo {
        public MetadataInfo addToDocs(Map<String, MetadataInfo> docsMap) {
            return docsMap.put(metadata, this);
        }

        String metadata;
        List<String> arguments;
        String description;
        List<String> platforms;
    }
}


