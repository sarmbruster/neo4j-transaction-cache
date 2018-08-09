package org.neo4j.contrib.txcache;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TxCacheFunctions {

    static ThreadLocal<Map<String, Object>> cacheThreadLocal = ThreadLocal.withInitial(HashMap::new);
    static ThreadLocal<Long> lastTxStartTimeThreadLocal = ThreadLocal.withInitial(() -> -1l);

    @Context
    public KernelTransaction kernelTransaction;

    @Context
    public Log log;

    public static Map<String,Object> getCache(KernelTransaction kernelTransaction) {

        long lastTxStartTime = lastTxStartTimeThreadLocal.get();
        long thisTxStartTime = kernelTransaction.startTime();
        if (lastTxStartTime!=thisTxStartTime) {
            //log.info("we have a different tx on this thread, using a new cache");
            Map<String,Object> cache = new HashMap<>();
            cacheThreadLocal.set(cache);
            lastTxStartTimeThreadLocal.set(thisTxStartTime);
            return cache;
        } else {
            //log.info("we have a known tx on this thread, reusing cache");
            return cacheThreadLocal.get();
        }
    }

    @Procedure
    public Stream<MapResult> put(@Name("key") String key, @Name("value") Object value) {
        Map<String, Object> cache = getCache(kernelTransaction);
        cache.put(key, value);
        return Stream.of(new MapResult(cache));
    }

    @UserFunction
    public Object get(@Name("key") String key, @Name(value = "clearCache", defaultValue = "false") boolean clearCache) {
        Map<String, Object> cache = getCache(kernelTransaction);
        if (clearCache) {
            cacheThreadLocal.remove();
        }
        return cache.get(key);
    }

    @Procedure
    public Stream<MapResult> add(@Name("key") String key, @Name("value") long increment) {
        Map<String, Object> map = getCache(kernelTransaction);
        long value = (long) map.get(key) + increment;
        map.put(key, value);
        return Stream.of(new MapResult(map));
    }

    @Procedure
    public Stream<MapResult> clear() {
        Map<String, Object> map = getCache(kernelTransaction);
        cacheThreadLocal.remove();
        return Stream.of(new MapResult(map));
    }

    public class MapResult {
        public Map<String, Object> map;

        public MapResult(Map<String, Object> map) {
            this.map = map;
        }
    }

}
