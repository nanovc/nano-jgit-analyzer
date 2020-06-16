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
     * If this is empty then it uses the name of the field or parameter that this annotation is placed on,
     * unless {@link #useTestName()} is true, which means it will rather use the test name instead and ignore this name.
     * NOTE: Unless the code is compiled with the flag to preserve parameter names, you might get generic names like arg0, arg1 etc.
     *       In that case, consider adding names explicitly to the annotated parameters.
     *       https://www.baeldung.com/java-parameter-reflection
     * @return The name of the directory to create. Empty if it should be the field or parameter that the annotation was placed on.
     */
    String name() default "";

    /**
     * Flags whether to use the running test name as the name of the directory to create.
     * If this is specified then the {@link #name()} is ignored.
     * When we use the test name, the test folder is shared with all other folders for that test.
     * When we don't use the test name then each occurrence of the {@link TestDirectory} annotation gets its own directory.
     * This is most useful when there is only one {@link TestDirectory} parameter provided.
     * If this is false then the {@link #name()} will be taken from the field name or parameter name in the event that it is empty.
     * @return True to use the test name as the directory name (ignoring the {@link #name()}). False to use the field name or parameter name which might be arg0, arg1 unless parameter names are preserved during compilation (which usually doesn't happen).
     */
    boolean useTestName() default false;
}
