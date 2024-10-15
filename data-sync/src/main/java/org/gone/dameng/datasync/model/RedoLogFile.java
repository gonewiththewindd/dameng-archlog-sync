package org.gone.dameng.datasync.model;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

@Data
@Accessors(chain = true)
public class RedoLogFile {

    private String filename;
    private String absolutePath;
    private long lastModifiedTime;

    private long startScn;
    private long endScn;

    @SneakyThrows
    public boolean skipParse() {
        FileTime lastModifiedTime = Files.getLastModifiedTime(Paths.get(absolutePath));
        return this.getLastModifiedTime() == lastModifiedTime.toMillis();
    }
}
