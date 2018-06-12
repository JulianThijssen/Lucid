package lucid.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private Runnable task;
    
    private ScheduledFuture<?> future;
    
    private ScheduledThreadPoolExecutor executor;
    
    public Scheduler(Runnable task) {
        this.task = task;
        
        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    }
    
    public void executeAtFixedRate(long msPeriod) {
        future = executor.scheduleAtFixedRate(task, 0, msPeriod, TimeUnit.MILLISECONDS);
    }
    
    public void executeOnce(long msDelay) {
        future = executor.schedule(task, msDelay, TimeUnit.MILLISECONDS);
    }
    
    public void stop() {
        future.cancel(false);
    }
}
