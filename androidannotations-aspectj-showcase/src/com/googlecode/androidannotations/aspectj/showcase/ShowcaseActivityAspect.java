package com.googlecode.androidannotations.aspectj.showcase;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import android.app.Activity;
import android.util.Log;

@Aspect
public class ShowcaseActivityAspect {
	
	// Currently useless aspect

	@Pointcut("call(* com.googlecode.androidannotations.aspectj.showcase.ShowcaseActivity.*(..)) && target(activ)")
	public void basicPointCut(Activity activ) {
	}

	@Before("basicPointCut(activ)")
	public void anyCall(Activity activ) {

		Log.i("The Aspect",
				"Called method on package name " + activ.getPackageName());

	}

	@Around("basicPointCut(activ)")
	public Object anyCall2(ProceedingJoinPoint thisJoinPoint, Activity activ) throws Throwable {
		Log.i("The Aspect", "Called method "
				+ thisJoinPoint.getSignature().getName() + "(..) on class "
				+ activ.getClass().getSimpleName() + " on package name "
				+ activ.getPackageName());
		
		return thisJoinPoint.proceed();
	}

}