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

	// This is a very very basic aspect to show what can be done with an aspect
	// You can see that we can very easily bind to anything we want on the
	// activity
	// because we know its exact name.

	// This kind of aspect is meant to be generated.

	// Once generated, it will do the exact thing we need it to do.

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