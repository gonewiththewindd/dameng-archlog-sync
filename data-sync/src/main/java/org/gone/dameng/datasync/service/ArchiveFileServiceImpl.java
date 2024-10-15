package org.gone.dameng.datasync.service;

import com.dameng.logmnr.LogmnrDll;
import com.dameng.logmnr.LogmnrRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.gone.dameng.datasync.enums.OperationCodeEnums;
import org.gone.dameng.datasync.enums.RedisKeyEnums;
import org.gone.dameng.datasync.model.RedoLogFile;
import org.gone.dameng.datasync.service.def.ArchiveFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArchiveFileServiceImpl implements ArchiveFileService {

    public static final String RSYNC_EXE_PATH = "D:\\Users\\Desktop\\cwrsync_6.3.1_x64_free\\bin\\rsync.exe";
    public static final String SRC_PATH = "/cygdrive/d/workspace/dmdbms/data/DAMENG/bak";
    public static final String DEST_PATH = "/cygdrive/d/workspace/dmdbms/sync/DAMENG";
    public static final Integer LOGMNR_PARSE_BATCH_SIZE = 100000;
    public static final Integer REPLAY_BATCH_SIZE = 100000;

    private Map<String, RedoLogFile> redoLogFileCache = new HashMap<>();

    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public static final Set<Integer> DML_DDL = new HashSet<>(Arrays.asList(
            OperationCodeEnums.INSERT.getCode(),
            OperationCodeEnums.DELETE.getCode(),
            OperationCodeEnums.UPDATE.getCode(),
            OperationCodeEnums.DDL.getCode()
    ));

    @Override
    public void sync() {
        try {
            long start = System.currentTimeMillis();
            String[] cmd = {RSYNC_EXE_PATH, "-avz", "--delete", SRC_PATH, DEST_PATH};
            log.info("[Synchronizer]sync archive file, cmd:{}", Arrays.toString(cmd));
            Process rsync = new ProcessBuilder().command(cmd).start();
            rsync.waitFor();

            byte[] bytes = IOUtils.readFully(rsync.getInputStream(), rsync.getInputStream().available());
            String result = new String(bytes);
            log.info("[Synchronizer]sync archive file...result:\n{}using {}ms", result, System.currentTimeMillis() - start);
            rsync.destroy();
        } catch (Exception e) {
            log.error("[Synchronizer]sync archive file error", e);
        }
    }

    @Override
    public void replay(String dir) {
        // 加载归档文件
        File[] files = Paths.get(dir).toFile().listFiles();
        if (Objects.isNull(files) || files.length == 0) {
            return;
        }
        // 设置类库路径和初始化
        List<File> sortedFiles = Arrays.stream(files).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());
        for (File file : sortedFiles) {
            long connectId = initAndCreateConnection();
            try {
                ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                // 获取上次重演scn
                Number lastScn = Optional.ofNullable((Number) valueOperations.get(RedisKeyEnums.REPLAY_LAST_SCN.format())).orElse(Long.valueOf(0));
                RedoLogFile redoLogFile = redoLogFileCache.get(file.getAbsolutePath());
                if (Objects.nonNull(redoLogFile) && redoLogFile.skipParse()) {
                    // 该redo log文件解析过之后未被修改
                    continue;
                }
                long start = System.currentTimeMillis();
                // 解析归档文件
                List<LogmnrRecord> rawList = loadAndParseRedoLogFile(connectId, file);
                if (CollectionUtils.isEmpty(rawList)) {
                    return;
                }
                // 加载历史待提交记录
                List<LogmnrRecord> pendingList = (List<LogmnrRecord>) Optional.ofNullable(
                        valueOperations.get(RedisKeyEnums.REPLAY_PENDING_LIST.format())).orElse(Collections.emptyList());
                // 合并历史待提交记录
                List<LogmnrRecord> recordList = mergeHistoryPendingRecords(rawList, pendingList, lastScn);
                // 确定可重演的scn列表（事务已提交）
                List<LogmnrRecord> commitRecords = recordList.stream()
                        .filter(r -> r.getCommitScn() != -1)
                        .sorted(Comparator.comparing(LogmnrRecord::getScn))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(commitRecords)) {
                    // 进行重演
                    long scn = doReplay(recordList, commitRecords, pendingList, lastScn);
                    log.info("[Replay]replay redo log end in {}ms, replay sql count:{}, scn progress:{}", System.currentTimeMillis() - start, commitRecords.size(), scn);
                }
            } finally {
                if (connectId > 0) {
                    releaseLogmnr(connectId);
                }
            }
        }
    }

    private static long initAndCreateConnection() {
        System.setProperty("dm.library.path", "D:\\workspace\\dmdbms\\bin");
        LogmnrDll.initLogmnr();
        long connectId = LogmnrDll.createConnect("localhost", 52362, "SYSDBA", "SYSDBA");
        LogmnrDll.setAttr(connectId, LogmnrDll.LOGMNR_ATTR_PARALLEL_NUM, 12);
        LogmnrDll.setAttr(connectId, LogmnrDll.LOGMNR_ATTR_BUFFER_NUM, 512);
        LogmnrDll.setAttr(connectId, LogmnrDll.LOGMNR_ATTR_CONTENT_NUM, 1024);
        return connectId;
    }


    private List<LogmnrRecord> loadAndParseRedoLogFile(long connectId, File file) {

        long start = System.currentTimeMillis();

        long lastModifiedTime = 0;
        try {
            lastModifiedTime = Files.getLastModifiedTime(file.toPath()).toMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        List<LogmnrRecord> rawList = doLoadRedoLog(connectId, file);

        long startScn = 0, endScn = 0;
        if (CollectionUtils.isNotEmpty(rawList)) {
            startScn = rawList.stream().mapToLong(LogmnrRecord::getScn).min().getAsLong();
            endScn = rawList.stream().mapToLong(LogmnrRecord::getScn).max().getAsLong();
        }
        RedoLogFile redoLogFile = new RedoLogFile()
                .setAbsolutePath(file.getAbsolutePath())
                .setLastModifiedTime(lastModifiedTime)
                .setStartScn(startScn)
                .setEndScn(endScn);
        redoLogFileCache.put(file.getAbsolutePath(), redoLogFile);
        log.info("[Replay]load and parse redo log file '{}({})' end in {}ms, produce {} records", file.getName(), byteToMB(file.length()), System.currentTimeMillis() - start, rawList.size());

        return rawList;
    }

    private static List<LogmnrRecord> doLoadRedoLog(long connectId, File file) {
        int i = LogmnrDll.addLogFile(connectId, file.getAbsolutePath(), 1);
        int i1 = LogmnrDll.startLogmnr(connectId, -1, null, null);

        List<LogmnrRecord> rawList = new ArrayList<>(LOGMNR_PARSE_BATCH_SIZE);
        LogmnrRecord[] records;
        do {
            // 这里存在调用部分失败的情况
            records = LogmnrDll.getData(connectId, Integer.MAX_VALUE);

            List<LogmnrRecord> recordList = Arrays.stream(records)
                    .filter(r -> r.getRollBack() == 0)
                    .filter(r -> DML_DDL.contains(r.getOperationCode()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(recordList)) {
                rawList.addAll(recordList);
            }
        } while (Objects.nonNull(records) && records.length >= LOGMNR_PARSE_BATCH_SIZE);
        LogmnrDll.endLogmnr(connectId, 0);

        return rawList;
    }

    private String byteToMB(long length) {
        return ((float) length) / 1024 / 1024 + "MB";
    }

    public static void main(String[] args) throws IOException {

        try (InputStream bufferedReader = Files.newInputStream(Paths.get("D:\\Users\\Desktop\\string_value.txt"))) {
            long l = System.currentTimeMillis();
            byte[] bytes = IOUtils.readFully(bufferedReader, bufferedReader.available());
            List deserialize = new GenericJackson2JsonRedisSerializer().deserialize(bytes, List.class);
            log.info("{}", System.currentTimeMillis() - l);
            System.out.println();
        }
    }

    private List<LogmnrRecord> mergeHistoryPendingRecords(List<LogmnrRecord> records, List<LogmnrRecord> pendingList, Number lastScn) {
        Set<Long> pendingScnList = pendingList.stream().map(LogmnrRecord::getScn).collect(Collectors.toSet());
        // 仅保留未回滚的DML 和 DDL记录
        List<LogmnrRecord> recordList = records.stream()
                .filter(r -> r.getScn() > lastScn.longValue() || pendingScnList.contains(r.getScn())) // 去重
                .collect(Collectors.toList());
        // 合并历史待提交记录
        if (CollectionUtils.isNotEmpty(pendingList)) {
            Set<Long> scnSet = recordList.stream().map(LogmnrRecord::getScn).collect(Collectors.toSet());
            Map<String, Long> txCommitScnMap = recordList.stream()
                    .collect(Collectors.toMap(LogmnrRecord::getXid, v -> v.getCommitScn(), (v1, v2) -> v1));
            pendingList.forEach(pendingScn -> {
                if (!scnSet.contains(pendingScn.getScn())) {
                    // 数据修复，不清楚事务跨日志提交（事务操作记录在A，事务提交记录在B）是否会同步更新所有文件的commitScn，所以同意针对历史待提交记录进行数据修复
                    if (pendingScn.getCommitScn() == -1 && txCommitScnMap.containsKey(pendingScn.getXid())) {
                        pendingScn.setCommitScn(txCommitScnMap.get(pendingScn.getXid()));
                    }
                    recordList.add(pendingScn);
                }
            });
        }
        return recordList;
    }

    private long persistPendingListAndLastScn(List<LogmnrRecord> recordList, List<LogmnrRecord> commitRecords, Number lastScn) {
        // TODO redis transaction

        // 未提交记录列表
        List<LogmnrRecord> pendingList = recordList.stream()
                .filter(r -> r.getCommitScn() == -1)
                .collect(Collectors.toList());
        redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_PENDING_LIST.format(), pendingList);
        OptionalLong max = commitRecords.stream().mapToLong(LogmnrRecord::getScn).max();
        // 更新已重演的scn最大值
        if (max.isPresent()) {
            redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_LAST_SCN.format(), max.getAsLong());
        }
        return max.isPresent() ? max.getAsLong() : lastScn.longValue();
    }

    /**
     * 本地数据库+缓存数据库 事务联动
     * // TODO redis transaction
     *
     * @param recordList
     * @param commitRecords
     * @param lastPendingList
     * @param lastScn
     * @return
     * @throws SQLException
     */
    private long doReplay(List<LogmnrRecord> recordList, List<LogmnrRecord> commitRecords, List<LogmnrRecord> lastPendingList, Number lastScn) {
        try (Connection connection = sqlSessionFactory.openSession().getConnection()) {
            try {
                connection.setAutoCommit(false);

                Statement statement = connection.createStatement();
                for (int i = 0; i < commitRecords.size(); i++) {
                    statement.addBatch(commitRecords.get(i).getSqlRedo());
                }
                statement.executeBatch();

                // TODO 可能会遇到操作系统内存不足，导致redis指令执行失败
                long retry = 3, scn = lastScn.longValue();
                while (retry > 0) {
                    try {
                        // 未提交记录列表
                        List<LogmnrRecord> pendingList = recordList.stream()
                                .filter(r -> r.getCommitScn() == -1)
                                .collect(Collectors.toList());
                        redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_PENDING_LIST.format(), pendingList);
                        scn = commitRecords.stream().mapToLong(LogmnrRecord::getScn).max().getAsLong();
                        redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_LAST_SCN.format(), scn);
                        break;
                    } catch (Exception e) {
                        retry--;
                        log.warn(e.getMessage(), e);
                    }
                }

                if (retry == 0) {
                    log.error("[Replay]replay failed due to redis cache execute failure.see the warning log detail");
                    // TODO 如果是由于内存不足引起的失败，这里进行重置大概率还是失败
                    redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_LAST_SCN.format(), lastScn.longValue());
                    redisTemplate.opsForValue().set(RedisKeyEnums.REPLAY_PENDING_LIST.format(), lastPendingList);
                    throw new RuntimeException("failed due to redis cache execute failure");
                } else {
                    connection.commit();
                }
                // 保存待提交记录和本轮scn重演进度
                return scn;
            } catch (SQLException e) {
                log.error("[Replay]rollback due to exception:", e);
                connection.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void releaseLogmnr(long connectId) {
/*        int ec = LogmnrDll.endLogmnr(connectId, 1);
        if (ec != 0) {
            log.error("[Replay]failed to end log analyze, error code:{}", ec);
        }*/
        int cc = LogmnrDll.closeConnect(connectId);
        if (cc != 0) {
            log.error("[Replay]failed to close connection, error code:{}", cc);
        }
        int dc = LogmnrDll.deinitLogmnr();
        if (dc != 0) {
            log.error("[Replay]failed to destroy log mnr, error code:{}", dc);
        }
    }
}
