package io.nanovc.junit;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.ReflectionUtils.isPrivate;

/**
 * This is an extension to JUnit to create a test directory for the test that runs.
 * <p>
 * The way you inject a temporary directory is by declaring a test parameter that has a Path variable annotated with
 * <p>
 * For details about extensions, see here:
 * https://www.baeldung.com/junit-5-extensions
 * <p>
 * We are particularly interested in parameter resolution:
 * https://www.baeldung.com/junit-5-extensions#4-parameter-resolution
 * <p>
 * This implementation is also taken from {@link TempDir} and TempDirectory as reference.
 */
public class TestDirectoryExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver
{

    /**
     * This is the namespace to use to store general data for this extension.
     */
    private static final ExtensionContext.Namespace GENERAL_NAMESPACE = ExtensionContext.Namespace.create(TestDirectoryExtension.class);

    /**
     * This is the key in the store for the creation timestamp of this extension.
     * This is used to timestamp the directories that we create.
     */
    private final static String CREATION_TIMESTAMP_KEY = "creationTimestamp";

    /**
     * This is the namespace to use to store paths for this extension.
     */
    private static final ExtensionContext.Namespace PATH_NAMESPACE = ExtensionContext.Namespace.create(TestDirectory.class);


    public TestDirectoryExtension()
    {
    }

    /**
     * Callback that is invoked once <em>before</em> all tests in the current
     * container.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override public void beforeAll(ExtensionContext context) throws Exception
    {
        injectFields(context, null, context.getRequiredTestClass(), ReflectionUtils::isStatic);
    }

    /**
     * Callback that is invoked <em>before</em> each test is invoked.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override public void beforeEach(ExtensionContext context) throws Exception
    {
        // Go through each test instance:
        context
            .getRequiredTestInstances()
            .getAllInstances() //
            .forEach(testInstance -> injectFields(
                context,
                testInstance,
                context.getRequiredTestClass(),
                ReflectionUtils::isNotStatic)
            );
    }

    /**
     * Determine if this resolver supports resolution of an argument for the
     * {@link Parameter} in the supplied {@link ParameterContext} for the supplied
     * {@link ExtensionContext}.
     *
     * <p>The {@link Method} or {@link Constructor}
     * in which the parameter is declared can be retrieved via
     * {@link ParameterContext#getDeclaringExecutable()}.
     *
     * @param parameterContext the context for the parameter for which an argument should
     *                         be resolved; never {@code null}
     * @param extensionContext the extension context for the {@code Executable}
     *                         about to be invoked; never {@code null}
     * @return {@code true} if this resolver can resolve an argument for the parameter
     * @see #resolveParameter
     * @see ParameterContext
     */
    @Override public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException
    {
        boolean annotated = parameterContext.isAnnotated(TestDirectory.class);
        if (annotated && parameterContext.getDeclaringExecutable() instanceof Constructor)
        {
            throw new ParameterResolutionException(
                "@TestDirectory is not supported on constructor parameters. Please use field injection instead.");
        }
        return annotated;
    }

    /**
     * Resolve an argument for the {@link Parameter} in the supplied {@link ParameterContext}
     * for the supplied {@link ExtensionContext}.
     *
     * <p>This method is only called by the framework if {@link #supportsParameter}
     * previously returned {@code true} for the same {@link ParameterContext}
     * and {@link ExtensionContext}.
     *
     * <p>The {@link Method} or {@link Constructor}
     * in which the parameter is declared can be retrieved via
     * {@link ParameterContext#getDeclaringExecutable()}.
     *
     * @param parameterContext the context for the parameter for which an argument should
     *                         be resolved; never {@code null}
     * @param extensionContext the extension context for the {@code Executable}
     *                         about to be invoked; never {@code null}
     * @return the resolved argument for the parameter; may only be {@code null} if the
     *     parameter type is not a primitive
     * @see #supportsParameter
     * @see ParameterContext
     */
    @Override public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException
    {
        // Get our annotation from the parameter:
        Optional<TestDirectory> annotationOptional = parameterContext.findAnnotation(TestDirectory.class);
        if (annotationOptional.isPresent())
        {
            // Get the annotation:
            TestDirectory annotation = annotationOptional.get();

            // Get the specific type of parameter we want to set:
            Class<?> parameterType = parameterContext.getParameter().getType();

            // Make sure that the parameter type is as we want it:
            assertSupportedType("parameter", parameterType);

            // Create the path for the parameter:
            return getPathOrFile(parameterType, parameterContext.getParameter().getName(), annotation, extensionContext);
        }
        else
        {
            return null;
        }
    }

    /**
     * @param context The extension context that we are running in.
     * @param testInstance The test instance to augment. If this is a BeforeAll context then pass null to represent static fields.
     * @param testClass The class that is being processed.
     * @param predicate The predicate to test whether we want to process the field or not.
     */
    private void injectFields(ExtensionContext context, Object testInstance, Class<?> testClass, Predicate<Field> predicate)
    {
        findAnnotatedFields(testClass, TestDirectory.class, predicate).forEach(field -> {
            assertValidFieldCandidate(field);
            try
            {
                // Get the annotation:
                TestDirectory annotation = field.getAnnotation(TestDirectory.class);

                // Get a new path for the field:
                Object pathOrFile = getPathOrFile(field.getType(), field.getName(), annotation, context);

                // Set the field value:
                field.set(testInstance, pathOrFile);
            }
            catch (Throwable t)
            {
                ExceptionUtils.throwAsUncheckedException(t);
            }
        });
    }

    /**
     * This checks that the field matches our criteria.
     * Currently it makes sure that the field is not private.
     * @param field The field to make sure matches our criteria.
     */
    private void assertValidFieldCandidate(Field field)
    {
        assertSupportedType("field", field.getType());
        if (isPrivate(field))
        {
            throw new ExtensionConfigurationException("@TestDirectory field [" + field + "] must not be private.");
        }
    }

    /**
     * This makes sure that the target type is a Path or File.
     * @param target The target name (for making the exception specific). This should indicate where we got this from (field or parameter).
     * @param type The target type that we want to check to be Path or File.
     */
    private void assertSupportedType(String target, Class<?> type)
    {
        if (type != Path.class && type != File.class)
        {
            throw new ExtensionConfigurationException("Can only resolve @TestDirectory " + target + " of type "
                                                      + Path.class.getName() + " or " + File.class.getName() + " but was: " + type.getName());
        }
    }

    /**
     * @param targetFieldValueType The return type that we want. This is either {@link Path} or {@link File}.
     * @param targetFieldName      The name of the field or property that the attribute was applied to.
     * @param annotation           The annotation that was applied to the field or parameter. This has the context to use for the directory.
     * @param extensionContext     The context in which this extension is being applied.
     * @return The Path or File to set for the field or parameter.
     */
    private Object getPathOrFile(Class<?> targetFieldValueType, String targetFieldName, TestDirectory annotation, ExtensionContext extensionContext)
    {
        // Get the root path to start from:
        Path rootPath = Paths.get(annotation.rootPath());
        Path path = rootPath;

        // Get the general store for this extension:
        ExtensionContext.Store generalStore = extensionContext.getStore(GENERAL_NAMESPACE);

        // Get or create the creation timestamp for this store:
        LocalDateTime creationTimestamp = generalStore.getOrComputeIfAbsent(CREATION_TIMESTAMP_KEY, key -> LocalDateTime.now(), LocalDateTime.class);

        // Get the date time format string:
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss");

        // Get the timestamp for the folder:
        String timestamp = creationTimestamp.format(formatter);

        // Get the path folder with the timestamp:
        path = path.resolve(timestamp);

        // Get the test class that is running:
        Class<?> testClass = extensionContext.getRequiredTestClass();

        // Add the test class name:
        path = path.resolve(testClass.getSimpleName());

        // Get the test that is running:
        Optional<Method> testMethodOptional = extensionContext.getTestMethod();
        // This might be null if it's a static method.

        // Check whether we have a test method:
        if (testMethodOptional.isPresent())
        {
            // We have a test name.

            // Get the test:
            Method testMethod = testMethodOptional.get();

            // Add the test name:
            path = path.resolve(testMethod.getName());
        }

        // Check whether we should just use the test as the name:
        if (!annotation.useTestName())
        {
            // We mustn't use the test name,
            // but rather get the name from the field or parameter that the annotation was put on.

            // Keep a reference to the test path because we re-use it later:
            Path testPath = path;

            // Check whether we have a custom name:
            String customName = annotation.name();
            String folderName;
            if (customName.isEmpty())
            {
                // We don't have a custom name for this test directory.

                // Use the target field name as the folder name:
                folderName = targetFieldName;
            }
            else
            {
                // We have a custom name for this test directory.

                // Use the custom name as the folder name:
                folderName = customName;
            }

            // Add the folder name:
            path = path.resolve(folderName);

            // Get the path store for this extension so that we can reference count the unique paths:
            ExtensionContext.Store pathStore = extensionContext.getStore(PATH_NAMESPACE);

            // Get or create the count entry for this path:
            AtomicInteger nextPathCount = pathStore.getOrComputeIfAbsent(path.toString(), key -> new AtomicInteger(), AtomicInteger.class);

            // Get the next count value for this path:
            int instanceCount = nextPathCount.getAndIncrement();

            // Build the path that we want with the instance count:
            if (instanceCount > 0) path = testPath.resolve(folderName + '-' + instanceCount);
        }
        // else
        // {
        //     // We must use the test name as the directory.
        //     // NOTE: This mode will reuse the same folder for each annotation that asks for this.
        // }

        // Now we have the path that we want.

        try
        {
            // Make sure the folder exists:
            Files.createDirectories(path);
        }
        catch (IOException e)
        {
            ExceptionUtils.throwAsUncheckedException(e);
        }

        return (targetFieldValueType == Path.class) ? path : path.toFile();
    }

}
