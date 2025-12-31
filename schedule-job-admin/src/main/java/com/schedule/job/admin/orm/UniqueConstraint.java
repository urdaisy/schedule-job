package com.schedule.job.admin.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueConstraint {
    String name() default "";

    String[] columnNames();
}
