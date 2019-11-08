package com.cn.datasource.datasource.intercepter;


import com.cn.datasource.datasource.annotation.EnableAutoDynamicDataSource;
import com.cn.datasource.datasource.annotation.TargetDataSource;
import com.cn.datasource.datasource.context.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源拦截器，针对设置数据源的接口进行动态拦截，数据处理后还原数据源默认设置
 * @author 李涛
 * @Date 2019年11月5日
 */
@Aspect
@Order
@Component
@Slf4j
@ConditionalOnBean (annotation = {EnableAutoDynamicDataSource.class})
public class DynamicDataSourceAspect  {
    @Before (value = "@annotation(com.cn.datasource.datasource.annotation.TargetDataSource) && @annotation(targetDataSource)")
    public void changeDataSource(JoinPoint joinPoint, TargetDataSource targetDataSource) {
    String dataSourceName = targetDataSource.dataSourceName();
    //判断是否包含当前数据源
    if (!DynamicDataSourceContextHolder.isContainsDataSource(dataSourceName)) {
        log.error("数据源 " + dataSourceName + " 不存在使用默认的数据源 -> " + joinPoint.getSignature());
    } else {
        log.debug("使用数据源：" + dataSourceName);
        DynamicDataSourceContextHolder.setDataSourceType(dataSourceName);
    }
}

        @After (value = "@annotation(com.cn.datasource.datasource.annotation.TargetDataSource) && @annotation(targetDataSource)")
        @AfterThrowing (value = "@annotation(com.cn.datasource.datasource.annotation.TargetDataSource) && @annotation(targetDataSource)")
        public void clearDataSource(JoinPoint joinPoint, TargetDataSource targetDataSource) {
            log.debug("清除数据源 " + targetDataSource.dataSourceName() + " !");
            DynamicDataSourceContextHolder.clearDataSource();
        }
}