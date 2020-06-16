package io.nanovc.nano_jgit_analyzer;

import io.nanovc.junit.TestDirectory;
import io.nanovc.junit.TestDirectoryExtension;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestDirectoryExtension.class)
public class JGitTests
{

    /**
     * This is from the getting started guide for JGit here:
     * https://www.codeaffine.com/2015/12/15/getting-started-with-jgit/
     *
     * This article was written by:
     * Rüdiger Herrmann
     * Routined programmer, team lead, presenter, blog writer,
     * and convinced open source contributor with two decades experience in the field.
     *
     * Strong focus on quality backed up by agile methods like test driven development,
     * modularization, pair programming, clean code, continuous integration.
     *
     * Specialized in Java with JEE, REST, OSGi, RCP, RAP and
     * building developer tools based on the Eclipse IDE and Visual Studio Code.
     */
    @Test
    public void gettingStartedWithJGit(@TestDirectory(useTestName = true) Path testDirectory) throws GitAPIException, IOException
    {
        assertNotNull(testDirectory);
        assertTrue(Files.exists(testDirectory));

        // ** Getting Started with JGit **
        // On December 15, 2015, Posted by Rüdiger Herrmann,
        // In Eclipse, By Eclipse,Git,HowTo,JGit , With 37 Comments
        // If you ever wondered how basic Git commands like git init, git checkout
        //  and so on are executed in JGit, read on.

        // This tutorial gives an overview of the most commonly used git commands and their counterparts in JGit.
        // It walks through the steps to create a repository, fetch contents from a remote, add and remove files to/from the history, inspect the history, and finally push back the changes to the originating repository.

        // JGit provides an API that is similar to the Git high-level commands. Instead of

        /*
        git commit -m "Gabba Gabba Hey"
         */

        // on the command line, you would write
        /*
        git.commit().setMessage( "Gabba Gabba Hey" ).call();
         */
        // in JGit.

        // All JGit commands have a call() method that,
        // after setting up the command is used to actually execute it.
        // The classes are named after the respective Git command with the suffix Command.
        // While some commands offer a public constructor,
        // it is recommended to use the Git factory class to create command
        // instances like shown in the above example.

        // ** Getting the Library **
        // But before diving further into the JGit API, let’s get hold of the library first.
        // The most common way to get JGit is probably from the Maven repository.
        // But if you prefer OSGi bundles, then there is also a p2 repository for you.
        // The download page (http://www.eclipse.org/jgit/download/)
        // lists the necessary information to integrate the library.
        //
        // For the scope of this article,
        // it is sufficient to integrate what is referred to as the core library in
        // project/bundle org.eclipse.jgit.
        // If you are interested what else there is in the JGit source code repository,
        // I recommend reading the Introduction to the JGit Sources (https://www.codeaffine.com/2013/10/28/an-introduction-to-the-jgit-sources/)


        // ** Creating a Repository **
        // To start with, we need a repository.
        // And to get hold of such thing, we can either initialize a new repository or clone an existing one.

        // The InitCommand lets us create an empty repository. The following line

        /* Git git = Git.init().setDirectory( "/path/to/repo" ).call(); */
        Git gitInit = Git.init().setDirectory(testDirectory.resolve("init").toFile()).call();

        // will create a repository with a work directory at the location given to setDirectory().
        // The .git directory will be directly underneath in /path/to/repo/.git.
        // For a detailed explanation of the InitCommand please read the
        // Initializing Git Repositories with JGit article (https://www.codeaffine.com/2015/05/06/jgit-initialize-repository/).

        // An existing repository can be cloned with the CloneCommand

        /*
        Git git = Git.cloneRepository()
          .setURI( "https://github.com/eclipse/jgit.git" )
          .setDirectory( "/path/to/repo" )
          .call();
         */
        Git gitClone = Git.cloneRepository()
            .setURI( "https://github.com/nanovc/nano-jgit-analyzer.git" )
            .setDirectory(testDirectory.resolve("clone").toFile())
            .call();

        // The code above will clone the JGit repository into the local directory ‘path/to/repo’.
        // All options of the CloneCommand are explained in depth in
        // How to Clone Git Repositories with JGit (https://www.codeaffine.com/2015/11/30/jgit-clone-repository/).

        // If you happen to have an existing local repository already that you wish to use,
        // you can do so as described in How to Access a Git Repository with JGit (https://www.codeaffine.com/2014/09/22/access-git-repository-with-jgit/).

        // ** Close Git When Done **
        // Note that commands that return an instance of Git like the\
        // InitCommand or CloneCommand may leak file handles if they are
        // not explicitly closed (git.close()) when no longer needed.
        // Fortunately, Git implements AutoCloseable so that you can use the try-with-resources statement.
        gitInit.close();
        gitClone.close();

        // Populating a Repository
        // Now that we have a repository, we can start populating its history.
        // But to commit a file, we first need to add it to the so-called index (aka staging area).
        // The commit command will only consider files that are added to (or removed from) the index.

        Path gitPopulatingPath = testDirectory.resolve("populating");
        try(Git git = Git.init().setDirectory(gitPopulatingPath.toFile()).call())
        {
            // The JGit command therefore is – you guess it – the AddCommand.
            /* DirCache index = git.add().addFilePattern( "readme.txt" ).call(); */
            Files.writeString(gitPopulatingPath.resolve("readme.txt"), "Hello World!", StandardOpenOption.CREATE_NEW);
            DirCache index1 = git.add().addFilepattern("readme.txt").call();

            // Consequently the above line adds the file readme.txt to the index.
            // It is noteworthy that the actual contents of the file are copied to the index.
            // This means that later modifications to the file will not be contained in the index,
            // unless they are added again.
            //
            // The path given to addFilePattern() must be relative to the work directory root.
            // If a path does not point to an existing file, it is simply ignored.
            //
            // Though the method name suggests that also patterns are accepted,
            // the JGit support, therefore, is limited.
            // Passing a ‘.’ will add all files within the working directory recursively.
            // But fileglobs (e.g. *.java) as they are available in native Git are not yet supported.
            //
            // The index returned by call(), in JGit named DirCache,
            // can be examined to verify that it actually contains what we expect.
            // Its getEntryCount() method returns the total number of files
            // and getEntry() returns the entry at the specified position.

            // Now everything is prepared to use the CommitCommand in order to store
            // the changes in the repository.

            /* RevCommit commit = git.commit().setMessage( "Create readme file" ).call(); */
            RevCommit commit1 = git.commit().setMessage("Create readme file").call();

            // At least the message must be specified, otherwise call() will complain with a NoMessageException.
            // An empty message, however is allowed. The author and committer are taken from the configuration
            // if not denoted with the accordingly labelled methods.
            //
            // The returned RevCommit describes the commit with its message, author, committer, time stamp,
            // and of course a pointer to the tree of files and directories that constitute this commit.
            //
            // In the same way that new or changed files need to be added,
            // deleted files need to be removed explicitly.
            // The RmCommand is the counterpart of the AddCommand and can be used in the same way
            // (with the contrary result of course).

            /* DirCache index = git.rm().addFilepattern( "readme.txt" ).call(); */
            DirCache index2 = git.rm().addFilepattern( "readme.txt" ).call();

            // The above line will remove the given file again.
            // Since it is the only file within the repository,
            // the returned index will return zero when asked for the number of entries in it.

            // Unless setCached( true ) was specified,
            // the file will also be deleted from the work directory.
            // Because Git does not track directories (https://git.wiki.kernel.org/index.php/Git_FAQ#Can_I_add_empty_directories.3F)
            // the RmCommand also deletes empty parent directories of the given files.

            // An attempt to remove a non-existing file is ignored.
            // But unlike the AddCommand, the RmCommand does not accept wildcards in its addFilepattern() method.
            // All files to be removed need to be specified individually.
            //
            // And with the next commit, these changes will be stored in the repository.
            // Note that it is perfectly legal to create an empty commit,
            // i.e. one that hasn’t had files added or removed before executed.
            // Though I’m not aware of a decent use case.


            // ** State of a Repository **
            // The status command lists files that have differences
            // between either the index and the current HEAD commit
            // or the working directory and the index or files that
            // are not tracked by Git.
            //
            // In its simplest form, the StatusCommand collects the
            // status of all files that belong to the repository:
            /* Status status = git.status().call(); */
            Status status1 = git.status().call();

            // The getters of the Status object should be self-explaining.
            // They return the set of file names which are in the state that the method name describes.
            // For example, after the readme.txt file was added to the index like shown previously,
            // status.getAdded() would return a set that contains the path to the just added file.
            //
            // If there are no differences at all and no untracked files either,
            // Status.isClean() will return true.
            // And as its name implies, returns Status.hasUncommittedChanges()
            // true if there are uncommitted changes.
            //
            // With addPath(), the StatusCommand can be configured to show only the status of certain files.
            // The given path must either name a file or a directory.
            // Non-existing paths are ignored and regular expressions or wildcards are not supported.

            /* Status status = git.status().addPath( "documentation" ).call(); */
            Path documentationPath = gitPopulatingPath.resolve("documentation");
            Files.createDirectories(documentationPath);
            Files.writeString(documentationPath.resolve("Doc1.txt"), "Documentation 1", StandardOpenOption.CREATE_NEW);
            Files.writeString(documentationPath.resolve("Doc2.txt"), "Documentation 2", StandardOpenOption.CREATE_NEW);
            Status status2 = git.status().addPath( "documentation" ).call();

            // In the above example, the status of all files recursively underneath the
            // ‘documentation’ directory will be computed.

            // ** Exploring a Repository **

            // Now that the repository has a (small) history we will look into the command to list existing commits.
            //
            // The simplest form of the git log counterpart of JGit allows to list all commits
            // that are reachable from current HEAD.

            /* Iterable<RevCommit> iterable = git.log().call(); */

            // The returned iterator can be used to loop over all commits that are found by the LogCommand.
            //
            // For more advanced use cases I recommend using the RevWalk API directly,
            // the same class that is also used by the LogCommand.
            // Apart from providing more flexibility it also avoids a possible resource leak
            // that occurs because the RevWalk that is used internally by the LogCommand is never closed.
            //
            // For example, its markStart() method can be used to also list commits that are reachable
            // from other branches (or more generally speaking from other refs).
            //
            // Unfortunately, only ObjectIds are accepted and therefore the desired refs
            // need to be resolved first. An ObjectId in JGit encapsulates an SHA-1 hash
            // that points to an object in Gits object database.
            // Here, ObjectIds, that point to commits, are required and resolving in this
            // context means to obtain the ObjectId that a particular ref points to.
            //
            // Putting it all together, it looks like the snippet below:
            /*
            Repository repository = git.getRepository()
            try( RevWalk revWalk = new RevWalk( repository ) ) {
              ObjectId commitId = repository.resolve( "refs/heads/side-branch" );
              revWalk.markStart( revWalk.parseCommit( commitId ) );
              for( RevCommit commit : revWalk ) {
                System.out.println( commit.getFullMessage );
              }
            }
             */
            Repository repository = git.getRepository();
            try(RevWalk revWalk = new RevWalk(repository))
            {
                ObjectId commitId = repository.resolve("refs/heads/master" );
                revWalk.markStart( revWalk.parseCommit( commitId ) );
                for( RevCommit commit : revWalk ) {
                    System.out.println( commit.getFullMessage());
                }
            }

            // The commit id to which the branch ‘side-branch’ points is obtained
            // and then the RevWalk is instructed to start iterating over the history from there.
            // Because markStart() requires a RevCommit,
            // RevWalk’s parseCommit() is used to resolve the commit id into an actual commit.
            //
            // Once the RevWalk is set up, the snippet loops over the commits
            // to print the message of each commit.
            // The try-with-resource statement ensures that the RevWalk will be closed when done.
            // Note that it is legal to call markStart() multiple times to include multiple refs into the traversal.
            //
            // A RevWalk can also be configured to filter commits,
            // either by matching attributes of the commit object itself or by
            // matching paths of the directory tree that it represents.
            // If known in advance, uninteresting commits and their ancestry chain can be excluded from the output.
            // And of course, the output can be sorted, for example by date or topologically
            // (all children before parents).
            // But these features are outside of the scope of this article
            // but may be covered in a future article of its own.


            // ** Exchanging with a Remote Repository **

            // Often a local repository was cloned from a remote repository.
            // And the changes that were made locally should ultimately be
            // published to the originating repository.
            // To accomplish this, there is the PushCommand, the counterpart of git push.
            //
            // The simplest form will push the current branch to its corresponding remote branch.

            /*
            Iterable<PushResult> iterable = local.push().call();
            PushResult pushResult = iterable.iterator().next();
            Status status = pushResult.getRemoteUpdate( "refs/heads/master" ).getStatus();
             */

            // The command returns an iterable of PushResults.
            // In the above case the iterable holds a single element.
            // To verify that the push succeeded, the pushResult can be asked to
            // return a RemoteRefUpdate for a given branch.
            //
            // A RemoteRefUpdate describes in detail what was updated and how it was updated.
            // But it also has a status attribute that summarizes the outcome.
            // And if the status returns OK, we can rest assured that the operation succeeded.
            //
            // Even though the command works without giving any advice, it has plenty of options.
            // However, in the following only the more commonly used are listed.
            // By default, the command pushes to the default remote called ‘origin’.
            // Use setRemote() to specify the URL or name of a different remote repository.
            // If other branches than the current one should be pushed refspecs
            // can be specified with setRefSpec().
            // Whether tags should also be transferred can be controlled with setPushTags().
            // And finally, if you are uncertain whether the outcome is desired,
            // there is a dry-run option that allows simulating a push operation.
            //
            // Now that we have seen how to transfer local objects to a
            // remote repository we will look a how the opposite direction works.
            // The FetchCommand can be used much like its push counterpart and also
            // succeeds with its default settings.

            /*
            FetchResult fetchResult = local.fetch().call();
            TrackingRefUpdate refUpdate = fetchResult.getTrackingRefUpdate( "refs/remotes/origin/master" );
            Result result = refUpdate.getResult();
             */

            // Without further configuration, the command fetches changes from
            // the branch that corresponds to the current branch on the default remote.
            //
            // The FetchResult provides detailed information about the outcome of the operation.
            // For each affected branch, a TrackingRefUpdate instance can be obtained.
            // Most interesting probably is the return value of getResult() that summarizes
            // how the update turned out. In addition it holds information about which
            // local ref (getLocalName()) was updated with which remote ref (getRemoteName())
            // and to which object id the local ref pointed before and after the update
            // (getOldObjectId() and getNewObjectid()).
            //
            // If the remote repository requires authentication,
            // the PushCommand and FetchCommand can be prepared in the same way as all commands
            // that communicate with remote repositories.
            // A detailed discussion can be found in the JGit Authentication Explained article.
            // https://www.codeaffine.com/2014/12/09/jgit-authentication/
        }

        // ** Concluding Getting Started with JGit **
        // Now it is your turn to take JGit for a spin.
        // The high-level JGit API isn’t hard to understand.
        // If you know what git command to use, you can easily guess which classes and methods to use in JGit.
        //
        // While not all subtleties of the of the Git command line are available,
        // there is solid support for the most often used functionalities.
        // And if there is something crucial missing,
        // you can often resort to the lower-level APIs of JGit to work around the limitation.
        //
        // The snippets shown throughout the article are excerpts of a collection of learning tests.
        // The full version can be found here:
        // https://gist.github.com/rherrmann/433adb44b3d15ed0f0c7
        //
        // If you still have difficulties or questions,
        // please leave a comment or ask the friendly and helpful JGit community for assistance.
        // https://www.eclipse.org/jgit/support/
    }
}
