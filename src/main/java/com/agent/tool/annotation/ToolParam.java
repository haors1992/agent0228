package com.agent.tool.annotation;

import java.lang.annotation.*;

/**
 * @ToolParam Annotation
 * 
 *            Defines a single parameter schema for a tool
 *            Used within @Tool annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Documented
public @interface ToolParam {

    /**
     * Parameter name
     */
    String name();

    /**
     * Parameter type: string, integer, number, boolean, array, object
     */
    String type();

    /**
     * Parameter description - clear explanation of what this parameter is for
     */
    String description();

    /**
     * Whether this parameter is required
     */
    boolean required() default true;

    /**
     * Default value if not provided (as string)
     */
    String defaultValue() default "";

    /**
     * For enum-like parameters, list allowed values
     * Example: {"USD", "EUR", "CNY"} for currency parameter
     */
    String[] enum_() default {};

    /**
     * Pattern/regex for validation (for string types)
     * Example: "[0-9]+" for numbers
     */
    String pattern() default "";

    /**
     * Min value for numeric types
     */
    String minValue() default "";

    /**
     * Max value for numeric types
     */
    String maxValue() default "";

    /**
     * For array types, specify element type
     * Example: "string" for string array
     */
    String itemType() default "";

    /**
     * Example value - helps LLM understand the correct format
     * Example: "hello@example.com" for email parameter
     */
    String example() default "";
}
