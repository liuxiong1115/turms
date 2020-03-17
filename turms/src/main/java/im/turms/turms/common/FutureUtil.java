package im.turms.turms.common;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FutureUtil {
    private FutureUtil() {
    }

    public static <T> CompletableFuture<T> race(List<CompletableFuture<T>> futures) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        for (CompletableFuture<T> future : futures) {
            future.whenComplete((value, throwable) -> {
                if (value != null) {
                    resultFuture.complete(value);
                    for (CompletableFuture<T> futureToCancel : futures) {
                        futureToCancel.cancel(false);
                    }
                }
            });
        }
        return resultFuture;
    }
}
