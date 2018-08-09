package org.neo4j.contrib.txcache;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Iterators;

import java.util.Map;

import static org.junit.Assert.*;

public class TxCacheFunctionsTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withConfig(GraphDatabaseSettings.procedure_unrestricted, "org.neo4j.contrib.txcache.*")
            .withProcedure(TxCacheFunctions.class)
            .withFunction(TxCacheFunctions.class);

    @Test
    public void shouldCacheIncrementWork() {
        long counter = singleResultCypher(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "RETURN org.neo4j.contrib.txcache.get('counter', true)");
        assertEquals(2, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

    @Test
    public void shouldCacheWorkWithMultipleInvocations() {
        long counter = singleResultCypher(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "UNWIND range(0,9) AS x CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "WITH COUNT(*) AS dummy3 " +
                        "RETURN org.neo4j.contrib.txcache.get('counter', true)");
        assertEquals(11, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

    @Test
    public void shouldClearWork() {
        long counter = singleResultCypher(
                "CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map AS dummy1 " +
                        "CALL org.neo4j.contrib.txcache.add('counter', 1) YIELD map AS dummy2 " +
                        "CALL org.neo4j.contrib.txcache.clear() YIELD map AS cache " +
                        "RETURN cache.counter");
        assertEquals(2, counter);
        assertTrue(TxCacheFunctions.cacheThreadLocal.get().isEmpty());
    }

    @Test
    public void shouldDifferentTransactionsHaveDifferentCacheWithoutExplicitCleaning() {

        // NB: the two cypher statements in this thes are executed in separate transactions, so the cache will be flushed
        Map<String,Object> cache = singleResultCypher("CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map RETURN map");
        assertEquals(1, cache.size());
        assertEquals(1l, cache.get("counter"));

        Object value = singleResultCypher("RETURN org.neo4j.contrib.txcache.get('counter')");
        assertNull(value);
    }

    @Test
    public void shouldTransactionsMaintainCacheForMultipleStatements() {

        try (Transaction tx = neo4j.getGraphDatabaseService().beginTx()) {

            // NB: the two cypher statements in this thes are executed in separate transactions, so the cache will be flushed
            Map<String,Object> cache = singleResultCypher("CALL org.neo4j.contrib.txcache.put('counter', 1) YIELD map RETURN map");
            assertEquals(1, cache.size());
            assertEquals(1l, cache.get("counter"));

            long value  = singleResultCypher("RETURN org.neo4j.contrib.txcache.get('counter')");
            assertEquals(1l, value);
            tx.success();
        }
    }

    private <T> T singleResultCypher(String cypher) {
        Map<String, Object> row = Iterators.single(neo4j.getGraphDatabaseService().execute(cypher));
        return (T) Iterables.singleOrNull(row.values());
    }

}
