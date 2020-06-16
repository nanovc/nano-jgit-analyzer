package io.nanovc.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the {@link TestDirectory} extension is processed correctly by the {@link TestDirectoryExtension}.
 */
@ExtendWith(TestDirectoryExtension.class)
public class TestDirectoryTests
{
    @TestDirectory
    public static Path staticTestDirectoryPath;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public static Path staticTestDirectoryPathCustomRoot;

    @TestDirectory(name = "StaticCustomPath")
    public static Path staticTestDirectoryPathCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "StaticCustomPath")
    public static Path staticTestDirectoryPathCustomRootCustomName;

    @TestDirectory(useTestName = true)
    public static Path staticTestDirectoryPathUseTestName;


    @TestDirectory
    public static File staticTestDirectoryFile;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public static File staticTestDirectoryFileCustomRoot;

    @TestDirectory(name = "StaticCustomFile")
    public static File staticTestDirectoryFileCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "StaticCustomFile")
    public static File staticTestDirectoryFileCustomRootCustomName;

    @TestDirectory(useTestName = true)
    public static File staticTestDirectoryFileUseTestName;

    @Test
    public void testStaticFolderCreation()
    {
        assertNotNull(staticTestDirectoryPath);
        assertNotNull(staticTestDirectoryPathCustomRoot);
        assertNotNull(staticTestDirectoryPathCustomName);
        assertNotNull(staticTestDirectoryPathCustomRootCustomName);
        assertNotNull(staticTestDirectoryPathUseTestName);
        assertTrue(Files.exists(staticTestDirectoryPath));
        assertTrue(Files.exists(staticTestDirectoryPathCustomRoot));
        assertTrue(Files.exists(staticTestDirectoryPathCustomName));
        assertTrue(Files.exists(staticTestDirectoryPathCustomRootCustomName));
        assertTrue(Files.exists(staticTestDirectoryPathUseTestName));

        assertEquals("CustomRoot", staticTestDirectoryPathCustomRoot.getParent().getParent().getParent().getFileName().toString());
        assertEquals("CustomRoot", staticTestDirectoryPathCustomRootCustomName.getParent().getParent().getParent().getFileName().toString());

        assertEquals("StaticCustomPath", staticTestDirectoryPathCustomName.getFileName().toString());
        assertEquals("StaticCustomPath", staticTestDirectoryPathCustomRootCustomName.getFileName().toString());

        assertEquals("TestDirectoryTests", staticTestDirectoryPathUseTestName.getFileName().toString());

        assertNotNull(staticTestDirectoryFile);
        assertNotNull(staticTestDirectoryFileCustomRoot);
        assertNotNull(staticTestDirectoryFileCustomName);
        assertNotNull(staticTestDirectoryFileCustomRootCustomName);
        assertNotNull(staticTestDirectoryFileUseTestName);
        assertTrue(staticTestDirectoryFile.exists());
        assertTrue(staticTestDirectoryFileCustomRoot.exists());
        assertTrue(staticTestDirectoryFileCustomName.exists());
        assertTrue(staticTestDirectoryFileCustomRootCustomName.exists());
        assertTrue(staticTestDirectoryFileUseTestName.exists());

        assertEquals("CustomRoot", staticTestDirectoryFileCustomRoot.getParentFile().getParentFile().getParentFile().getName());
        assertEquals("CustomRoot", staticTestDirectoryFileCustomRootCustomName.getParentFile().getParentFile().getParentFile().getName());

        assertEquals("StaticCustomFile", staticTestDirectoryFileCustomName.getName());
        assertEquals("StaticCustomFile", staticTestDirectoryFileCustomRootCustomName.getName());

        assertEquals("TestDirectoryTests", staticTestDirectoryFileUseTestName.getName());
    }


    @TestDirectory
    public Path instanceTestDirectoryPath;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public Path instanceTestDirectoryPathCustomRoot;

    @TestDirectory(name = "InstanceCustomPath")
    public Path instanceTestDirectoryPathCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "InstanceCustomPath")
    public Path instanceTestDirectoryPathCustomRootCustomName;

    @TestDirectory(useTestName = true)
    public Path instanceTestDirectoryPathUseTestName;


    @TestDirectory
    public File instanceTestDirectoryFile;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public File instanceTestDirectoryFileCustomRoot;

    @TestDirectory(name = "InstanceCustomFile")
    public File instanceTestDirectoryFileCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "InstanceCustomFile")
    public File instanceTestDirectoryFileCustomRootCustomName;

    @TestDirectory(useTestName = true)
    public File instanceTestDirectoryFileUseTestName;

    @Test
    public void testInstanceFolderCreation()
    {
        assertNotNull(instanceTestDirectoryPath);
        assertNotNull(instanceTestDirectoryPathCustomRoot);
        assertNotNull(instanceTestDirectoryPathCustomName);
        assertNotNull(instanceTestDirectoryPathCustomRootCustomName);
        assertNotNull(instanceTestDirectoryPathUseTestName);

        assertTrue(Files.exists(instanceTestDirectoryPath));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomRoot));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomName));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomRootCustomName));
        assertTrue(Files.exists(instanceTestDirectoryPathUseTestName));

        assertEquals("CustomRoot", instanceTestDirectoryPathCustomRoot.getParent().getParent().getParent().getParent().getFileName().toString());
        assertEquals("CustomRoot", instanceTestDirectoryPathCustomRootCustomName.getParent().getParent().getParent().getParent().getFileName().toString());

        assertEquals("InstanceCustomPath", instanceTestDirectoryPathCustomName.getFileName().toString());
        assertEquals("InstanceCustomPath", instanceTestDirectoryPathCustomRootCustomName.getFileName().toString());

        assertEquals("testInstanceFolderCreation", instanceTestDirectoryPathUseTestName.getFileName().toString());


        assertNotNull(instanceTestDirectoryFile);
        assertNotNull(instanceTestDirectoryFileCustomRoot);
        assertNotNull(instanceTestDirectoryFileCustomName);
        assertNotNull(instanceTestDirectoryFileCustomRootCustomName);
        assertNotNull(instanceTestDirectoryFileUseTestName);

        assertTrue(instanceTestDirectoryFile.exists());
        assertTrue(instanceTestDirectoryFileCustomRoot.exists());
        assertTrue(instanceTestDirectoryFileCustomName.exists());
        assertTrue(instanceTestDirectoryFileCustomRootCustomName.exists());
        assertTrue(instanceTestDirectoryFileUseTestName.exists());

        assertEquals("CustomRoot", instanceTestDirectoryFileCustomRoot.getParentFile().getParentFile().getParentFile().getParentFile().getName());
        assertEquals("CustomRoot", instanceTestDirectoryFileCustomRootCustomName.getParentFile().getParentFile().getParentFile().getParentFile().getName());

        assertEquals("InstanceCustomFile", instanceTestDirectoryFileCustomName.getName());
        assertEquals("InstanceCustomFile", instanceTestDirectoryFileCustomRootCustomName.getName());

        assertEquals("testInstanceFolderCreation", instanceTestDirectoryFileUseTestName.getName());
    }

    @Test
    public void testParameterFolderCreation(
        @TestDirectory Path paramPath,
        @TestDirectory(rootPath = "./test-output/CustomRoot") Path paramPathCustomRoot,
        @TestDirectory(name = "ParamCustomPath") Path paramPathCustomName,
        @TestDirectory(rootPath = "./test-output/CustomRoot", name = "ParamCustomPath") Path paramPathCustomRootCustomName,

        @TestDirectory(useTestName = true) Path paramPathUseTestName,
        @TestDirectory(useTestName = true, name = "ParamCustomPathUseTestName") Path paramPathUseTestNameCustomName,

        @TestDirectory File paramFile,
        @TestDirectory(rootPath = "./test-output/CustomRoot") File paramFileCustomRoot,
        @TestDirectory(name = "ParamCustomFile") File paramFileCustomName,
        @TestDirectory(rootPath = "./test-output/CustomRoot", name = "ParamCustomFile") File paramFileCustomRootCustomName,

        @TestDirectory(useTestName = true) File paramFileUseTestName,
        @TestDirectory(useTestName = true, name = "ParamCustomFileUseTestName") File paramFileUseTestNameCustomName
        )
    {
        assertNotNull(paramPath);
        assertNotNull(paramPathCustomRoot);
        assertNotNull(paramPathCustomName);
        assertNotNull(paramPathCustomRootCustomName);

        assertTrue(Files.exists(paramPath));
        assertTrue(Files.exists(paramPathCustomRoot));
        assertTrue(Files.exists(paramPathCustomName));
        assertTrue(Files.exists(paramPathCustomRootCustomName));

        assertEquals("CustomRoot", paramPathCustomRoot.getParent().getParent().getParent().getParent().getFileName().toString());
        assertEquals("CustomRoot", paramPathCustomRootCustomName.getParent().getParent().getParent().getParent().getFileName().toString());

        assertEquals("ParamCustomPath", paramPathCustomName.getFileName().toString());
        assertEquals("ParamCustomPath", paramPathCustomRootCustomName.getFileName().toString());

        assertEquals("testParameterFolderCreation", paramPathUseTestName.getFileName().toString());
        assertEquals("testParameterFolderCreation", paramPathUseTestNameCustomName.getFileName().toString());


        assertNotNull(paramFile);
        assertNotNull(paramFileCustomRoot);
        assertNotNull(paramFileCustomName);
        assertNotNull(paramFileCustomRootCustomName);

        assertTrue(paramFile.exists());
        assertTrue(paramFileCustomRoot.exists());
        assertTrue(paramFileCustomName.exists());
        assertTrue(paramFileCustomRootCustomName.exists());

        assertEquals("CustomRoot", paramFileCustomRoot.getParentFile().getParentFile().getParentFile().getParentFile().getName());
        assertEquals("CustomRoot", paramFileCustomRootCustomName.getParentFile().getParentFile().getParentFile().getParentFile().getName());

        assertEquals("ParamCustomFile", paramFileCustomName.getName());
        assertEquals("ParamCustomFile", paramFileCustomRootCustomName.getName());

        assertEquals("testParameterFolderCreation", paramFileUseTestName.getName());
        assertEquals("testParameterFolderCreation", paramFileUseTestNameCustomName.getName());
    }

    @Test
    public void testAllFolderCreation(@TestDirectory Path paramPath, @TestDirectory File paramFile, @TestDirectory(useTestName = true) Path testFolderPath, @TestDirectory(useTestName = true) File testFolderFile)
    {
        assertNotNull(staticTestDirectoryPath);
        assertTrue(Files.exists(staticTestDirectoryPath));

        assertNotNull(staticTestDirectoryFile);
        assertTrue(staticTestDirectoryFile.exists());

        assertNotNull(instanceTestDirectoryPath);
        assertTrue(Files.exists(instanceTestDirectoryPath));

        assertNotNull(instanceTestDirectoryFile);
        assertTrue(instanceTestDirectoryFile.exists());

        assertNotNull(paramPath);
        assertTrue(Files.exists(paramPath));

        assertNotNull(paramFile);
        assertTrue(paramFile.exists());

        assertNotNull(testFolderPath);
        assertTrue(Files.exists(testFolderPath));
        assertEquals("testAllFolderCreation", testFolderPath.getFileName().toString());

        assertNotNull(testFolderFile);
        assertTrue(testFolderFile.exists());
        assertEquals("testAllFolderCreation", testFolderFile.getName());
    }
}
