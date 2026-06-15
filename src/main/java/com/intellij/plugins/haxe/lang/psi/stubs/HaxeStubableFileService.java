package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.List;

@CustomLog
public class HaxeStubableFileService implements AsyncFileListener {

    private static final Key<Boolean> CAN_CREATE_STUB_KEY =  Key.create("haxe.file.stub.create");

    public static boolean skipFilebasedIndex(HaxeFile haxeFile) {
        return isStubable(haxeFile.getVirtualFile());
    }


    public static void evaluateStubable(VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray(true));
            if(content.contains("#if")){
                file.putUserData(CAN_CREATE_STUB_KEY, Boolean.FALSE);
            }else {
                file.putUserData(CAN_CREATE_STUB_KEY, Boolean.TRUE);
            }
        } catch (IOException e) {
            log.warn("Unable to determine if file is stubable", e);
        }
    }
    public static boolean isStubable(VirtualFile file) {
        if(file == null) return false;

        Boolean isStubable = file.getUserData(CAN_CREATE_STUB_KEY);
        if (isStubable == Boolean.TRUE) return true;
        if (isStubable == Boolean.FALSE) return false;

        synchronized (file) {
            evaluateStubable(file);
        }

        isStubable = file.getUserData(CAN_CREATE_STUB_KEY);
        // while it should not be Null after evaluation its better to be safe as there are many threads that
        // may access this in parallel, if it turn out to be a problem we can just do a syncronize on file
        // or something like that
        if(isStubable == null) {
            log.warn("Stubable result still null after evaluation");
        }
        return isStubable ==  Boolean.TRUE;
    }

    private void clearSubableFlag(VirtualFile file) {
        if (isHaxeFile(file)) {
            file.putUserData(CAN_CREATE_STUB_KEY, null);
        }
    }
    private void updateStubable(@NotNull VirtualFile file) {
        if (isHaxeFile(file)) {
            evaluateStubable(file);
        }
    }

    private static boolean isHaxeFile(@NonNull VirtualFile file) {
        return file != null && file.getFileType() == HaxeFileType.INSTANCE;
    }


    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {

        return new ChangeApplier(){
            @Override
            public void beforeVfsChange() {
                for (VFileEvent event : events) {
                    if(event instanceof VFileContentChangeEvent) {
                        clearSubableFlag(event.getFile());
                    }
                }
            }
        };
    }
}
