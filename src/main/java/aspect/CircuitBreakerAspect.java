package com.example.feishuai.aspect;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CircuitBreakerAspect {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Around("@annotation(circuitBreakerAnnotation)")
    public Object circuitBreakerAround(ProceedingJoinPoint joinPoint, 
                                      CircuitBreakerAnnotation circuitBreakerAnnotation) throws Throwable {
        String circuitBreakerName = circuitBreakerAnnotation.name();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    public @interface CircuitBreakerAnnotation {
        String name();
    }
}