package com.schedule.job.security.reflect;

public interface ProceedingJoinPoint extends JoinPoint {
    Object proceed() throws Throwable;
    Object proceed(Object[] args) throws Throwable;
}
