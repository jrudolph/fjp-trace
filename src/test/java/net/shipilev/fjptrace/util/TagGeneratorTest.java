package net.shipilev.fjptrace.util;

import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.shipilev.fjptrace.TagGenerator;

public class TagGeneratorTest {

    @Test
    public void testSeq() {
        TagGenerator generator = new TagGenerator(1);

        Set<Integer> results = new HashSet<>();
        for (int c = 0; c < 100000; c++) {
            boolean added = results.add(generator.next());
            Assert.assertTrue("Duplicate element", added);
        }
    }

    @Test
    public void testPar() throws ExecutionException, InterruptedException {
        final TagGenerator generator = new TagGenerator(1);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Future<Set<Integer>>> futures = new ArrayList<>(threads);
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(new Callable<Set<Integer>>() {
                @Override
                public Set<Integer> call() throws Exception {
                    Set<Integer> results = new HashSet<>();
                    for (int c = 0; c < 100000; c++) {
                        Assert.assertTrue("Duplicate element", results.add(generator.next()));
                    }
                    return results;
                }
            }));
        }

        Set<Integer> global = new HashSet<>();
        for (Future<Set<Integer>> f : futures) {
            for (Integer i : f.get()) {
                Assert.assertTrue("Duplicate element", global.add(i));
            }
        }
    }


}
