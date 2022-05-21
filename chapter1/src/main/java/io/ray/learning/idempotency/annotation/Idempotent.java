package io.ray.learning.idempotency.annotation;

import java.lang.annotation.*;

/**
 * 定义幂等性验证时KEY的策略
 * @author ray
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Idempotent {
    //默认使用方法全限定类名加方法名
    OperationName operationName() default OperationName.METHOD;
    //当使用CUSTOMIZE时，需要定义名称，避免方法重载情况造成冲突
    String customizeOperationName() default "";
    enum OperationName {
        METHOD,
        CUSTOMIZE;
    }
}
