package ru.tsvetikov.warehouse.router.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("[{}] Starting method: {}", className, methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Method {} completed in {} ms",
                    className, methodName, duration);

            return result;

        } catch (Exception e) {
            log.error("[{}] Method {} failed: {}",
                    className, methodName, e.getMessage());
            throw e;
        }
    }
}

