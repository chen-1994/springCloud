package com.fishing.common.annotation;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

/**
 * @Description: @Log注解解释器
 * @Author: sunyw
 * @CreateDate: 2021/1/31 23:14
 * @UpdateUser: sunyw
 * @UpdateDate: 2021/1/31 23:14
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
@Aspect
@Component
@Slf4j
public class LogAsp {

    /**
     * 切点
     */
//    @Pointcut("@annotation(com.basic.works.annotation.Log)")
    //@Pointcut("@annotation(com.fishing.common.annotation.Log)")
    @Pointcut("execution(* com.fishing.*.controller.*.* (..)) "+"||execution(* com.fishing.*.inter.*.* (..)) ")
    public void pointCut() {

    }


    /**
     * 环绕日志打印
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        /*Log annotation = method.getAnnotation(Log.class);
        if (null == annotation) {
            return joinPoint.getArgs();
        }
        String name = annotation.name();*/
        String name = "controller";
        long startTime = System.currentTimeMillis();
        // log.info("[环绕日志]>>>>>>>>>>>>>>>>>>>>>>>>>方法[{}]开始执行,开始时间{}", name, startTime);
        HttpServletRequest request = getRequest();

        // log.info("[环绕日志]>>>>>>>>>>>>>>>>>>>>>>>>>请求方法路径为:{}", request.getRequestURL());
        StringBuilder params = new StringBuilder();
        //参数值
        Object[] argValues = joinPoint.getArgs();
        //参数名称
        String[] argNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        if (argValues != null) {
            for (int i = 0; i < argValues.length; i++) {
                params.append(argNames[i]).append(":").append(argValues[i]);
//                params.append(argNames[i]).append(":").append(JSONObject.toJSONString(argValues[i]));
            }
        }
        //log.info("[环绕日志]===>请求接口路径：[{}],请求参数为:[{}]", request.getRequestURI(), params + "");
        Object proceed = joinPoint.proceed();
        long costTime = System.currentTimeMillis() - startTime;
        if (costTime > 100) {
            // log.info("[环绕日志]>>>>>>>>>>>>>>>>>>>>>>>>>响应参数为: [{}]", JSON.toJSON(proceed));
            log.info("[环绕日志] 请求方法路径为:{},执行完毕耗时 :{}", method.getName(), costTime);
        }

        return proceed;
    }

    /**
     * 异常日志打印
     *
     * @param joinPoint
     * @param e
     */
//    @AfterThrowing(pointcut = "pointCut()", throwing = "e")
//    public void throwIng(JoinPoint joinPoint, Throwable e) {
//        log.info("[异常日志]>>>>>>>>>>>>>>>>>>>>>>>>>开始进行记录");
//        String stackTrace = stackTrace(e);
//        HttpServletRequest request = getRequest();
//        String ipAddr = HttpUtil.getIpAddress(request);
//        String cityInfo = IpUtils.getCityInfo(ipAddr);
//        log.info("[异常日志]>>>>>>>>>>>>>>>>>>>>>>>>>当前请求的Ip地址为:{}", ipAddr);
//        log.info("[异常日志]>>>>>>>>>>>>>>>>>>>>>>>>>当前请求的Ip所在地区:{}", cityInfo);
//        log.info("[异常日志]>>>>>>>>>>>>>>>>>>>>>>>>>错误信息为:{}", stackTrace);
//        log.info("[异常日志]>>>>>>>>>>>>>>>>>>>>>>>>>异常日志记录完毕");
//        // TODO: 2021/1/31 未做数据库存储
//    }

    /**
     * 堆栈异常获取
     *
     * @param throwable
     * @return
     */
    private static String stackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }

    /**
     * 获取HttpServletRequest
     *
     * @return
     */
    private HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        assert servletRequestAttributes != null;
        return servletRequestAttributes.getRequest();
    }
}


