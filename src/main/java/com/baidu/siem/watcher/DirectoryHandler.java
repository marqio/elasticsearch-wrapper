package com.baidu.siem.watcher;

import com.baidu.siem.config.CommonConfig;
import com.baidu.siem.model.Consist;
import com.baidu.siem.throttle.EsThrottleFactory;
import org.apache.log4j.Logger;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Created by yuxuefeng on 15/9/23.
 */
public class DirectoryHandler implements Observer {
    private static Logger logger = Logger.getLogger(Observer.class);
    private ConcurrentMap<String, Long> lastmodifies = new ConcurrentHashMap<>();

    @Override
    public synchronized void update(Observable o, Object event) {
        FileSystemEvent ev = (FileSystemEvent) event;
        String fileName = ev.getFileName();
        Long lastTime = lastmodifies.get(fileName);
        //delete throttle ignore
        if (ENTRY_DELETE.equals(ev.getKind())) {
            //删除事件
//            EsThrottleFactory.deleteByClusterName(fileName.substring(0, fileName.lastIndexOf(".")));
            return;
        }
        //十毫秒内，同一文件的修改无效
        if (lastTime != null) {
            if (System.currentTimeMillis() - lastTime < 10) {
                logger.warn("Please do not remodify the same file:[" + fileName + "] to many times in 10 ms");
                return;
            }
        }
        lastmodifies.put(fileName, System.currentTimeMillis());
        //update commonConfig
        if (Consist.DEFAULT_COMMON_FILE_NAME.equals(fileName)) {
            CommonConfig.updateCommonFile();
            return;
        }

        //update throttle
        EsThrottleFactory.updateByClusterName(fileName.substring(0, fileName.lastIndexOf(".")), true);
    }
}
