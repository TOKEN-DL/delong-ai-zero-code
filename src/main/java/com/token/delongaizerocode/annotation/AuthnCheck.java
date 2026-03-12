package com.token.delongaizerocode.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthnCheck {


    /**
     * 必须有某个角色
     *
     * @return
     */
    String mustRole() default "";
}
