package org.gone.dameng.datasync.schedule;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gone.dameng.datasync.model.di.req.DatabaseInstanceBaseParam;
import org.gone.dameng.datasync.service.def.ArchiveFileService;
import org.gone.dameng.datasync.service.def.DatabaseInstanceService;
import org.gone.dameng.datasync.utils.SshUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
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
    @Autowired
    private RedisTemplate redisTemplate;

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

    @Scheduled(fixedDelay = 1 * 1000)
    public void replayArchiveFile() {
        try {
            // 归档日志重演
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("[Scheduler]acquire lock failed, skip");
                return;
            }
            archiveFileService.sync();
            archiveFileService.replay(ARCHIVE_FILE_DIR);
        } catch (Exception e) {
            log.error("[Scheduler]replay archive file error", e);
        } finally {
            lock.unlock();
        }
    }

    //    @Scheduled(fixedDelay = 1 * 1000)
    public void checkDatabaseInstanceStatus() {
        try {
            String primaryDatabaseHost = "127.0.0.1";
            int primarySshPort = 22;
            String primarySshUsername = "Administrator";
            String primarySshPassword = ".12121122.";
            String primaryDatabasePort = "52361"; // TODO：端口号最好使用5位数，不然有重复的可能，比如52362端口就包含了523 5236 5362等端口号
            int defaultTimeoutSeconds = 30;
            String instanceStatusCommand = "netstat -an | findstr ${port} | findstr LISTENING".replace("${port}", primaryDatabasePort);
//            String statusCommand = "ss -l | grep ${port}".replace("${port}", primaryDatabasePort);

            String standbyDatabaseUsername = "SYSDBA";
            String standbyDatabasePassword = "SYSDBA";
            String standbyDatabaseHost = "127.0.0.1";
            String standbyDatabasePort = "52362";

            String s = SshUtils.execRemoteCommand(primaryDatabaseHost, primarySshPort, primarySshUsername, primarySshPassword, instanceStatusCommand, defaultTimeoutSeconds);
            if (StringUtils.isBlank(s)) {
                log.warn("[Scheduler]remote dm database instance is down, prepare to upgrade standby instance to primary...");
                // 主从参数置换

                // 标记主库状态为下线

                // 停止同步重演调度任务（定时任务调度需要支持可中途取消）

                // 关闭实例
//                databaseInstanceService.shutdown();
                // 清空归档日志目录

                // 重启实例
//                databaseInstanceService.up();
                // 修改模式为primary开始提供服务
                DatabaseInstanceBaseParam param = new DatabaseInstanceBaseParam()
                        .setDatabaseHost(primaryDatabaseHost)
                        .setDatabasePort(Integer.valueOf(standbyDatabasePort))
                        .setDatabaseUsername(standbyDatabaseUsername)
                        .setDatabasePassword(standbyDatabasePassword)
                        .setMode("PRIMARY")
                        .setStatus("OPEN FORCE");
                Boolean b = databaseInstanceService.alterModeAndStatus(param);
                log.info("[Scheduler]instance upgrade result:{}", b);
            } else {
                log.info("[Scheduler]remote dm database instance is up...");
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