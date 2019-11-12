package com.mycompany.dataextractor;

import com.github.mauricioaniche.ck.util.LOCCalculator;
import com.mycompany.model.MatrixData;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MatrixComputation {

    private static Repository repo;
    private static Git r;

    public List<MatrixData> computeMatrix(List<Integer> versions, Repository repository, Git git, List<MatrixData> matrixDataList) throws IOException, GitAPIException {
        repo = repository;
        r = git;
        List<Ref> tags = r.tagList().call();
        HashMap<String, Ref> refs = new HashMap<>();
        //using a range. So we need to add 1
        int lastVersion = versions.get(versions.size() - 1) + 1;
        int firstVersion = versions.get(0);
        if (lastVersion == firstVersion || lastVersion == tags.size() + 1) {
            return null;
        }
        refs.put("current", tags.get(lastVersion - 1));
        System.out.println("current " + tags.get(lastVersion - 1));
        refs.put("previous", tags.get(firstVersion - 1));
        System.out.println("previous " + tags.get(firstVersion - 1));
        Iterable<RevCommit> logs = getRevCommits(refs);

        RevCommit commitToCheck = null;
        int count = 0;
        if (logs.iterator().hasNext()) {
            count++;
            commitToCheck = logs.iterator().next();
            //String dateAsText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(commitToCheck.getCommitTime() * 1000L));
            //System.out.println("Commit message.: " + commitToCheck.getFullMessage() + " commit date " + dateAsText);
            //System.out.println("Tag for this commit: " + commitToCheck + " " + commitToCheck.getName());
        }
        System.out.println("total commits " + count);
        for (MatrixData matrixData : matrixDataList)
        {
            String[] classNames = matrixData.getClassName().split("\\.");
            String className = classNames[classNames.length - 1];
            try (TreeWalk tw = new TreeWalk(r.getRepository()))
            {
                if(commitToCheck.getTree()==null)
                    return null;
                tw.addTree(commitToCheck.getTree());
                tw.setRecursive(false);
                while (tw.next())
                {
                    if (tw.isSubtree())
                    {
                        tw.enterSubtree();
                    }
                    else
                        {
                        if (tw.getPathString().contains(className))
                        {
                            System.out.println("file: " + tw.getPathString() + " objectID " + tw.getObjectId(0));
                            ObjectId objectId = tw.getObjectId(0);
                            int loc = getLOC(objectId);
                            matrixData.setLoc(String.valueOf(loc));
                            break;
                        }
                    }
                }
            }
        }
        return matrixDataList;
    }

    private int getLOC(ObjectId objectId) throws IOException
    {
        ObjectLoader loader = repo.open(objectId);
        ObjectStream loaderstream = loader.openStream();
        // Calculate LOC of file
        int loc = LOCCalculator.calculate(loaderstream);
        System.out.println("loc " + loc);
        return loc;
    }

    private static ObjectId getActualRefObjectId(Ref ref)
    {
        final Ref repoPeeled = repo.peel(ref);
        if (repoPeeled.getPeeledObjectId() != null) {
            return repoPeeled.getPeeledObjectId();
        }
        return ref.getObjectId();
    }

    private static Iterable<RevCommit> getRevCommits(HashMap<String, Ref> refs) throws IOException, GitAPIException
    {
        // get a logcommand object to call commits
        LogCommand log = r.log();
        log.addRange(getActualRefObjectId(refs.get("previous")), getActualRefObjectId(refs.get("current")));
        // RevCommit object will contain all the commits for the release
        return log.call();
    }
}

