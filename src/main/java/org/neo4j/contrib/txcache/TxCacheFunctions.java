package org.neo4j.contrib.txcache;

import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TxCacheFunctions {

    static ThreadLocal<Map<String, Object>> cacheThreadLocal = ThreadLocal.withInitial(HashMap::new);

    @Procedure
    public Stream<MapResult> put(@Name("key") String key, @Name("value") Object value) {
        Map<String, Object> cache = cacheThreadLocal.get();
        cache.put(key, value);
        return Stream.of(new MapResult(cache));
    }

    @UserFunction
    public Object get(@Name("key") String key, @Name(value = "clearCache", defaultValue = "false") boolean clearCache) {
        Map<String, Object> cache = cacheThreadLocal.get();
        if (clearCache) {
            cacheThreadLocal.remove();
        }
        return cache.get(key);
    }

    @Procedure
    public Stream<MapResult> add(@Name("key") String key, @Name("value") long increment) {
        Map<String, Object> map = cacheThreadLocal.get();
        long value = (long) map.get(key) + increment;
        map.put(key, value);
        return Stream.of(new MapResult(map));
    }

    @Procedure
    public Stream<MapResult> clear() {
        Map<String, Object> map = cacheThreadLocal.get();
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
