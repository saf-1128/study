package com.cn.datasource.datasource.annotation;


import com.cn.datasource.datasource.register.DynamicDataSourceRegister;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 能够自动注入数据源
 * @author litao
 * @date 2019年11月5日
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Import(DynamicDataSourceRegister.class)
public @interface EnableAutoDynamicDataSource {
}
