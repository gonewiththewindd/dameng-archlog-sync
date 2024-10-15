package org.gone.dameng.datasync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DataSynchronizer {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DataSynchronizer.class, args);
//        Scheduler scheduler = context.getBean(Scheduler.class);
//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
//        scheduledExecutorService.scheduleWithFixedDelay(() -> scheduler.syncArchiveFile(), 100, 3000, TimeUnit.MILLISECONDS);
//        scheduledExecutorService.scheduleWithFixedDelay(() -> scheduler.replayArchiveFile(), 50, 1000, TimeUnit.MILLISECONDS);
    }
}