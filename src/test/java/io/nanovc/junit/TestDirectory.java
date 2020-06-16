package io.nanovc.junit;

import java.lang.annotation.*;

/**
 * This flags a field or test parameter to be injected with a unique directory for the test.
 * This can be used to write temporary files for the test.
 * Each test gets a new directory.
 * The directory is not deleted after the test. You need to do that yourself when you are done with it.
 * The other temporary directories delete themselves after the test is done
 * which makes it difficult to find and review by hand.
 * This implementation deliberately doesn't delete the directory.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestDirectory
{
    /**
     * The root path to create the test output under.
     * If this is not supplied then it defaults to ./test-output.
     * @return The root path where this test directory should be stored.
     */
    String rootPath() default "./test-output";

    /**
     * The name of the test directory to create.
     * If this is empty then it uses the name of the field or parameter that this annotation is placed on.
     * NOTE: Unless the code is compiled with the flag to preserve parameter names, you might get generic names like arg0, arg1 etc.
     *       In that case, consider adding names explicitly to the annotated parameters.
     *       https://www.baeldung.com/java-parameter-reflection
     * @return The name of the directory to create. Empty if it should be the field or parameter that the annotation was placed on.
     */
    String name() default "";
}
