package com.cn.datasource.datasource.annotation;

import com.cn.datasource.datasource.context.DynamicDataSourceContextHolder;
import java.lang.annotation.*;

/**
 * 作用于方法上，用于拦截controler方法，更换数据源
 * @author litao
 * @date 2019年11月5日
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {

    String dataSourceName() default DynamicDataSourceContextHolder.DEFAULT_DATASOURCE_NAME;
}