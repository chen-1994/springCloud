
package com.fishing.annotation;

import java.lang.annotation.*;


/**
 * redis 动态缓存读取
 * 如果数据不存在则存储返回值到redis
 * 如果redis存在数据，则直接返回
 * attribute key支持# 字符el表达式
 *
 * <code>
 * \@RedisCacheable(key = "#userId",timeout = 60)
 * public User queryUserInfo(Integer userId){
 * ...
 * }
 * </code>
 *
 * @author licl
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheable {
    /**
     * redis key
     * # 字符支持 EL表达式
     */
    String key();

    /**
     * type 类型：0全局1个人
     */
    int type() default 1;

    /**
     * 超时时间秒数 second
     */
    long timeout() default 100;

    /**
     * 接口描述
     */
    String describe() default "Interface description";

    /**
     * 自定义处理
     * 用于处理数据存储前的数据格式
     */
    Class<? extends AbstractRedisCacheProccessor> process() default DefaultRedisCacheProccessor.class;
}
