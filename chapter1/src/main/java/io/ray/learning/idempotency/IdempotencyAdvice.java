package io.ray.learning.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.ray.learning.idempotency.annotation.Idempotent;
import io.ray.learning.idempotency.annotation.IdempotentContent;
import io.ray.learning.lock.DistributedLockManager;
import io.ray.learning.lock.RedisLockKey;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author ray
 */
@Aspect
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Component
@Slf4j
public class IdempotencyAdvice {

    private static final String KEY_PREFIX = "Idempotency";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${idempotency.key.appId:testing}")
    private String appId;

    @Autowired
    private DistributedLockManager<RedisLockKey> distributedLockManager;

    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void controller() {/* point cut */}

    @Pointcut("@annotation(io.ray.learning.idempotency.annotation.Idempotent)")
    public void idempotent() {/* point cut */}

    @Pointcut("controller() && idempotent()")
    public void idempotentController() {/* point cut */}

    @Around("idempotentController()")
    public Object enableIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            return joinPoint.proceed();
        Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        String operationName = "";
        if(idempotent.operationName() == Idempotent.OperationName.CUSTOMIZE)
            operationName = idempotent.customizeOperationName();
        else
            operationName = method.getDeclaringClass().getName()+"$"+method.getName();
        IdempotencyContent idempotencyContent = IdempotencyContent.buildContent(operationName);
        Parameter[] params = method.getParameters();
        Object[] args = joinPoint.getArgs();
        //添加附件参数信息
        for (int i = 0; i < params.length; i++) {
            IdempotentContent idempotentContent = params[i].getAnnotation(IdempotentContent.class);
            if (idempotentContent != null && args[i] != null) {
                IdempotentContent.Key[] keys = idempotentContent.value();
                if (keys.length == 1 && keys[0].valuePath().isEmpty())
                    idempotencyContent.addContent(keys[0].name(), args[i].toString());
                else {
                    ReadContext ctx = JsonPath.parse(objectMapper.writeValueAsString(args[i]));
                    for (IdempotentContent.Key key : keys)
                        idempotencyContent.addContent(key.name(), ctx.read(key.valuePath()).toString());
                }
            }
        }
        Lock operationLock = distributedLockManager.getDistributedLock(new RedisLockKey(appId, KEY_PREFIX + idempotencyContent.serialize()));
        if (operationLock.tryLock()) {
            try {
                return joinPoint.proceed();
            } finally {
                operationLock.unlock();
            }
        } else
            throw new RuntimeException("System busy");
    }
}
