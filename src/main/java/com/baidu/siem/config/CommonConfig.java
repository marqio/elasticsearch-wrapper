package com.baidu.siem.config;

import com.baidu.siem.exception.InvokeException;
import com.baidu.siem.model.Consist;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by yuxuefeng on 15/11/16.
 */
//@Component
public class CommonConfig extends AbstractConfig {

    //kv对象
    public static final Properties properties = new Properties();
    public static final CommonConfig commonConfig = new CommonConfig();
    //默认集群名
    public static String defaultClusterName = "SSD-TEST-ES";
    //默认请求超时时间
    public static long defaultTimeout = 60000;
    //默认慢操作记录时间
    public static long defaultSlowTimeout = 3000;
    //默认日志路径
    public static String defaultLogDir = "/home/work/siem/";

    public synchronized static void updateCommonFile() throws InvokeException {
        try {
            logger.info("Start to update [wrapper-common] configuration!");
            CommonConfig.properties.load(CommonConfig.class.getClassLoader().getResourceAsStream(Consist.DEFAULT_COMMON_FILE_NAME));
            logger.info("Congratulations!Update [wrapper-common] configuration completed sucessfully!");

        } catch (IOException e) {
            logger.error("the configuration file:[" + Consist.DEFAULT_COMMON_FILE_NAME + "] IOException,please retry", e);
            throw new InvokeException("the configuration file:[" + Consist.DEFAULT_COMMON_FILE_NAME + "] IOError,please retry", 505);
        } catch (NullPointerException e) {
            logger.error("the configuration file:[" + Consist.DEFAULT_COMMON_FILE_NAME + "] does not exsit,please check!", e);
            throw new InvokeException("the configuration file:[" + Consist.DEFAULT_COMMON_FILE_NAME + "] does not exsit,please check!", 506);
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    //加载commonConfig
    static {
        updateCommonFile();
    }
}