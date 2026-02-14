package com.schedule.job.common.config;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载业务数据库配置
 */
@Slf4j
@Configuration
public class JdbcConfigLoader {
    private static final String CONFIG_PATH = "jdbc-config.xml";
    private static Map<String, String> configMap = new HashMap<>();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        // 读取XML文件
        try (InputStream inputStream = JdbcConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_PATH)) {

            SAXReader reader = new SAXReader();
            Document doc = reader.read(inputStream);
            Element root = doc.getRootElement();

            // 解析数据库连接信息
            Element dbInfo = root.element("db-info");
            configMap.put("driverClass", dbInfo.elementText("driver-class"));
            configMap.put("url", dbInfo.elementText("url"));
            configMap.put("username", dbInfo.elementText("username"));
            configMap.put("password", dbInfo.elementText("password"));

            // 解析连接池配置
            Element poolConfig = root.element("pool-config");
            configMap.put("maxPoolSize", poolConfig.elementText("max-pool-size"));
            configMap.put("minIdle", poolConfig.elementText("min-idle"));
            configMap.put("maxWait", poolConfig.elementText("max-wait"));

            log.info("JDBC配置加载完成：{}", configMap);
        } catch (IOException | DocumentException e) {
            log.error("读取数据库XML配置文件失败", e);
            throw new RuntimeException(e);
        }
    }

    public static String getConfig(String key) {
        return configMap.get(key);
    }
}
