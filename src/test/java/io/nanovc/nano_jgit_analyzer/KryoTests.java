package io.nanovc.nano_jgit_analyzer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.nanovc.Area;
import io.nanovc.AreaEntry;
import io.nanovc.Content;
import io.nanovc.RepoPath;
import io.nanovc.areas.ByteArrayArea;
import io.nanovc.areas.ByteArrayHashMapArea;
import io.nanovc.content.ByteArrayContent;
import io.nanovc.junit.TestDirectory;
import io.nanovc.junit.TestDirectoryExtension;
import io.nanovc.memory.MemoryCommit;
import io.nanovc.memory.MemoryNanoRepo;
import io.nanovc.memory.MemoryRepo;
import io.nanovc.memory.MemoryRepoHandler;
import io.nanovc.timestamps.InstantTimestamp;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This tests the excellent Kryo Serialization framework.
 * https://github.com/EsotericSoftware/kryo
 */
@ExtendWith(TestDirectoryExtension.class)
public class KryoTests
{
    /**
     * This shows the basic usage of Kryo from the quick start guide here:
     * https://github.com/EsotericSoftware/kryo#quickstart
     */
    @Test
    public void quickStart(@TestDirectory(useTestName = true) Path testDirectory) throws FileNotFoundException
    {
        Kryo kryo = new Kryo();
        kryo.register(SomeClass.class);

        SomeClass object = new SomeClass();
        object.value = "Hello Kryo!";

        Output output = new Output(new FileOutputStream(testDirectory.resolve("file.bin").toFile()));
        kryo.writeObject(output, object);
        output.close();

        Input input = new Input(new FileInputStream(testDirectory.resolve("file.bin").toFile()));
        SomeClass object2 = kryo.readObject(input, SomeClass.class);
        input.close();

        assertEquals(object.value, object2.value);
    }

    static public class SomeClass
    {
        String value;
    }


    @Test
    public void serializeNanoRepo(@TestDirectory(useTestName = true) Path testDirectory) throws FileNotFoundException
    {
        // Create the memory repo (as a handler, because we want to serialize only the repo state, not the handler dependencies):
        MemoryRepoHandler<ByteArrayContent, ByteArrayHashMapArea> repoHandler = new MemoryRepoHandler<>(ByteArrayContent::new, ByteArrayHashMapArea::new);
        ByteArrayHashMapArea contentArea = repoHandler.createArea();
        contentArea.putBytes("/readme.txt", "Hello World!".getBytes(StandardCharsets.UTF_8));
        repoHandler.commitToBranch(contentArea, "first", "First Commit");
        contentArea.putBytes("/more.txt", "More!".getBytes(StandardCharsets.UTF_8));
        repoHandler.commitToBranch(contentArea, "second", "Second Commit");

        // Create the serializer:
        Kryo kryo = new Kryo();
        kryo.register(byte[].class);
        kryo.register(MemoryCommit[].class);
        kryo.register(ByteArrayContent.class, new ByteArrayContentSerializer());
        kryo.register(ByteArrayHashMapArea.class, new ByteArrayHashMapAreaSerializer());
        kryo.register(MemoryCommit.class, new MemoryCommitSerializer());
        kryo.register(MemoryRepo.class, new MemoryRepoSerializer<ByteArrayContent, ByteArrayHashMapArea>());
        kryo.setReferences(true); // References to the same objects (specifically byte arrays for commits) will be referenced.
        kryo.setCopyReferences(false);

        // Serialize the repo:
        try(Output output = new Output(new FileOutputStream(testDirectory.resolve("file.bin").toFile())))
        {
            kryo.writeObject(output, repoHandler.getRepo());
        }

        // Deserialize the repo:
        try (Input input = new Input(new FileInputStream(testDirectory.resolve("file.bin").toFile())))
        {
            MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoFromDisk = kryo.readObject(input, MemoryRepo.class);

            // Use the repo for the handler:
            repoHandler.setRepo(repoFromDisk);

            // Check out the commits:
            Set<String> branchNames = repoHandler.getBranchNames();
            for (String branchName : branchNames)
            {
                // Get the commit for the branch:
                ByteArrayHashMapArea checkout = repoHandler.checkout(repoHandler.getLatestCommitForBranch(branchName));
            }
        }
    }

    @Test
    public void serializeHugeNanoRepo(@TestDirectory(useTestName = true) Path testDirectory) throws IOException, GitAPIException
    {
        MemoryNanoRepo nanoRepo = NanoJGitAnalyzer.createNanoRepoFromGitFilePath(Paths.get("/PATH/ToHuge/Repo"));

        // Create the serializer:
        Kryo kryo = new Kryo();
        kryo.register(byte[].class);
        kryo.register(MemoryCommit[].class);
        kryo.register(ByteArrayContent.class, new ByteArrayContentSerializer());
        kryo.register(ByteArrayHashMapArea.class, new ByteArrayHashMapAreaSerializer());
        kryo.register(MemoryCommit.class, new MemoryCommitSerializer());
        kryo.register(MemoryRepo.class, new MemoryRepoSerializer<ByteArrayContent, ByteArrayHashMapArea>());
        kryo.register(MemoryNanoRepo.class, new MemoryRepoSerializer<ByteArrayContent, ByteArrayHashMapArea>());
        kryo.setReferences(true); // References to the same objects (specifically byte arrays for commits) will be referenced.
        kryo.setCopyReferences(false);

        // Serialize the repo:
        try(Output output = new Output(new FileOutputStream(testDirectory.resolve("file.bin").toFile())))
        {
            kryo.writeObject(output, nanoRepo);
        }

        // Deserialize the repo:
        try (Input input = new Input(new FileInputStream(testDirectory.resolve("file.bin").toFile())))
        {
            MemoryRepoHandler<ByteArrayContent, ByteArrayHashMapArea> repoHandler = new MemoryRepoHandler<>(ByteArrayContent::new, ByteArrayHashMapArea::new);

            MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoFromDisk = kryo.readObject(input, MemoryRepo.class);

            // Use the repo for the handler:
            repoHandler.setRepo(repoFromDisk);

            // Check out the commits:
            Set<String> branchNames = repoHandler.getBranchNames();
            for (String branchName : branchNames)
            {
                // Get the commit for the branch:
                ByteArrayHashMapArea checkout = repoHandler.checkout(repoHandler.getLatestCommitForBranch(branchName));
            }
        }
    }

    public static class MemoryRepoSerializer <TContent extends Content, TArea extends Area<TContent>> extends Serializer<MemoryRepo<TContent, TArea>>
    {
        /**
         * Writes the bytes for the object to the output.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
         * serialier.
         *
         * @param object May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public void write(Kryo kryo, Output output, MemoryRepo<TContent, TArea> object)
        {
            // Write the number of branches:
            output.writeVarInt(object.getBranchTips().size(), true);

            // Write the branches:
            for (Map.Entry<String, MemoryCommit> entry : object.getBranchTips().entrySet())
            {
                // Write the branch name:
                output.writeString(entry.getKey());

                // Write the commit:
                kryo.writeObject(output, entry.getValue());
            }

            // Write the number of tags:
            output.writeVarInt(object.getTags().size(), true);

            // Write the tags:
            for (Map.Entry<String, MemoryCommit> entry : object.getTags().entrySet())
            {
                // Write the tag name:
                output.writeString(entry.getKey());

                // Write the commit:
                kryo.writeObject(output, entry.getValue());
            }

            // Write the number of dangling commits:
            output.writeVarInt(object.getDanglingCommits().size(), true);

            // Write the dangling commits:
            for (MemoryCommit danglingCommit : object.getDanglingCommits())
            {
                // Write the commit:
                kryo.writeObject(output, danglingCommit);
            }
        }

        /**
         * Reads bytes and returns a new object of the specified concrete type.
         * <p>
         * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
         * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
         * be reentrant.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
         * serialier.
         *
         * @return May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public MemoryRepo<TContent, TArea> read(Kryo kryo, Input input, Class<? extends MemoryRepo<TContent, TArea>> type)
        {
            // Create the memory repo:
            MemoryRepo<TContent, TArea> memoryRepo = new MemoryRepo<>();

            // Read the number of branches:
            int branchCount = input.readVarInt(true);

            // Read the branches:
            for (int i = 0; i < branchCount; i++)
            {
                // Read the branch name:
                String branchName = input.readString();

                // Read the commit:
                MemoryCommit commit = kryo.readObject(input, MemoryCommit.class);

                // Create the branch:
                memoryRepo.getBranchTips().put(branchName, commit);
            }

            // Read the number of tags:
            int tagCount = input.readVarInt(true);

            // Read the tags:
            for (int i = 0; i < tagCount; i++)
            {
                // Read the tag name:
                String tagName = input.readString();

                // Read the commit:
                MemoryCommit commit = kryo.readObject(input, MemoryCommit.class);

                // Create the tag:
                memoryRepo.getTags().put(tagName, commit);
            }

            // Read the number of dangling commits:
            int danglingCount = input.readVarInt(true);

            // Read the dangling commits:
            for (int i = 0; i < danglingCount; i++)
            {
                // Read the commit:
                MemoryCommit commit = kryo.readObject(input, MemoryCommit.class);

                // Create the dangling commit:
                memoryRepo.getDanglingCommits().add(commit);
            }

            return memoryRepo;
        }
    }

    public static class MemoryCommitSerializer extends Serializer<MemoryCommit>
    {

        /**
         * Writes the bytes for the object to the output.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
         * serialier.
         *
         * @param object May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public void write(Kryo kryo, Output output, MemoryCommit object)
        {
            output.writeString(object.message);
            output.writeVarLong(object.timestamp.getInstant().toEpochMilli(), true);
            kryo.writeObject(output, object.snapshot);
            kryo.writeObjectOrNull(output, object.firstParent, MemoryCommit.class);
            kryo.writeObjectOrNull(output, object.otherParents == null ? null : object.otherParents.toArray(), MemoryCommit[].class);
        }

        /**
         * Reads bytes and returns a new object of the specified concrete type.
         * <p>
         * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
         * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
         * be reentrant.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
         * serialier.
         *
         * @return May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public MemoryCommit read(Kryo kryo, Input input, Class<? extends MemoryCommit> type)
        {
            String message = input.readString();
            long timestampEpochMillis = input.readVarLong(true);
            ByteArrayArea snapshot = kryo.readObject(input, ByteArrayHashMapArea.class);
            MemoryCommit firstParent = kryo.readObjectOrNull(input, MemoryCommit.class);
            MemoryCommit[] memoryCommits = kryo.readObjectOrNull(input, MemoryCommit[].class);

            MemoryCommit memoryCommit = new MemoryCommit();
            memoryCommit.message = message;
            memoryCommit.timestamp = new InstantTimestamp(Instant.ofEpochMilli(timestampEpochMillis));
            memoryCommit.snapshot = snapshot;
            memoryCommit.firstParent = firstParent;
            memoryCommit.otherParents = memoryCommits == null ? null : Arrays.asList(memoryCommits);
            return memoryCommit;
        }
    }

    public static class ByteArrayHashMapAreaSerializer extends Serializer<ByteArrayHashMapArea>
    {


        /**
         * Writes the bytes for the object to the output.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
         * serialier.
         *
         * @param object May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public void write(Kryo kryo, Output output, ByteArrayHashMapArea object)
        {
            // Write the number of entries:
            output.writeVarInt(object.size(), true);

            // Write each entry:
            for (AreaEntry<ByteArrayContent> areaEntry : object)
            {
                // Write the path:
                output.writeString(areaEntry.path.path);

                // Write the content:
                kryo.writeObject(output, areaEntry.content);
            }
        }

        /**
         * Reads bytes and returns a new object of the specified concrete type.
         * <p>
         * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
         * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
         * be reentrant.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
         * serialier.
         *
         * @return May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public ByteArrayHashMapArea read(Kryo kryo, Input input, Class<? extends ByteArrayHashMapArea> type)
        {
            // Read the number of entries:
            int entryCount = input.readVarInt(true);

            // Create the content area:
            ByteArrayHashMapArea contentArea = new ByteArrayHashMapArea();

            // Get each entry:
            for (int i = 0; i < entryCount; i++)
            {
                // Read the path:
                String path = input.readString();

                // Read the content:
                ByteArrayContent content = kryo.readObject(input, ByteArrayContent.class);

                // Create the entry:
                contentArea.putContent(RepoPath.at(path), content);
            }

            return contentArea;
        }
    }

    public static class ByteArrayContentSerializer extends Serializer<ByteArrayContent>
    {

        /**
         * Writes the bytes for the object to the output.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
         * serialier.
         *
         * @param object May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public void write(Kryo kryo, Output output, ByteArrayContent object)
        {
            // Write the bytes, allowing Kryo to reuse references:
            kryo.writeObject(output, object.bytes);
        }

        /**
         * Reads bytes and returns a new object of the specified concrete type.
         * <p>
         * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
         * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
         * be reentrant.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
         * serialier.
         *
         * @return May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public ByteArrayContent read(Kryo kryo, Input input, Class<? extends ByteArrayContent> type)
        {
            // Read the bytes, allowing kryo to resolve references:
            byte[] bytes = kryo.readObject(input, byte[].class);

            // Create the byte array content:
            return new ByteArrayContent(bytes);
        }
    }

}
