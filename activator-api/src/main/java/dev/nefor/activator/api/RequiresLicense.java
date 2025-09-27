package dev.nefor.activator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated plugin requires a specific product id to be granted by the Activator license.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresLicense {
    String productId();
}
