package com.cn.datasource.datasource.register;


import com.cn.datasource.datasource.DynamicDataSource;
import com.cn.datasource.datasource.context.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Preconditions;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 注册动态数据源
 * 初始化数据源和提供了执行动态切换数据源的工具类
 * EnvironmentAware（获取配置文件配置的属性值）
 * @author litao
 * @date 2019年11月5日
 */
@Slf4j
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    //指定默认数据源(springboot2.0默认数据源是hikari如何想使用其他数据源可以自己配置)
    private static final String DATASOURCE_TYPE_DEFAULT = "com.zaxxer.hikari.HikariDataSource";
    /**
     * 默认数据源
     */
    private DataSource defaultDataSource;
    /**
     * 数据源分割符
     */
    private final static String DATASOURCE_SEPARATION_CHARACTER=",";
    /**
     * 存储系统已生效自定义数据源信息
     * key:数据源类型
     * value:已初始化的数据源
     */
    private static final Map<String, DataSource> datasourceStorage=new ConcurrentHashMap<>();

    @Override
    public void setEnvironment(Environment environment) {
        try{
            initDefaultDataSource(environment);
            initOtherDataSources(environment);
        }catch (Exception e){
            log.error("数据源初始化异常",e);
        }
    }

    private void initDefaultDataSource(Environment env) throws Exception {
        // 读取主数据源
        String driver=env.getProperty("spring.datasource.driver-class-name");
        Preconditions.checkArgument(StringUtils.isNoneBlank(driver),"数据源spring.datasource.driver-class-name未配置");
        String userName=env.getProperty("spring.datasource.username");
        Preconditions.checkArgument(StringUtils.isNoneBlank(userName),"数据源spring.datasource.username未配置");
        String url=env.getProperty("spring.datasource.url");
        Preconditions.checkArgument(StringUtils.isNoneBlank(url),"数据源spring.datasource.url未配置");
        String password=env.getProperty("spring.datasource.password");
        Preconditions.checkArgument(StringUtils.isNoneBlank(password),"数据源spring.datasource.password未配置");
        String type=env.getProperty("spring.datasource.type");
        Preconditions.checkArgument(StringUtils.isNoneBlank(type),"数据源spring.datasource.type未配置");
        Map<String, Object> datasourceMap = new HashMap<>(8);
        datasourceMap.put("driver", driver);
        datasourceMap.put("url", url);
        datasourceMap.put("username", userName);
        datasourceMap.put("password", password);
        datasourceMap.put("type", type);
        defaultDataSource = buildDataSource(datasourceMap);
    }

    /**
     * 初始化其他数据源信息
     * @param env
     */
    private void initOtherDataSources(Environment env) throws Exception {
        // 读取配置文件获取更多数据源
        String datasourceNames = env.getProperty("slave.datasource.names");
        for (String datasourceName : StringUtils.split(datasourceNames,DATASOURCE_SEPARATION_CHARACTER)) {
            // 多个数据源
            String driver=env.getProperty("spring.datasource." + datasourceName + ".driver-class-name");
            Preconditions.checkArgument(StringUtils.isNoneBlank(driver),"数据源slave.datasource."+datasourceName+".driver-class-name未配置");
            String userName=env.getProperty("spring.datasource." + datasourceName + ".username");
            Preconditions.checkArgument(StringUtils.isNoneBlank(userName),"数据源slave.datasource."+datasourceName+".username未配置");
            String url=env.getProperty("spring.datasource." + datasourceName + ".url");
            Preconditions.checkArgument(StringUtils.isNoneBlank(url),"数据源slave.datasource."+datasourceName+".url未配置");
            String password=env.getProperty("spring.datasource." + datasourceName + ".password");
            Preconditions.checkArgument(StringUtils.isNoneBlank(password),"数据源slave.datasource."+datasourceName+".password未配置");
            String type=env.getProperty("spring.datasource." + datasourceName + ".type");
            Preconditions.checkArgument(StringUtils.isNoneBlank(type),"数据源slave.datasource."+datasourceName+".type未配置");
            Map<String, Object> dsMap = new HashMap<>(8);
            dsMap.put("driver", driver);
            dsMap.put("url", url);
            dsMap.put("username", userName);
            dsMap.put("password", password);
            dsMap.put("type", type);
            DataSource ds = buildDataSource(dsMap);
            datasourceStorage.put(datasourceName, ds);
        }
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
        //添加默认数据源
        targetDataSources.put(DynamicDataSourceContextHolder.DEFAULT_DATASOURCE_NAME, this.defaultDataSource);
        DynamicDataSourceContextHolder.getDatasourceStorage().put(DynamicDataSourceContextHolder.DEFAULT_DATASOURCE_NAME, this.defaultDataSource);
        //添加其他数据源
        targetDataSources.putAll(datasourceStorage);
        for (String key : datasourceStorage.keySet()) {
            DynamicDataSourceContextHolder.getDatasourceStorage().put(key,datasourceStorage.get(key));
        }

        //创建DynamicDataSource
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);
        //注册 - BeanDefinitionRegistry
        beanDefinitionRegistry.registerBeanDefinition("dataSource", beanDefinition);
        log.info("Dynamic DataSource Registry");
    }

    public DataSource buildDataSource(Map<String, Object> dataSourceMap) throws Exception {
        try {
            Object type = dataSourceMap.get("type");
            if (type == null) {
                /**
                 *  设置默认DataSource
                 */
                type = DATASOURCE_TYPE_DEFAULT;
            }
            Class<? extends DataSource> dataSourceType;
            dataSourceType = (Class<? extends DataSource>) Class.forName((String) type);
            String driverClassName = dataSourceMap.get("driver").toString();
            String url = dataSourceMap.get("url").toString();
            String username = dataSourceMap.get("username").toString();
            String password = dataSourceMap.get("password").toString();
            // 自定义DataSource配置
            DataSourceBuilder factory = DataSourceBuilder.create().driverClassName(driverClassName).url(url)
                    .username(username).password(password).type(dataSourceType);
            return factory.build();
        } catch (ClassNotFoundException e) {
           throw e;
        }
    }
}
