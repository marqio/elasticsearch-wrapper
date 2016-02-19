package com.baidu.siem.config;

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * //提供一些Properties的公共方法
 * Created by yuxuefeng on 15/11/23.
 */
public abstract class AbstractConfig {
    public static final Logger logger = Logger.getLogger(AbstractConfig.class);

    public String getString(String key, String defaultValue) {
        String value = getProperties().getProperty(key);
        if (value != null) {
            return value.trim();
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = getProperties().getProperty(key);
        try {
            if (value != null) {
                return Integer.parseInt(value);
            }
            return defaultValue;
        } catch (Exception e) {
            logger.error("Value of Properties:[" + key + "] format error! return defaultValue instead!");
            return defaultValue;
        }
    }

    public int getPositiveInt(String key, int defaultValue) {
//        logger.warn("Value of the key:[" + key + "] must be positive,or defaultValue:[" + defaultValue + "] will be returned instead");
        String value = getProperties().getProperty(key);
        int v;
        try {
            if (value != null) {
                v = Integer.parseInt(value);
                return v > 0 ? v : defaultValue > 0 ? defaultValue : 1;
            }

            return defaultValue > 0 ? defaultValue : 1;

        } catch (Exception e) {
            logger.error("Value of Properties:[" + key + "] format error! return defaultValue instead!");
            return defaultValue > 0 ? defaultValue : 1;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = getProperties().getProperty(key);
        try {
            if (value != null) {
                return Long.parseLong(value);
            }
            return defaultValue;
        } catch (Exception e) {
            logger.error("Value of Properties:[" + key + "] format error! return defaultValue instead!");
            return defaultValue;
        }
    }

    public long getPositiveLong(String key, long defaultValue) {
//        logger.warn("Value of the key:[" + key + "] must be positive,or defaultValue:[" + defaultValue + "] will be returned instead");
        String value = getProperties().getProperty(key);
        long v;
        try {
            if (value != null) {
                v = Long.parseLong(value);
                return v > 0 ? v : defaultValue > 0 ? defaultValue : 1;
            }

            return defaultValue > 0 ? defaultValue : 1;

        } catch (Exception e) {
            logger.error("Value of Properties:[" + key + "] format error! return defaultValue instead!");
            return defaultValue > 0 ? defaultValue : 1;
        }
    }


    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperties().getProperty(key);

        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        logger.error("Value of Properties:[" + key + "] not exsits or format error! return defaultValue instead!");
        return defaultValue;

    }

    public String[] getStringArray(String key, String defaultValue, String regex) {
        String value = getProperties().getProperty(key);
        try {
            if (value != null) {
                return value.trim().split(regex);
            }
            return defaultValue.split(regex);
        } catch (Exception e) {
            logger.error("Value of Properties:[" + key + "] format error! return defaultValue instead!");
            return defaultValue.split(regex);
        }
    }

    public abstract Properties getProperties();
}
