package com.fishing.annotation;

import java.lang.annotation.*;


@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisKeyField {

    /**
     * 拼接顺序
     * @return
     */
    int order();
}
