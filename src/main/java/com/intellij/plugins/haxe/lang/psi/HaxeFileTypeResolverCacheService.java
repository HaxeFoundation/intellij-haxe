package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT;

/**
 * Experimental resolve caching for Type psi elements
 * the theory is that all Type psi elements with the same text in the same file  should  resolve to the same class
 * the only exception to this rule is TypeParameters as multiple methods can have typeParameters with the same name.
 * this cache should hopefully reduce the need to look trough imports and same package files for Types already resolved.
 */
@Service(Service.Level.PROJECT)
public final class HaxeFileTypeResolverCacheService implements Disposable {

    private static final Key<CachedValue<ConcurrentMap<String, HaxeClass>>> RESOLVE_CACHE_KEY = Key.create("HAXE_TYPE_RESOLVE_CACHE");

    public static @NotNull HaxeFileTypeResolverCacheService getInstance(@NotNull Project project) {
        return project.getService(HaxeFileTypeResolverCacheService.class);
    }

    private  SimpleModificationTracker lowMemoryTracker = new SimpleModificationTracker();
    public boolean ENABLE_CACHING = true;


    public HaxeFileTypeResolverCacheService() {
        LowMemoryWatcher.register(() -> lowMemoryTracker.incModificationCount(), this);
    }

    @Nullable
    public HaxeClass getCachedResolvedType(@NotNull HaxeType type) {
        if(!ENABLE_CACHING) return null;
        PsiFile containingFile = type.getContainingFile();
        if(containingFile.isPhysical()) {
            var cacheMap = getFileTypeResolveCacheMap(containingFile);
            return cacheMap.get(type.getText());
        }else {
            return null;
        }
    }

    public void putResolvedType(@NotNull HaxeType type, @NotNull HaxeClass resolved) {
        if(!ENABLE_CACHING) return;
        // we can not cache typeParameters as these can be different between methods and classes
        if(resolved.isTypeParameter()) return;

        PsiFile containingFile = type.getContainingFile();
        if(containingFile.isPhysical()) {
            var cacheMap = getFileTypeResolveCacheMap(containingFile);
            cacheMap.put(type.getText(), resolved);
        }
    }


    @NotNull
    private ConcurrentMap<String, HaxeClass> getFileTypeResolveCacheMap(@NotNull PsiFile file) {
        CachedValuesManager manager = CachedValuesManager.getManager(file.getProject());
        return manager.getCachedValue(file, RESOLVE_CACHE_KEY, new CacheProvider(lowMemoryTracker));
    }

    @Override
    public void dispose() {
        lowMemoryTracker.incModificationCount();
        lowMemoryTracker = null;
    }

    //NOTE:
    // Trying to use CachedValueProvider here as ConcurrentMap is supposedly exempt from the idempotence check.
    // if it causes problem then we will need to replace this with something like userData or something else.
    // Note that CachedValueStabilityChecker will likely perform checks for this class os try to avoid adding
    // more fields to it.
    //
    private static final class CacheProvider implements CachedValueProvider<ConcurrentMap<String, HaxeClass>> {
        private final SimpleModificationTracker lowMemoryTracker;

        CacheProvider(@NotNull SimpleModificationTracker lowMemoryTracker) {
            this.lowMemoryTracker = lowMemoryTracker;
        }
        @Override
        public @NotNull Result<ConcurrentMap<String, HaxeClass>> compute() {
            // we clear the cache on any PSI change the be sure we  avoid issues when import.hx or other parts if the resolve chain changes
            return Result.create(new ConcurrentHashMap<>(), MODIFICATION_COUNT, lowMemoryTracker);
        }
    }
}
