package benchmark.rtree;

import com.github.plokhotnyuk.rtree2d.core.EuclideanPlane;
import com.github.plokhotnyuk.rtree2d.core.RTree;
import com.github.plokhotnyuk.rtree2d.core.RTreeEntry;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import scala.collection.Iterable;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.Nil;
import scala.jdk.CollectionConverters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
@State(Scope.Thread)
public class PlokhotnyukBenchmark {
    private RTree<Long> treeWithData;
    private RTree<Long> emptyTree;
    private long userId;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PlokhotnyukBenchmark.class.getSimpleName())
                .forks(1)
                .addProfiler(GCProfiler.class)
                .output("PlokhotnyukBenchmark.log")
                .build();
        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setup() {
        List<RTreeEntry<Long>> emptyList = Collections.emptyList();
        Iterable<RTreeEntry<Long>> emptyIterable = CollectionConverters.IterableHasAsScala(emptyList).asScala().toIterable();
        emptyTree = RTree.apply(emptyIterable, 16);

        List<RTreeEntry<Long>> list = new ArrayList<>(100000);
        for (long i = 0; i < 100000; i++) {
            list.add(getEntry(i));
        }
        Iterable<RTreeEntry<Long>> iterable = CollectionConverters.IterableHasAsScala(list).asScala().toIterable();
        treeWithData = RTree.apply(iterable, 16);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        userId = 0;
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public RTree<Long> addUserLocation() {
        userId++;
        List<RTreeEntry<Long>> list = List.of(getEntry(userId));
        Iterable<RTreeEntry<Long>> iterable = CollectionConverters.IterableHasAsScala(list).asScala().toIterable();
        return RTree.update(emptyTree, Nil.toSet(), iterable, 16);
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public IndexedSeq<RTreeEntry<Long>> queryNearestUserIds() {
        userId++;
        RTreeEntry<Long> entry = getEntry(userId);
        return treeWithData.nearestK(entry.minX(), entry.minY(), 50, 45, EuclideanPlane.distanceCalculator());
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public RTree<Long> deleteUserLocations() {
        userId++;
        List<RTreeEntry<Long>> list = List.of(getEntry(userId));
        Iterable<RTreeEntry<Long>> iterable = CollectionConverters.IterableHasAsScala(list).asScala().toIterable();
        return RTree.update(treeWithData, iterable, Nil.toSet(), 16);
    }

    private RTreeEntry<Long> getEntry(long userId) {
        float x = 180 / (userId % 180f);
        float y = 90 / (userId % 90f);
        return EuclideanPlane.entry(x, y, userId);
    }
}
