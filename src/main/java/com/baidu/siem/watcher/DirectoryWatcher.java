package com.baidu.siem.watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 监控一个目录内文件的更新、创建和删除事件（不包括子目录）
 *
 * @author yuxuefeng on 15/9/23.
 */
public class DirectoryWatcher extends Observable {

    private WatchService watcher;
    private Path path;
    private WatchKey key;
    private Executor executor = Executors.newSingleThreadExecutor();//由线程池管理线程的生命周期

    //事件处理
    FutureTask<Integer> task = new FutureTask<Integer>(
            new Callable<Integer>() {
                public Integer call() throws InterruptedException {
                    processEvents();
                    return Integer.valueOf(0);
                }
            });

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public DirectoryWatcher(String dir) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        path = Paths.get(dir);
        //    监控目录内文件的更新、创建和删除事件
        key = path.register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
    }

    /**
     * 启动监控过程
     */
    public void execute() {
        // 通过线程池启动一个额外的线程加载Watching过程
        executor.execute(task);
    }

    /**
     * 关闭后的对象无法重新启动
     *
     * @throws IOException
     */
    public void shutdown() throws IOException {
        watcher.close();
        executor = null;
    }

    /**
     * 监控文件系统事件
     */
    void processEvents() {
        while (true) {
            // 等待直到获得事件信号
            WatchKey signal;
            try {
                signal = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : signal.pollEvents()) {
                Kind<?> kind = event.kind();

                // how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context of directory  event is the  individual file
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();

                //handle the  individual file
                notifiy(name.getFileName().toString(), kind);
            }
            //为监控下一个通知做准备
            key.reset();
        }
    }

    /**
     * 通知外部各个Observer目录有新的事件更新
     */
    void notifiy(String fileName, Kind<?> kind) {
        //过滤配置文件
        if (fileName.endsWith(".properties")) {
            // 标注目录已经被做了更改,以便外部observers知晓
            setChanged();

            //    这里采用的是观察者模式的“推”方式，主动通知各个观察者目标对象状态的变更
            notifyObservers(new FileSystemEvent(fileName, kind));
        }
    }
}