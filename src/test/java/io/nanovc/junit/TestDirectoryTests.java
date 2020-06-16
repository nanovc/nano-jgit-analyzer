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


    @TestDirectory
    public static File staticTestDirectoryFile;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public static File staticTestDirectoryFileCustomRoot;

    @TestDirectory(name = "StaticCustomFile")
    public static File staticTestDirectoryFileCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "StaticCustomFile")
    public static File staticTestDirectoryFileCustomRootCustomName;

    @Test
    public void testStaticFolderCreation()
    {
        assertNotNull(staticTestDirectoryPath);
        assertNotNull(staticTestDirectoryPathCustomRoot);
        assertNotNull(staticTestDirectoryPathCustomName);
        assertNotNull(staticTestDirectoryPathCustomRootCustomName);
        assertTrue(Files.exists(staticTestDirectoryPath));
        assertTrue(Files.exists(staticTestDirectoryPathCustomRoot));
        assertTrue(Files.exists(staticTestDirectoryPathCustomName));
        assertTrue(Files.exists(staticTestDirectoryPathCustomRootCustomName));

        assertEquals("CustomRoot", staticTestDirectoryPathCustomRoot.getParent().getParent().getParent().getFileName().toString());
        assertEquals("CustomRoot", staticTestDirectoryPathCustomRootCustomName.getParent().getParent().getParent().getFileName().toString());

        assertEquals("StaticCustomPath", staticTestDirectoryPathCustomName.getFileName().toString());
        assertEquals("StaticCustomPath", staticTestDirectoryPathCustomRootCustomName.getFileName().toString());

        assertNotNull(staticTestDirectoryFile);
        assertNotNull(staticTestDirectoryFileCustomRoot);
        assertNotNull(staticTestDirectoryFileCustomName);
        assertNotNull(staticTestDirectoryFileCustomRootCustomName);
        assertTrue(staticTestDirectoryFile.exists());
        assertTrue(staticTestDirectoryFileCustomRoot.exists());
        assertTrue(staticTestDirectoryFileCustomName.exists());
        assertTrue(staticTestDirectoryFileCustomRootCustomName.exists());

        assertEquals("CustomRoot", staticTestDirectoryFileCustomRoot.getParentFile().getParentFile().getParentFile().getName());
        assertEquals("CustomRoot", staticTestDirectoryFileCustomRootCustomName.getParentFile().getParentFile().getParentFile().getName());

        assertEquals("StaticCustomFile", staticTestDirectoryFileCustomName.getName());
        assertEquals("StaticCustomFile", staticTestDirectoryFileCustomRootCustomName.getName());
    }


    @TestDirectory
    public Path instanceTestDirectoryPath;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public Path instanceTestDirectoryPathCustomRoot;

    @TestDirectory(name = "InstanceCustomPath")
    public Path instanceTestDirectoryPathCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "InstanceCustomPath")
    public Path instanceTestDirectoryPathCustomRootCustomName;



    @TestDirectory
    public File instanceTestDirectoryFile;

    @TestDirectory(rootPath = "./test-output/CustomRoot")
    public File instanceTestDirectoryFileCustomRoot;

    @TestDirectory(name = "InstanceCustomFile")
    public File instanceTestDirectoryFileCustomName;

    @TestDirectory(rootPath = "./test-output/CustomRoot", name = "InstanceCustomFile")
    public File instanceTestDirectoryFileCustomRootCustomName;

    @Test
    public void testInstanceFolderCreation()
    {
        assertNotNull(instanceTestDirectoryPath);
        assertNotNull(instanceTestDirectoryPathCustomRoot);
        assertNotNull(instanceTestDirectoryPathCustomName);
        assertNotNull(instanceTestDirectoryPathCustomRootCustomName);

        assertTrue(Files.exists(instanceTestDirectoryPath));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomRoot));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomName));
        assertTrue(Files.exists(instanceTestDirectoryPathCustomRootCustomName));

        assertEquals("CustomRoot", instanceTestDirectoryPathCustomRoot.getParent().getParent().getParent().getParent().getFileName().toString());
        assertEquals("CustomRoot", instanceTestDirectoryPathCustomRootCustomName.getParent().getParent().getParent().getParent().getFileName().toString());

        assertEquals("InstanceCustomPath", instanceTestDirectoryPathCustomName.getFileName().toString());
        assertEquals("InstanceCustomPath", instanceTestDirectoryPathCustomRootCustomName.getFileName().toString());


        assertNotNull(instanceTestDirectoryFile);
        assertNotNull(instanceTestDirectoryFileCustomRoot);
        assertNotNull(instanceTestDirectoryFileCustomName);
        assertNotNull(instanceTestDirectoryFileCustomRootCustomName);

        assertTrue(instanceTestDirectoryFile.exists());
        assertTrue(instanceTestDirectoryFileCustomRoot.exists());
        assertTrue(instanceTestDirectoryFileCustomName.exists());
        assertTrue(instanceTestDirectoryFileCustomRootCustomName.exists());

        assertEquals("CustomRoot", instanceTestDirectoryFileCustomRoot.getParentFile().getParentFile().getParentFile().getParentFile().getName());
        assertEquals("CustomRoot", instanceTestDirectoryFileCustomRootCustomName.getParentFile().getParentFile().getParentFile().getParentFile().getName());

        assertEquals("InstanceCustomFile", instanceTestDirectoryFileCustomName.getName());
        assertEquals("InstanceCustomFile", instanceTestDirectoryFileCustomRootCustomName.getName());
    }

    @Test
    public void testParameterFolderCreation(
        @TestDirectory Path paramPath,
        @TestDirectory(rootPath = "./test-output/CustomRoot") Path paramPathCustomRoot,
        @TestDirectory(name = "ParamCustomPath") Path paramPathCustomName,
        @TestDirectory(rootPath = "./test-output/CustomRoot", name = "ParamCustomPath") Path paramPathCustomRootCustomName,
        @TestDirectory File paramFile,
        @TestDirectory(rootPath = "./test-output/CustomRoot") File paramFileCustomRoot,
        @TestDirectory(name = "ParamCustomFile") File paramFileCustomName,
        @TestDirectory(rootPath = "./test-output/CustomRoot", name = "ParamCustomFile") File paramFileCustomRootCustomName
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
    }

    @Test
    public void testAllFolderCreation(@TestDirectory Path paramPath, @TestDirectory File paramFile)
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
    }
}
