package org.gone.dameng.datasync.schedule;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.gone.dameng.datasync.enums.SystemTypeEnums;
import org.gone.dameng.datasync.model.di.DatabaseInstance;
import org.gone.dameng.datasync.model.di.req.DatabaseInstanceBaseParam;
import org.gone.dameng.datasync.service.def.ArchiveFileService;
import org.gone.dameng.datasync.service.def.DatabaseInstanceService;
import org.gone.dameng.datasync.utils.SshUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@EnableScheduling
@Component
public class Scheduler {

    public static final String ARCHIVE_FILE_DIR = "D:\\workspace\\dmdbms\\sync\\DAMENG\\bak";
    private Lock lock = new ReentrantLock();

    @Autowired
    private ArchiveFileService archiveFileService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private DatabaseInstanceService databaseInstanceService;

    private AtomicBoolean stop = new AtomicBoolean(false);

/*    @Scheduled(fixedDelay = 3 * 1000)
    public void syncArchiveFile() {
        try {
            // 归档文件同步
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("[Scheduler-Synchronizer]acquire sync lock failed, skip");
                return;
            }
            long start = System.currentTimeMillis();
            String[] cmd = {RSYNC_EXE_PATH, "-avz --delete", SRC_PATH, DEST_PATH};
            log.info("[Scheduler-Synchronizer]sync archive file, cmd:{}", Arrays.toString(cmd));
            Process rsync = new ProcessBuilder().command(cmd).start();
            rsync.waitFor();

            byte[] bytes = IOUtils.readFully(rsync.getInputStream(), rsync.getInputStream().available());
            String result = new String(bytes);
            log.info("[Scheduler-Synchronizer]sync archive file...result:\n{}using {}ms", result, System.currentTimeMillis() - start);
            rsync.destroy();
        } catch (Exception e) {
            log.error("[Scheduler-Synchronizer]sync archive file error", e);
        } finally {
            lock.unlock();
        }
    }*/

    @Scheduled(fixedDelay = 1 * 10000)
    public void replayArchiveFile() {
        try {
            if (stop.get()) {
                return;
            }
            // 归档日志重演
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("[Scheduler]acquire lock failed, skip");
                return;
            }
            try {
                archiveFileService.sync();
                archiveFileService.replay(ARCHIVE_FILE_DIR);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("[Scheduler]replay archive file error", e);
        }
    }

    public DatabaseInstance primary;
    public DatabaseInstance standby;


    @PostConstruct
    public void init() {

        String primarySshHost = "127.0.0.1";
        int primarySshPort = 22;
        String primarySshUsername = "Administrator";
        String primarySshPassword = ".12121122.";

        String primaryDatabaseUsername = "SYSDBA";
        String primaryDatabasePassword = "SYSDBA";
        String primaryDatabaseHost = "127.0.0.1";
        String primaryDatabasePort = "52361"; // TODO：端口号最好使用5位数，不然有重复的可能，比如52362端口就包含了523 5236 5362等端口号

        String primaryArchLogFilePath = "D:\\workspace\\dmdbms\\data\\DAMENG\\bak";
        String primaryArchLogSyncReplayPath = "D:\\workspace\\dmdbms\\sync\\DAMENG";

        primary = new DatabaseInstance();
        primary.setSshHost(primarySshHost);
        primary.setSshPort(primarySshPort);
        primary.setSshUsername(primarySshUsername);
        primary.setSshPassword(primarySshPassword);

        primary.setDatabaseHost(primaryDatabaseHost);
        primary.setDatabasePort(primaryDatabasePort);
        primary.setDatabaseUsername(primaryDatabaseUsername);
        primary.setDatabasePassword(primaryDatabasePassword);

        primary.setArchLogFilePath(primaryArchLogFilePath);
        primary.setArchLogSyncReplayPath(primaryArchLogSyncReplayPath);

        String standbySshHost = "127.0.0.1";
        int standbySshPort = 22;
        String standbySshUsername = "Administrator";
        String standbySshPassword = ".12121122.";
        String standbyDatabaseUsername = "SYSDBA";
        String standbyDatabasePassword = "SYSDBA";
        String standbyDatabaseHost = "127.0.0.1";
        String standbyDatabasePort = "52362";
        String standbyArchLogFilePath = "D:\\workspace\\dmdbms\\data\\DAMENG2\\bak";
        String standbyArchLogSyncReplayPath = "D:\\workspace\\dmdbms\\sync\\DAMENG2";

        standby = new DatabaseInstance();
        standby.setSshHost(standbySshHost);
        standby.setSshPort(standbySshPort);
        standby.setSshUsername(standbySshUsername);
        standby.setSshPassword(standbySshPassword);

        standby.setDatabaseHost(standbyDatabaseHost);
        standby.setDatabasePort(standbyDatabasePort);
        standby.setDatabaseUsername(standbyDatabaseUsername);
        standby.setDatabasePassword(standbyDatabasePassword);

        standby.setArchLogFilePath(standbyArchLogFilePath);
        standby.setArchLogSyncReplayPath(standbyArchLogSyncReplayPath);
    }


    @Scheduled(fixedDelay = 1 * 1000)
    public void checkDatabaseInstanceStatus() {
        try {
            DatabaseInstanceBaseParam primaryParam = new DatabaseInstanceBaseParam()
                    .setSystemType(SystemTypeEnums.WINDOWS)
                    .setSshHost(primary.getSshHost())
                    .setSshPort(primary.getSshPort())
                    .setSshUsername(primary.getSshUsername())
                    .setSshPassword(primary.getSshPassword())
                    .setDatabasePort(Integer.parseInt(primary.getDatabasePort()));
            Boolean up = databaseInstanceService.isUp(primaryParam);
            if (!up) {
                long startTime = System.currentTimeMillis();
                log.warn("remote dm database instance is down, prepare to upgrade standby instance to primary...");
                // 停止同步重演调度任务（定时任务调度需要支持可中途取消）
                stop.set(true);
                // 最后一次同步
                lock.lock();
                log.info("execute the last time sync and replay task");
                archiveFileService.sync();
                archiveFileService.replay(ARCHIVE_FILE_DIR);
                // 关闭实例
                DatabaseInstanceBaseParam param = new DatabaseInstanceBaseParam()
                        .setSystemType(SystemTypeEnums.WINDOWS)
                        .setSshHost(standby.getSshHost())
                        .setSshPort(standby.getSshPort())
                        .setSshUsername(standby.getSshUsername())
                        .setSshPassword(standby.getSshPassword())
                        .setDatabaseHost(standby.getDatabaseHost())
                        .setDatabasePort(Integer.valueOf(standby.getDatabasePort()))
                        .setDatabaseUsername(standby.getDatabaseUsername())
                        .setDatabasePassword(standby.getDatabasePassword())
                        .setServiceName("DmServiceDMSERVER2")
                        .setMode("PRIMARY")
                        .setStatus("OPEN FORCE");
                Boolean shutdown = databaseInstanceService.shutdown(param);
                // 清空归档日志目录
                if (shutdown) {
                    log.info("database instance '{}' shutdown, prepare to clean arch files", param.getServiceName());
                    File dir = Paths.get(standby.getArchLogFilePath()).toFile();
                    for (File file : dir.listFiles()) {
                        file.delete();
                    }
                    log.info("clean arch file exist in dir '{}'", dir.getAbsolutePath());
                } else {
                    throw new RuntimeException(String.format("failed to shutdown database instance:'%s'", param.getServiceName()));
                }
                // 重启实例
                Boolean start = databaseInstanceService.start(param);
                if (!start) {
                    throw new RuntimeException(String.format("failed to start database instance:'%s'", param.getServiceName()));
                }
                log.info("database instance '{}' started, prepare to upgrade mode", param.getServiceName());
                // 修改模式为primary开始提供服务
                Boolean alter = databaseInstanceService.alterModeAndStatus(param);
                if (!alter) {
                    throw new RuntimeException(String.format("failed to alter database instance '%s' mode and status", param.getServiceName()));
                }

                //主从参数置换
                DatabaseInstance temp = standby;
                standby = primary;
                primary = temp;
                // 标记主库状态为下线

                log.info("database instance '{}' upgrade to mode '{}' and status '{}'", param.getServiceName(), param.getMode(), param.getStatus());
                log.info("database instance '{}' auto switch to primary finish, end in {} ms.", param.getServiceName(), System.currentTimeMillis() - startTime);
            } else {
                log.info("remote dm database instance is up...");
                // 非normal的数据库为什么重启初始状态为mount了，因为如果为open，挂了重启的瞬间可能存在被写入的风险
                // 检测实例是否下过线
                // 数据库模式调整为normal

                // 启动反向归档日志同步重演任务

            }
        } catch (Exception e) {
            log.error("[Scheduler]failed to check database instance status.", e);
        }
    }
}