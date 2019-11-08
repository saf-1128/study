package com.cn.datasource.datasource.context;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源处理上下文
 * @author litao
 * @date 2019年11月5日
 */
@Slf4j
public class DynamicDataSourceContextHolder {
    /**
     * 指定默认数据源
     */
    public final  static String DEFAULT_DATASOURCE_NAME="dataSource";
    /**
     * 存储当前使用的数据源类型
     */
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();
    /**
     * 存储系统已生效数据源信息
     * key:数据源类型
     * value:已初始化的数据源
     */
    private static final Map<String, DataSource> datasourceStorage=new ConcurrentHashMap<>();
    /**
     * 获取当前数据源
     */
    public static String getCurrentDataSourceType() {
        return contextHolder.get();
    }
    /**
     * 设置数据源
     */
    public static void setDataSourceType(String dataSourceType) {
        contextHolder.set(dataSourceType);
    }
    /**
     * 清除数据源
     */
    public static void clearDataSource() {
        contextHolder.remove();
    }
    /**
     * 获取数据源存储对象
     */
    public static Map<String, DataSource> getDatasourceStorage(){
        return datasourceStorage;
    }

    /**
     * 判断是否包含当前数据源
     * @param datasourceName
     * @return
     */
    public static boolean isContainsDataSource(String datasourceName){
        return datasourceStorage.containsKey(datasourceName);
    }
}

