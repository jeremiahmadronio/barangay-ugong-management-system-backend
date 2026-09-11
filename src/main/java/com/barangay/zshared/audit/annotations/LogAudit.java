package com.barangay.zshared.audit.annotations;

import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place this annotation on any service method you want to audit.

 * Usage:
 *   @LogAudit(module = AuditModule.SECURITY, action = "CREATED_CASE", severity = Severity.INFO)
 *   public BlotterCase createCase(BlotterCaseDTO dto) { ... }

 * Old and new values are automatically captured from the method's
 * first argument (as old value) and return value (as new value).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAudit {

    // Which module this action belongs to
    AuditModule module();

    // Short label for the action (e.g. "Created Cases", "Lock Users")
    String action();

    // How serious is this action
    Severity severity() default Severity.INFO;

    // Optional reason — leave blank if not needed
    String reason()  default "";

    // Set to true if you want to capture the method argument as old value
    boolean captureOldValue() default false;

    // Which argument index to use as the old value (default: 0 = first arg)
    int oldValueArgIndex() default 0;

    // Set to true if you want to capture the return value as new value
    boolean captureNewValue() default true;
}
