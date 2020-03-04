package benchmark.rtree;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
@State(Scope.Thread)
public class DavidmotenBenchmark {
    private RTree<Long, PointFloat> treeWithData;
    private RTree<Long, PointFloat> emptyTree;
    private long userId;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DavidmotenBenchmark.class.getSimpleName())
                .forks(1)
                .addProfiler(GCProfiler.class)
                .output("DavidmotenBenchmark.log")
                .build();
        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setup() {
        emptyTree = RTree.maxChildren(16).create();
        treeWithData = RTree.maxChildren(16).create();
        for (long i = 0; i < 100000; i++) {
            treeWithData.add(i, getUserLocation(i));
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        userId = 0;
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public RTree<Long, PointFloat> addUserLocation() {
        userId++;
        emptyTree.add(userId, getUserLocation(userId));
        return emptyTree;
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public Iterable<Entry<Long, PointFloat>> queryNearestUserIds() {
        userId++;
        return treeWithData.nearest(getUserLocation(userId), 45, 50);
    }

    @Benchmark
    @Warmup(iterations = 3, batchSize = 100000)
    @Measurement(iterations = 3, batchSize = 100000)
    @BenchmarkMode(Mode.SingleShotTime)
    public RTree<Long, PointFloat> deleteUserLocations() {
        userId++;
        return treeWithData.delete(userId, getUserLocation(userId));
    }

    private PointFloat getUserLocation(long userId) {
        float x = 180 / (userId % 180f);
        float y = 90 / (userId % 90f);
        return PointFloat.create(x, y);
    }
}
