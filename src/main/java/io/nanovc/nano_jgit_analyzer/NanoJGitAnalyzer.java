package io.nanovc.nano_jgit_analyzer;

import io.nanovc.CommitTags;
import io.nanovc.areas.ByteArrayHashMapArea;
import io.nanovc.memory.MemoryCommit;
import io.nanovc.memory.MemoryNanoRepo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a utility class that helps with reading real Git repo's using JGit
 * and gives them to you as Nano Version Control repositories.
 * This is useful because once in memory, it is much faster to interact with the content in NanoVC.
 */
public class NanoJGitAnalyzer
{
    /**
     * This has the paths for git commit information.
     */
    public static class GitCommitTagPaths extends CommitTags.CommonPaths
    {
        /**
         * This is the path to the authors name.
         */
        public static final String AUTHOR_NAME_PATH = AUTHOR_PATH + "/name";

        /**
         * This is the path to the authors email address.
         */
        public static final String AUTHOR_EMAIL_PATH = AUTHOR_PATH + "/email";

        /**
         * This is the path to commit message.
         */
        public static final String COMMIT_MESSAGE_PATH = "/message";

        /**
         * This is the path to the short commit message.
         * The first line is everything up to the first pair of LFs. This is the
         * "oneline" format, suitable for output in a single line display.
         */
        public static final String COMMIT_MESSAGE_SHORT_PATH = COMMIT_MESSAGE_PATH + "/short";
    }

    /**
     * This creates a {@link MemoryNanoRepo} for the entire contents of the given git repo.
     * The git repo has to already be passed in and the caller is responsible for closing it appropriately.
     *
     * @param git The git repo to load into memory. This git repo should already be open and it it up to the caller to close git appropriately.
     * @return The in-memory nano repository with the contents of the entire git repository.
     * @throws IOException     If files could not be opened or read correctly.
     * @throws GitAPIException If there were errors involved with accessing the Git repository.
     */
    public static MemoryNanoRepo createNanoRepoFromGitRepo(Git git) throws IOException, GitAPIException
    {
        // Create a simulated clock so that we can override timestamps for commits:
        SimulatedInstantClock clock = new SimulatedInstantClock();

        // Create the nano repo where we will load the entire Git repo:
        MemoryNanoRepo nanoRepo = new MemoryNanoRepo(
            null,
            MemoryNanoRepo.COMMON_ENGINE,
            clock,
            MemoryNanoRepo.COMMON_DIFFERENCE_HANDLER,
            MemoryNanoRepo.COMMON_COMPARISON_HANDLER,
            MemoryNanoRepo.COMMON_MERGE_HANDLER
        );

        // Keep a map of hashes to commits so that we can recreate our parentage correctly:
        Map<String, MemoryCommit> commitsByCommitHash = new HashMap<>();

        // Get the low level repository so we can interrogate it directly:
        Repository repository = git.getRepository();

        //
        //
        //
        // // Get all the commits for the git repo:
        // LogCommand logCommand = git
        //     .log()
        //     .all();
        // Iterable<RevCommit> revCommits = logCommand.call();

        try (RevWalk revWalk = new RevWalk(repository))
        {
            // Get the object reader that we are using for this revision walk:
            ObjectReader objectReader = revWalk.getObjectReader();

            // Get all the references for this repository:
            List<Ref> allRefs = repository.getRefDatabase().getRefs();

            // Keep a map of original refs to the actual refs that we ended up using after peeling:
            Map<Ref, Ref> originalRefToActualRefMap = new HashMap<>();

            // Keep a map of revCommits to the original ref that was used to get it:
            Map<RevCommit, Ref> revCommitToOriginalRefMap = new HashMap<>();

            // Go through each references and make sure that we walk it:
            for (Ref originalRef : allRefs)
            {
                // Start with the original ref:
                Ref ref = originalRef;

                // Check whether the reference is peeled (annotated tags).
                if (!ref.isPeeled())
                {
                    // The reference is not peeled.
                    // Peel the reference:
                    ref = repository.getRefDatabase().peel(ref);
                }

                // Get the objectID for this reference:
                ObjectId objectId = ref.getPeeledObjectId();
                if (objectId == null)
                {
                    objectId = ref.getObjectId();
                }

                // Try to get the revision commit:
                RevCommit revCommit = null;
                try
                {
                    // Parse the commit information:
                    revCommit = revWalk.parseCommit(objectId);
                }
                catch (MissingObjectException | IncorrectObjectTypeException e)
                {
                    // ignore as traversal starting point:
                    // - the ref points to an object that does not exist
                    // - the ref points to an object that is not a commit (e.g. a
                    // tree or a blob)
                }
                if (revCommit != null)
                {
                    // Add this commit as a starting point for our revision walk:
                    revWalk.markStart(revCommit);

                    // Save the mappings:
                    revCommitToOriginalRefMap.put(revCommit, originalRef);
                    originalRefToActualRefMap.put(originalRef, ref);
                }
            }
            // Now we have all the references marked as starting points for the revision walk.

            // Define the sort order that we want:
            // We want all parents to be traversed (old commits) before children and then children must be traversed in time order.
            revWalk.sort(RevSort.TOPO, true);
            revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
            revWalk.sort(RevSort.REVERSE, true);

            // Walk each revision:
            for (RevCommit revCommit : revWalk)
            {
                System.out.println("id: " + revCommit.getId());
                System.out.println("name: " + revCommit.getName());
                System.out.println("message: " + revCommit.getFullMessage());

                // Create a content area for this commit:
                ByteArrayHashMapArea contentArea = nanoRepo.createArea();

                // Get the tree of files for this commit:
                RevTree commitRootOfTree = revCommit.getTree();

                // Walk the tree for this revision:
                try (TreeWalk treeWalk = new TreeWalk(repository, objectReader))
                {
                    // Walk the tree recursively:
                    treeWalk.setRecursive(true);

                    // Set the root of the tree walker to point at the root of the commit:
                    treeWalk.addTree(commitRootOfTree);

                    // Get all the paths for the commit by walking the tree:
                    while (treeWalk.next())
                    {
                        // Get the details of this path:
                        final ObjectId blobId = treeWalk.getObjectId(0);
                        final FileMode mode = treeWalk.getFileMode(0);
                        final String path = treeWalk.getPathString();
                        // System.out.println(path + ":" + blobId.getName());

                        // Get the loader for the contents of the file:
                        ObjectLoader blobLoader = repository.open(blobId);

                        // Get the bytes for the blob:
                        byte[] blobBytes = blobLoader.getBytes();
                        //System.out.println(blobBytes.length);
                        //System.out.println(new String(blobBytes));

                        // Place the bytes in our content area:
                        contentArea.putBytes(path, blobBytes);
                    }
                }
                // Now we have walked the entire tree of paths for this commit.

                // Search for all parent commits:
                MemoryCommit firstParentCommit = null;
                List<MemoryCommit> otherParentCommits = null;
                for (RevCommit revCommitParent : revCommit.getParents())
                {
                    // Resolve the memory commit for this revCommit:
                    // NOTE: Our revWalk sort order means that we get commits from oldest to newest.
                    MemoryCommit memoryCommit = commitsByCommitHash.get(revCommitParent.getName());

                    // Check whether we have our first parent commit yet:
                    if (firstParentCommit == null)
                    {
                        // This is our first parent commit.

                        // Set this as our first parent commit:
                        firstParentCommit = memoryCommit;
                    }
                    else
                    {
                        // This is not our first parent commit.

                        // Check whether this is our second parent commit:
                        if (otherParentCommits == null)
                        {
                            // This is our second parent commit.
                            // Create the list of other parent commits:
                            otherParentCommits = new ArrayList<>();
                        }

                        // Add this commit to the list of other parent commits:
                        otherParentCommits.add(memoryCommit);
                    }
                }
                // Now we have all of our parent commits resolved.


                // Override the commit timestamp:
                // It appears that the commits are the number of seconds from the Epoch.
                // At first we thought it uses the millis because the implementation uses System.currentTimeMillis(). Strange.
                // The difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
                clock.nowOverride = Instant.ofEpochSecond(revCommit.getCommitTime());

                // Get the commit tags:
                PersonIdent authorIdent = revCommit.getAuthorIdent();
                CommitTags commitTags = CommitTags
                    .with()
                    .and(GitCommitTagPaths.AUTHOR_NAME_PATH, authorIdent.getName())
                    .and(GitCommitTagPaths.AUTHOR_EMAIL_PATH, authorIdent.getEmailAddress())
                    .and(GitCommitTagPaths.COMMIT_MESSAGE_PATH, revCommit.getFullMessage())
                    .and(GitCommitTagPaths.COMMIT_MESSAGE_SHORT_PATH, revCommit.getShortMessage())
                    ;

                // Commit the content area to nano version control:
                // NOTE: We separate the API into different scenarios for performance.
                MemoryCommit memoryCommit;
                if (firstParentCommit == null)
                {
                    memoryCommit = nanoRepo.commit(contentArea, revCommit.getFullMessage(), commitTags);
                }
                else
                {
                    if (otherParentCommits == null)
                    {
                        memoryCommit = nanoRepo.commit(contentArea, revCommit.getFullMessage(), commitTags, firstParentCommit);
                    }
                    else
                    {
                        memoryCommit = nanoRepo.commit(contentArea, revCommit.getFullMessage(), commitTags, firstParentCommit, otherParentCommits);
                    }
                }
                // Now we have committed the content area with its parent commit information.

                // Save this commit to our map for easy lookup by commit hash:
                commitsByCommitHash.put(revCommit.getName(), memoryCommit);

                // Checkout the revision:
                // System.out.println("Checking out...");
                // git
                //     .checkout()
                //     .setStartPoint(revCommit)
                //     .setAllPaths(true)
                //     .call();
                // System.out.println("Checking out done!");

            }
            // Now we have processed each commit.


            // Update the branch references for the repo:
            List<Ref> branchList = git.branchList().call();

            // Go through each branch and create it in our memory repo:
            for (Ref branchRef : branchList)
            {
                // Start with the original ref:
                Ref ref = branchRef;

                // Check whether the reference is peeled (annotated tags).
                if (!ref.isPeeled())
                {
                    // The reference is not peeled.
                    // Peel the reference:
                    ref = repository.getRefDatabase().peel(ref);
                }

                // Get the objectID for this reference:
                ObjectId objectId = ref.getPeeledObjectId();
                if (objectId == null)
                {
                    objectId = ref.getObjectId();
                }

                // Get the commit hash:
                String commitHash = objectId.getName();

                // Get the revCommit for this ref:
                MemoryCommit memoryCommit = commitsByCommitHash.get(commitHash);

                // Get the branch name:
                String branchName = Repository.shortenRefName(branchRef.getName());

                // Create the branch for this commit:
                nanoRepo.createBranchAtCommit(memoryCommit, branchName);
            }


            // Update the tag references for the repo:
            List<Ref> tagList = git.tagList().call();

            // Go through each tag and create it in our memory repo:
            for (Ref branchRef : tagList)
            {
                // Start with the original ref:
                Ref ref = branchRef;

                // Check whether the reference is peeled (annotated tags).
                if (!ref.isPeeled())
                {
                    // The reference is not peeled.
                    // Peel the reference:
                    ref = repository.getRefDatabase().peel(ref);
                }

                // Get the objectID for this reference:
                ObjectId objectId = ref.getPeeledObjectId();
                if (objectId == null)
                {
                    objectId = ref.getObjectId();
                }

                // Get the commit hash:
                String commitHash = objectId.getName();

                // Get the revCommit for this ref:
                MemoryCommit memoryCommit = commitsByCommitHash.get(commitHash);

                // Get the tag name:
                String tagName = Repository.shortenRefName(branchRef.getName());

                // Create the tag for this commit:
                nanoRepo.tagCommit(memoryCommit, tagName);
            }
        }

        return nanoRepo;
    }

    /**
     * This creates a {@link MemoryNanoRepo} for the entire contents of the given git repo.
     * The git repo has to already be passed in and the caller is responsible for closing it appropriately.
     *
     * @param existingGitWorkingDirectoryPath The path to an existing Git working directory to load.
     * @return The in-memory nano repository with the contents of the entire git repository.
     * @throws IOException     If files could not be opened or read correctly.
     * @throws GitAPIException If there were errors involved with accessing the Git repository.
     */
    public static MemoryNanoRepo createNanoRepoFromGitFilePath(Path existingGitWorkingDirectoryPath) throws IOException, GitAPIException
    {
        // Open the git repository:
        try (Git git = Git.open(existingGitWorkingDirectoryPath.toFile()))
        {
            // Read in the whole Git repo into memory:
            return createNanoRepoFromGitRepo(git);
        }
    }

    /**
     * This creates a {@link MemoryNanoRepo} for the entire contents of the given git repo.
     * The git repo has to already be passed in and the caller is responsible for closing it appropriately.
     *
     * @param uri                     The URI to the git repository to clone.
     * @param gitWorkingDirectoryPath The path to clone the Git working directory to.
     * @return The in-memory nano repository with the contents of the entire git repository.
     * @throws IOException     If files could not be opened or read correctly.
     * @throws GitAPIException If there were errors involved with accessing the Git repository.
     */
    public static MemoryNanoRepo createNanoRepoFromGitUrl(String uri, Path gitWorkingDirectoryPath) throws IOException, GitAPIException
    {
        // Clone the git repository:
        try (Git git = Git
            .cloneRepository()
            .setURI(uri)
            .setDirectory(gitWorkingDirectoryPath.toFile())
            .call()
        )
        {
            // Read in the whole Git repo into memory:
            return createNanoRepoFromGitRepo(git);
        }
    }
}
