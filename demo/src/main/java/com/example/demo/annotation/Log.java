package com.fishing.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 请求接口名称
     *
     * @return
     */
    String name() default "default名称";
}


