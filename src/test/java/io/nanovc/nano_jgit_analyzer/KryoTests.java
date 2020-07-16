package io.nanovc.nano_jgit_analyzer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.nanovc.*;
import io.nanovc.areas.ByteArrayAreaAPI;
import io.nanovc.areas.ByteArrayHashMapArea;
import io.nanovc.areas.StringAreaAPI;
import io.nanovc.areas.StringLinkedHashMapArea;
import io.nanovc.content.ByteArrayContent;
import io.nanovc.content.StringContent;
import io.nanovc.junit.TestDirectory;
import io.nanovc.junit.TestDirectoryExtension;
import io.nanovc.memory.*;
import io.nanovc.searches.commits.SimpleSearchQueryDefinition;
import io.nanovc.searches.commits.expressions.AllRepoCommitsExpression;
import io.nanovc.timestamps.InstantTimestamp;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

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
    public void serializeNanoRepo(@TestDirectory(useTestName = true) Path testDirectory) throws IOException
    {
        // Create the memory repo (as a handler, because we want to serialize only the repo state, not the handler dependencies):
        // NOTE: This is the alternative way of using repo's (which contain just the state).
        //       The MemoryNanoRepo is an Object Oriented convenience API for repo's.
        MemoryRepoHandler<ByteArrayContent, ByteArrayHashMapArea> repoHandler = new MemoryRepoHandler<>(ByteArrayContent::new, ByteArrayHashMapArea::new);
        ByteArrayHashMapArea contentArea = repoHandler.createArea();
        contentArea.putBytes("/readme.txt", "Hello World!".getBytes(StandardCharsets.UTF_8));
        repoHandler.commitToBranch(contentArea, "first", "First Commit", CommitTags.none());
        contentArea.putBytes("/more.txt", "More!".getBytes(StandardCharsets.UTF_8));
        repoHandler.commitToBranch(contentArea, "second", "Second Commit", CommitTags.none());

        // Save the branch names so we can compare later:
        Set<String> branchNamesBeforeSave = repoHandler.getBranchNames();

        // Round trip the repo to disk and back:
        MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoFromDisk = roundTripNanoRepo(testDirectory.resolve("repo.kryo.bin"), repoHandler.getRepo());

        // Use the new repo for the handler:
        repoHandler.setRepo(repoFromDisk);

        // Check out the commits:
        Set<String> branchNamesAfterSave = repoHandler.getBranchNames();
        for (String branchName : branchNamesAfterSave)
        {
            // Get the commit for the branch:
            ByteArrayHashMapArea checkout = repoHandler.checkout(repoHandler.getLatestCommitForBranch(branchName));
        }

        // Make sure that the branch names match:
        assertIterableEquals(branchNamesBeforeSave, branchNamesAfterSave);

        // Assert that the repo is as expected:
        assertNanoRepo(repoFromDisk);
    }

    @Test
    public void serializeHugeNanoRepo(@TestDirectory(useTestName = true) Path testDirectory) throws IOException, GitAPIException
    {
        // Define timing variables:
        long nanoStart, nanoEnd, nanoDuration;

        // Set this to a repo that you already have checked out on disk:
        //Path pathToHugeExistingGitRepoOnDisk = Paths.get("/PATH/ToHuge/Repo");
        Path pathToHugeExistingGitRepoOnDisk = Paths.get("./");

        // Start timing:
        nanoStart = System.nanoTime();

        // Create a Nano Repo from the one on disk:
        MemoryNanoRepo nanoRepo = NanoJGitAnalyzer.createNanoRepoFromGitFilePath(pathToHugeExistingGitRepoOnDisk);

        // Count the total number of commits in the repo:
        int totalCommits = nanoRepo.search(new SimpleSearchQueryDefinition(null, AllRepoCommitsExpression.allRepoCommits(), null)).getCommits().size();

        // End timing:
        nanoEnd = System.nanoTime();
        nanoDuration = nanoEnd - nanoStart;

        System.out.println("*** READING ***");
        System.out.printf("Loading JGit Repo from '%s'%n", pathToHugeExistingGitRepoOnDisk.toString());
        System.out.printf("Duration: %,dns = %,dms%n", nanoDuration, nanoDuration / 1000_000L);
        System.out.printf("Rate: %,d commits/sec%n", totalCommits * 1000_000_000L / nanoDuration);
        System.out.println();

        // Round trip the repo to disk:
        MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoFromDisk = roundTripNanoRepo(testDirectory.resolve("file.bin"), nanoRepo);

        // Assert details about the repo:
        assertNanoRepo(repoFromDisk);
    }

    /**
     * This round-trips the given repo by writing it to disk and then loading it up again.
     * @param pathToWriteTo The path of the file to write to.
     * @param repoToWrite The repo to write.
     * @return The deserialized repo that was round-tripped to disk.
     * @throws FileNotFoundException
     */
    public MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> roundTripNanoRepo(Path pathToWriteTo, MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoToWrite) throws IOException
    {
        // Define timing variables:
        long nanoStart, nanoEnd, nanoDuration;

        // Create the serializer:
        Kryo kryo = new Kryo();
        kryo.register(byte[].class);
        kryo.register(MemoryCommit[].class);
        kryo.register(ByteArrayHashMapArea.class, new ByteArrayHashMapAreaSerializer());
        kryo.register(StringLinkedHashMapArea.class, new StringLinkedHashMapAreaSerializer());
        kryo.register(CommitTags.class, new StringLinkedHashMapAreaSerializer());
        kryo.register(MemoryCommit.class, new MemoryCommitSerializer());
        kryo.register(MemoryRepo.class, new MemoryRepoSerializer<ByteArrayContent, ByteArrayHashMapArea>());
        kryo.register(MemoryNanoRepo.class, new MemoryRepoSerializer<ByteArrayContent, ByteArrayHashMapArea>());
        kryo.setReferences(true); // References to the same objects (specifically byte arrays for commits) will be referenced.
        kryo.setCopyReferences(false);

        // Start timing:
        nanoStart = System.nanoTime();

        // Serialize the repo:
        try(Output output = new Output(new FileOutputStream(pathToWriteTo.toFile())))
        {
            kryo.writeObject(output, repoToWrite);
        }

        // End timing:
        nanoEnd = System.nanoTime();
        nanoDuration = nanoEnd - nanoStart;

        // Get the size of the saved repo:
        long totalBytes = Files.size(pathToWriteTo);

        System.out.println("*** SAVING ***");
        System.out.printf("Saving Repo to '%s'%n", pathToWriteTo.toString());
        System.out.printf("Total Bytes: %,d%n", totalBytes);
        System.out.printf("Duration: %,dns = %,dms%n", nanoDuration, nanoDuration / 1000_000L);
        System.out.printf("Throughput: %,d B/sec%n", totalBytes * 1000_000_000L / nanoDuration);
        System.out.println();

        // Start timing:
        nanoStart = System.nanoTime();

        // Deserialize the repo:
        try (Input input = new Input(new FileInputStream(pathToWriteTo.toFile())))
        {
            MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repoFromDisk = kryo.readObject(input, MemoryRepo.class);

            // End timing:
            nanoEnd = System.nanoTime();
            nanoDuration = nanoEnd - nanoStart;

            System.out.println("*** LOADING ***");
            System.out.printf("Loading Repo from '%s'%n", pathToWriteTo.toString());
            System.out.printf("Total Bytes: %,d%n", totalBytes);
            System.out.printf("Duration: %,dns = %,dms%n", nanoDuration, nanoDuration / 1000_000L);
            System.out.printf("Throughput: %,d B/sec%n", totalBytes * 1_000_000_000L / nanoDuration);
            System.out.println();

            return repoFromDisk;
        }
    }

    /**
     * This makes sure that the given repo is as expected.
     * It also performs timing against the repo.
     * @param repo The repo to analyze.
     */
    public void assertNanoRepo(MemoryRepo<ByteArrayContent, ByteArrayHashMapArea> repo)
    {
        // Define timing variables:
        long nanoStart, nanoEnd, nanoDuration;

        // Keep track of the total content that is processed:
        long totalBytesOfContent = 0L;
        int totalCommits = 0;

        // Create a handler so we can understand the deserialized repo:
        // NOTE: This is the alternative way of using repo's (which contain just the state).
        //       The MemoryNanoRepo is an Object Oriented convenience API for repo's.
        MemoryRepoHandler<ByteArrayContent, ByteArrayHashMapArea> repoHandler = new MemoryRepoHandler<>(ByteArrayContent::new, ByteArrayHashMapArea::new);

        // Use the repo for the handler:
        repoHandler.setRepo(repo);

        // Get all the commits:
        SearchQueryDefinitionAPI searchQueryDefinition = new SimpleSearchQueryDefinition(null, AllRepoCommitsExpression.allRepoCommits(), null);
        MemorySearchResults searchResults = repoHandler.search(searchQueryDefinition);

        // Start timing:
        nanoStart = System.nanoTime();

        // Go through each commit:
        for (MemoryCommit commit : searchResults.getCommits())
        {
            // Check out the commit:
            ByteArrayHashMapArea contentArea = repoHandler.checkout(commit);

            // Go through each piece of content to accumulate the size:
            for (Map.Entry<String, ByteArrayContent> entry : contentArea.entrySet())
            {
                // Accumulate the byte size:
                totalBytesOfContent += entry.getValue().bytes.length;
            }

            // Accumulate stats:
            totalCommits++;
        }


        // End timing:
        nanoEnd = System.nanoTime();
        nanoDuration = nanoEnd - nanoStart;

        System.out.println("*** CHECKOUT ***");
        System.out.println("Checking out each commit:");
        System.out.printf("Total Commits: %,d%n", totalCommits);
        System.out.printf("Total Bytes of Content: %,d%n", totalBytesOfContent);
        System.out.printf("Duration: %,dns = %,dms%n", nanoDuration, nanoDuration / 1000_000L);
        System.out.printf("Rate: %,d commits/sec%n", totalCommits * 1_000_000_000L / nanoDuration);
        BigInteger bigThroughput = BigInteger.valueOf(totalBytesOfContent).multiply(BigInteger.valueOf(1_000_000_000L)).divide(BigInteger.valueOf(nanoDuration));
        System.out.printf("Throughput: %,d B/sec = %,d MB/s%n", bigThroughput, bigThroughput.divide(BigInteger.valueOf(1024)).divide(BigInteger.valueOf(1024)));
        System.out.println();
    }

    public static class MemoryRepoSerializer <TContent extends ContentAPI, TArea extends AreaAPI<TContent>> extends Serializer<MemoryRepo<TContent, TArea>>
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
            kryo.writeObject(output, object.commitTags);
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
            StringAreaAPI commitTags = kryo.readObject(input, StringLinkedHashMapArea.class);
            ByteArrayAreaAPI snapshot = kryo.readObject(input, ByteArrayHashMapArea.class);
            MemoryCommit firstParent = kryo.readObjectOrNull(input, MemoryCommit.class);
            MemoryCommit[] memoryCommits = kryo.readObjectOrNull(input, MemoryCommit[].class);

            MemoryCommit memoryCommit = new MemoryCommit();
            memoryCommit.message = message;
            memoryCommit.timestamp = new InstantTimestamp(Instant.ofEpochMilli(timestampEpochMillis));
            memoryCommit.commitTags = commitTags;
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
                // NOTE: We allow kryo to reference paths that we have written before to save space:
                kryo.writeObject(output, areaEntry.path.path);

                // Write the content:
                kryo.writeObject(output, areaEntry.content.bytes);
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
                // NOTE: We allow Kryo to resolve references to paths to save space.
                String path = kryo.readObject(input, String.class);

                // Read the bytes for the content, allowing kryo to resolve references:
                byte[] bytes = kryo.readObject(input, byte[].class);

                // Create the content:
                ByteArrayContent content = new ByteArrayContent(bytes);

                // Create the entry:
                contentArea.putContent(RepoPath.at(path), content);
            }

            return contentArea;
        }
    }

    public static class StringLinkedHashMapAreaSerializer extends Serializer<StringLinkedHashMapArea>
    {
        /**
         * Writes the bytes for the object to the output.
         * <p>
         * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
         * serialier.
         *
         * @param object May be null if {@link #getAcceptsNull()} is true.
         */
        @Override public void write(Kryo kryo, Output output, StringLinkedHashMapArea object)
        {
            // Write the number of entries:
            output.writeVarInt(object.size(), true);

            // Write each entry:
            for (AreaEntry<StringContent> areaEntry : object)
            {
                // Write the path:
                // NOTE: We allow kryo to reference paths that we have written before to save space:
                kryo.writeObject(output, areaEntry.path.path);

                // Write the content:
                // NOTE: We allow kryo to reference commit tags that we have written before to save space:
                kryo.writeObject(output, areaEntry.content.value);
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
        @Override public StringLinkedHashMapArea read(Kryo kryo, Input input, Class<? extends StringLinkedHashMapArea> type)
        {
            // Read the number of entries:
            int entryCount = input.readVarInt(true);

            // Create the content area:
            StringLinkedHashMapArea contentArea = new StringLinkedHashMapArea();

            // Get each entry:
            for (int i = 0; i < entryCount; i++)
            {
                // Read the path:
                // NOTE: We allow Kryo to resolve references to paths to save space.
                String path = kryo.readObject(input, String.class);

                // Read the bytes for the content, allowing kryo to resolve references:
                String value = kryo.readObject(input, String.class);

                // Create the entry:
                contentArea.putString(RepoPath.at(path), value);
            }

            return contentArea;
        }
    }

}
