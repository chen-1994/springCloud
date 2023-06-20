package com.fishing.annotation;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fishing.common.constants.RedisKeyConstants;
import com.fishing.utils.Md5Util;
import com.fishing.utils.ReqUtil;
import com.fishing.utils.StringUtil;
import com.google.common.base.CaseFormat;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RedisCacheable aop
 *
 * @author licl
 */
@Log4j2
@Aspect
@Component
public class RedisCacheableAspect {
    /**
     * 实例缓存
     */
    private final static ConcurrentHashMap<String, Object> PROCESS_INSTANCE = new ConcurrentHashMap<>();
    /**
     * 实例方法缓存
     */
    private final static ConcurrentHashMap<String, Method> PROCESS_METHODS = new ConcurrentHashMap<>();

    //全局更新key
    private final static List<String> timeRedisList = Arrays.asList(
            RedisKeyConstants.VIDEO_COMMENT_REPLACE,RedisKeyConstants.ARTICLE_COMMENT_REPLACE,
            RedisKeyConstants.SHORTESSAY_COMMENT_REPLACE
    );

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired(required = false)
    private RedissonClient redissonClient;


    @Pointcut("@annotation(RedisCacheable)")
    private void aroundMethodAspect() {

    }

    @Around(value = "aroundMethodAspect()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        StringBuilder params = new StringBuilder();
        //参数值
        Object[] argValues = joinPoint.getArgs();
        Map<String,Object> paramMap = new HashMap<>();
        //参数名称
        String[] argNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        /*if (argValues != null) {
            for (int i = 0; i < argValues.length; i++) {
//                params.append(argNames[i]).append(":").append(argValues[i]);
                params.append(argNames[i]).append(":").append(JSONObject.toJSONString(argValues[i]));
                paramMap.put(argNames[i],argValues[i]);
            }
        }*/
        /*if (argValues != null) {
            for (int i = 0; i < argValues.length; i++) {
                if (argValues[i].getClass().getName().startsWith("com.fishing")){
                    JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(argValues[i]));
                    params.append(argNames[i]);
                    List<Field> fieldList = new ArrayList<>();
                    for (int j=0;j<fieldList.size();j++){
                        params.append(":").append(fieldList.get(j).getName()).append(":").append(jsonObject.get(fieldList.get(j).getName()));
                    }
                } else {
                    params.append(argNames[i]).append(":").append(argValues[i]);
                }
//                params.append(argNames[i]).append(":").append(argValues[i]);
//                params.append(argNames[i]).append(":").append(JSONObject.toJSONString(argValues[i]));
                paramMap.put(argNames[i],argValues[i]);
            }
        }*/
        String timeRedis = "";
        if (argValues != null) {
            for (int i = 0; i < argValues.length; i++) {
                StringBuilder params1 = new StringBuilder();
                StringBuilder params2 = new StringBuilder();
                if (argValues[i].getClass().getName().startsWith("com.fishing")){
                    JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(argValues[i]));
//                    params.append(argNames[i]);
                    params.append("req");
                    List<Field> fieldList = new ArrayList<>();
                    fieldList(argValues[i].getClass(),fieldList);

                    StringBuilder[] str = new StringBuilder[fieldList.size()];
                    for (int j=0;j<fieldList.size();j++){
                        RedisKeyField redisKeyField = fieldList.get(j).getAnnotation(RedisKeyField.class);
                        if (redisKeyField!=null){
                            str[redisKeyField.order()-1] = new StringBuilder().append(":").append(fieldList.get(j).getName()).append(":").append(jsonObject.get(fieldList.get(j).getName()));
//                            params1.append(":").append(fieldList.get(j).getName()).append(":").append(jsonObject.get(fieldList.get(j).getName()));
                        } else {
                            params2.append(":").append(fieldList.get(j).getName()).append(":").append(jsonObject.get(fieldList.get(j).getName()));
                        }
                    }
                    for (int h=0;h<str.length;h++){
                        if (str[h]!=null){
                            params1.append(str[h]);
                        }
                    }
                    timeRedis = params.append(params1).toString();
                    params.append(params2);

                } else {
                    String valueStr = JSONObject.toJSONString(argValues[i]);
                    if (valueStr.length()>20){
                        valueStr = Md5Util.md5Hex(valueStr)+valueStr.length();
                    }
                    params.append(argNames[i]).append(":").append(valueStr);
                }
//                params.append(argNames[i]).append(":").append(argValues[i]);
//                params.append(argNames[i]).append(":").append(JSONObject.toJSONString(argValues[i]));
                paramMap.put(argNames[i],argValues[i]);
            }
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisCacheable handler = method.getAnnotation(RedisCacheable.class);
//        AssertUtils.isTrue(StringUtils.isNotBlank(handler.key()), "annotation RedisCacheable key cannot be empty");
//        AssertUtils.isTrue(handler.timeout() > 0, "annotation RedisCacheable timeout value must be greater than 0");

        StandardEvaluationContext context = parseElContext(method, joinPoint.getArgs());
        String redisKey = this.getRedisKey(context, handler);
        timeRedis = redisKey + ":" + timeRedis;
        String className = null;
        if (StringUtil.isNullOrEmpty(handler.regularKey())){
            if ("T".equals(redisKey.substring(0,1))){//首字母T为泛型
                ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl)joinPoint.getSourceLocation().getWithinType().getGenericSuperclass();
                className = parameterizedType.getActualTypeArguments()[1].getTypeName();
                String name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, Class.forName(className).getAnnotation(TableName.class).value());
                redisKey = redisKey + name;
                className = null;
            }
            if ("CommonService:getModel:".equals(redisKey)){//CommonService接口getModel缓存
                ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl)joinPoint.getSourceLocation().getWithinType().getGenericSuperclass();
                className = parameterizedType.getActualTypeArguments()[1].getTypeName();
                String name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, Class.forName(className).getAnnotation(TableName.class).value());
                redisKey = redisKey + name;
            }
            if (handler.type()==1){//个人缓存
                redisKey = String.format(redisKey+":%d:%s", ReqUtil.getUserId(),params);
            }
            if (handler.type()==1&&!timeRedisList.contains(timeRedis.substring(0,timeRedis.lastIndexOf(":"))+":%s")){
                timeRedis = String.format(redisKey+":%d", ReqUtil.getUserId());
            }
            if (handler.type()==0){//全局缓存
                redisKey = String.format(redisKey+":%s", params);
            }
        } else {
            redisKey = handler.regularKey();
            String[] ps = handler.regularParamName().split(",");
            String[] values = new String[ps.length];
            for (int i=0;i<ps.length;i++){
                values[i]=paramMap.get(ps[i]).toString();
            }
            redisKey = String.format(redisKey, values);
            if ("list".equals(handler.regularValueType())){
                RList friends = redissonClient.getList(redisKey);
                redisKey = "regularKey:"+redisKey;
                List list = friends.readAll();
                if (list.size()>0){
                    return list;
                } else {
                    Object proceed = joinPoint.proceed();
                    if (null != proceed) {
                        return process(joinPoint, handler, redisKey);
                    }
                    return proceed;
                }
            }
        }

        log.info("redis缓存key:{}",redisKey);

        String result = redisTemplate.opsForValue().get(redisKey);



        if (StringUtils.isNotBlank(result)) {
            //变更返回的当前时间
            String key = "\"currentTime\":";
            int i = result.indexOf(key);
            log.info("是否包含数据产生的时间：{}",i);
            if (i!=-1){
                int l = i + key.length();
                Long time = Long.valueOf(result.substring(l,l+13))-1000;
                String timeStr = redisTemplate.opsForValue().get(timeRedis);
                log.info("time:{} timeStr:{} timeRedis：{}",time,timeStr,timeRedis);
                if (!StringUtil.isNullOrEmpty(timeStr)){
                    //数据时间小于数据更新时间，重新查询
                    if (time<Long.valueOf(timeStr)){
                        Object proceed = joinPoint.proceed();
                        if (null != proceed) {
                            return process(joinPoint, handler, redisKey);
                        }
                        return proceed;
                    }
                }
                //获取key
                if (l>key.length()){
                    result = result.substring(0,l) + System.currentTimeMillis() + result.substring(l+13);
                }
            }

            log.info("【{}】获取缓存数据 data = {}", handler.describe(), result);
            Type genericReturnType = method.getGenericReturnType();
            /*if (genericReturnType instanceof ParameterizedType) {
                Class<?> rawType = (Class<?>) ((ParameterizedType) genericReturnType).getRawType();
                return JSON.parseObject(result, rawType);
            }
            return JSON.parseObject(result, Class.forName(genericReturnType.getTypeName()));*/
            //泛型返回（下面也可以）
            if (className!=null){
                return JSON.parseObject(result, Class.forName(className));
            }
            return resultObject(result,genericReturnType);
        } else {
            Object proceed = joinPoint.proceed();
            if (null != proceed) {
                return process(joinPoint, handler, redisKey);
            }
            return proceed;
        }

    }

    public void fieldList(Class c,List<Field> list){
        if (c.getName().startsWith("com.fishing")){
            list.addAll(new ArrayList<>(Arrays.asList(c.getDeclaredFields())));
        }
        if (c.getSuperclass().getName().startsWith("com.fishing")){
            fieldList(c.getSuperclass(),list) ;
        }
    }


    public Object resultObject(String result,Type genericReturnType) throws ClassNotFoundException {
        if (genericReturnType instanceof ParameterizedType) {
            /*Class<?> rawType = (Class<?>) ((ParameterizedType) genericReturnType).getRawType();
            if ("java.util.List".equals(rawType.getName())){
                return JSON.parseArray(result,(Class<?>) ((ParameterizedType) genericReturnType).getActualTypeArguments()[0]);
            }*/
            return JSON.parseObject(result,genericReturnType);
//            return JSON.parseObject(result, rawType);
        }
        return JSON.parseObject(result, Class.forName(genericReturnType.getTypeName()));
    }

    /**
     * 执行处理程序
     */
    private Object process(ProceedingJoinPoint joinPoint, RedisCacheable handler, String redisKey) throws Throwable {
        Object proceed = joinPoint.proceed();
        Class<? extends AbstractRedisCacheProccessor> process = handler.process();

        Object instance = getInstance(process);
        Method beforeProcess = getMethod(process, "beforeProcess", Object.class, String.class);
        Method processMain = getMethod(process, "process", StringRedisTemplate.class, Object.class, String.class, Long.class);

        Object data = beforeProcess.invoke(instance, proceed, redisKey);

        processMain.invoke(instance, redisTemplate, data, redisKey, handler.timeout());

        return data;
    }

    public Method getMethod(Class<? extends AbstractRedisCacheProccessor> process, String methodName, Class<?>... parameterTypes) throws Exception {
        PROCESS_METHODS.clear();
        String name = process.getName();
        //
        String key = name + "." + methodName + "(" + formatParameterName(parameterTypes) + ")";
        Method oldMethod = PROCESS_METHODS.get(key);
        if (null == oldMethod) {
            Method method = process.getMethod(methodName, parameterTypes);
            PROCESS_METHODS.put(key, method);
        }
        return PROCESS_METHODS.get(key);
    }

    public Object getInstance(Class<? extends AbstractRedisCacheProccessor> process) throws Exception {
        String name = process.getName();
        Object instance = PROCESS_INSTANCE.get(name);
        if (null == instance) {
            PROCESS_INSTANCE.put(name, process.newInstance());
        }
        return PROCESS_INSTANCE.get(name);
    }


    public String formatParameterName(Class<?>... parameterTypes) {
        StringBuilder parameter = new StringBuilder(StringUtils.EMPTY);
        if (null == parameterTypes || parameterTypes.length == 0) {
            return parameter.toString();
        }
        for (Class<?> parameterType : parameterTypes) {
            String parameterToString = parameterType.toString();
            parameter.append(parameterToString.replace("class ", StringUtils.EMPTY)).append(",");
        }
        parameter.deleteCharAt(parameter.length() - 1);
        return parameter.toString();
    }


    /**
     * 解析redisKey
     */
    private String getRedisKey(StandardEvaluationContext context, RedisCacheable handler) {
        //使用SPEL进行key的解析
        ExpressionParser parser = new SpelExpressionParser();
        String redisKeySpel = handler.key();
        if (redisKeySpel.contains("#")) {
            return parser.parseExpression(redisKeySpel).getValue(context, String.class);
        } else {
            return redisKeySpel;
        }
    }

    /**
     * 内容解析
     */
    private StandardEvaluationContext parseElContext(Method method, Object[] args) {
        //获取被拦截方法参数名列表(使用Spring支持类库)
        LocalVariableTableParameterNameDiscoverer localVariableTable = new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = localVariableTable.getParameterNames(method);
        if (null != paraNameArr && paraNameArr.length > 0) {
            //SPEL上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            //把方法参数放入SPEL上下文中
            for (int i = 0; i < paraNameArr.length; i++) {
                context.setVariable(paraNameArr[i], args[i]);
            }
            return context;
        }
        return null;
    }

}
