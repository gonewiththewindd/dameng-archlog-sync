package org.gone.dameng.datasync.service.def;

public interface ArchiveFileService {

    void sync();

    void replay(String dir);

}
