package im.turms.turms.common;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class TrivialTimerService {

    Map<String, ScheduledFuture<?>> scheduledFutureMap;

    TaskScheduler taskScheduler;

    public TrivialTimerService() {
        scheduledFutureMap = new HashMap<>();
        taskScheduler = new ConcurrentTaskScheduler();
    }

    public synchronized void reschedule(String key, String cronExpression, Runnable runnable) {
        ScheduledFuture<?> future = scheduledFutureMap.get(key);
        if (future != null) {
            future.cancel(false);
        }
        future = taskScheduler.schedule(runnable, new CronTrigger(cronExpression));
        scheduledFutureMap.put(key, future);
    }
}
