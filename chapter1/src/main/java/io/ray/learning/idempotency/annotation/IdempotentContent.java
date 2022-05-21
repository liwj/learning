package io.ray.learning.idempotency.annotation;

import java.lang.annotation.*;
/**
 * 幂等性验证附加KEY的取值
 * @author ray
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentContent {
    Key[] value();
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Key {
        String name();
        String valuePath() default "";
    }

}
