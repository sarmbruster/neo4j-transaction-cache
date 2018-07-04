package org.neo4j.contrib.txcache;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.helpers.collection.Iterators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TxCacheFunctionsTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withProcedure(TxCacheFunctions.class)
            .withFunction(TxCacheFunctions.class);

    @Test
    public void shouldCacheIncrementWork() {
        long counter = Iterators.single(neo4j.getGraphDatabaseService().execute(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "RETURN org.neo4j.contrib.txcache.get('counter', true) AS counter"
        ).columnAs("counter"));
        assertEquals(2, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

    @Test
    public void shouldCacheWorkWithMultipleInvocations() {
        long counter = Iterators.single(neo4j.getGraphDatabaseService().execute(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "UNWIND range(0,9) AS x CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "WITH COUNT(*) AS dummy3 " +
                        "RETURN org.neo4j.contrib.txcache.get('counter', true) AS counter"
        ).columnAs("counter"));
        assertEquals(11, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

    @Test
    public void shouldClearWork() {
        long counter = Iterators.single(neo4j.getGraphDatabaseService().execute(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "CALL org.neo4j.contrib.txcache.clear() YIELD map AS cache " +
                        "RETURN cache.counter AS counter"
        ).columnAs("counter"));
        assertEquals(2, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

}
