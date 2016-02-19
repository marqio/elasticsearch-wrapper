package com.baidu.siem.watcher;

import java.nio.file.WatchEvent.Kind;

/**
 * 文件事件实体类
 * Created by yuxuefeng on 15/9/23.
 */
public class FileSystemEvent {
    private final String fileName;
    private final Kind<?> kind;

    public FileSystemEvent(String fileName, Kind<?> kind) {
        this.fileName = fileName;
        this.kind = kind;
    }

    /**
     * 文件的路径
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 操作类型：变更、创建、删除
     */
    @SuppressWarnings("rawtypes")
    public Kind getKind() {
        return kind;
    }
}
