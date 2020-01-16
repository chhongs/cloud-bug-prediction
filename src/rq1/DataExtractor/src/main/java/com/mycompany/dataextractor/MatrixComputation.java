package com.mycompany.dataextractor;

import com.github.mauricioaniche.ck.CK;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MatrixComputation {

    private static Repository repo;
    private static Git r;

    public List<MatrixData> computeMatrix(List<Integer> versions, Repository repository, Git git, List<MatrixData> matrixDataList, String repoPath) throws IOException, GitAPIException {
        repo = repository;
        r = git;
        //Tags are all release names
        List<Ref> tags = r.tagList().call();
        HashMap<String, Ref> refs = new HashMap<>();
        //using a range. So we need to add 1
        int lastVersion = versions.get(versions.size() - 1) + 1;
        int firstVersion = versions.get(0);
        if (lastVersion == firstVersion || lastVersion == tags.size() + 1) {
            return null;
        }
        // Get next release version from version list
        refs.put("current", tags.get(lastVersion - 1));
        //System.out.println("current " + tags.get(lastVersion - 1));
        refs.put("previous", tags.get(firstVersion - 1));
        //System.out.println("previous " + tags.get(firstVersion - 1));
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

        for (MatrixData matrixData : matrixDataList)
        {
            String[] classNames = matrixData.getClassName().split("\\.");
            String className = classNames[classNames.length - 1];
            String packageName = matrixData.getClassName();
            try (TreeWalk tw = new TreeWalk(r.getRepository()))
            {
                if(commitToCheck== null || commitToCheck.getTree()==null)
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
                            //System.out.println("file: " + tw.getPathString() + " objectID " + tw.getObjectId(0));
                            //System.out.println("class name: " + className);
                            ObjectId objectId = tw.getObjectId(0);
                            try {
                                //System.out.println("repo path"+repoPath);
                                // Add path of Local git repository to file path
                                String pathToFile = repoPath.substring(0,repoPath.lastIndexOf("\\")+1)+tw.getPathString();

                                //System.out.println("Package name: "+packageName); // For ex org.apache.hadoop.Test(For Test.java file)
                                List<Integer> metrics_list = calculateMetrics(pathToFile, packageName);
                                System.out.println(metrics_list.get(0).toString());
                                System.out.println(metrics_list.get(1).toString());
                                System.out.println(metrics_list.get(2).toString());
                                System.out.println(metrics_list.get(3).toString());
                                System.out.println(metrics_list.get(4).toString());
                                matrixData.setLoc(metrics_list.get(0).toString());
                                matrixData.setWmc(metrics_list.get(1).toString());
                                matrixData.setDit(metrics_list.get(2).toString());
                                matrixData.setCbo(metrics_list.get(3).toString());
                                matrixData.setRfc(metrics_list.get(4).toString());
                                matrixData.setLcom(metrics_list.get(5).toString());
                                break;
                            }
                            catch(Exception ex){
                                // Skip files that are not found.
                                //System.out.println(" Exception for file occurred.");
                                matrixData = null;
//                                matrixData.setLoc("Null");
//                                matrixData.setWmc("Null");
//                                matrixData.setDit("Null");
//                                matrixData.setCbo("Null");
//                                matrixData.setRfc("Null");
//                                matrixData.setLcom("Null");
                                break;
                            }
                        }
                    }
                }
            }
        }

        return matrixDataList;
    }
    /* We are already calculating LOC in calculateMetrics() method*/
    private int getLOC(ObjectId objectId) throws IOException
    {
        ObjectLoader loader = repo.open(objectId);
        ObjectStream loaderstream = loader.openStream();
        // Calculate LOC of file
        int loc = LOCCalculator.calculate(loaderstream);
        //System.out.println("loc " + loc);
        return loc;
    }

    private static List<Integer> calculateMetrics(String filepath, String class_name)
    {

        Boolean useJars = true;
        List<Integer> metrics_list = new ArrayList<>();
        System.out.println("check CK");
        new CK().calculate(filepath, useJars, result -> {
            System.out.println("check CK"+result.getClassName()+class_name);
            //If class name of Java file matches with classes fetched by CK(), then calculate metrics for the class/Java file
            if (result.getClassName().equals(class_name)) {
                System.out.println("equaln");
                metrics_list.add(result.getLoc());
                metrics_list.add(result.getWmc());
                metrics_list.add(result.getDit());
                metrics_list.add(result.getCbo());
                metrics_list.add(result.getRfc());
                metrics_list.add(result.getLcom());

                /*System.out.println("Class name: " + result.getClassName());
                System.out.println("DIT value: " + result.getDit());
                System.out.println("CBO value: " + result.getCbo());
                System.out.println("WMC value: " + result.getWmc());
                System.out.println("LCOM value: " + result.getLcom());*/
            }
            else {
                System.out.println("not equal");
            }
        });
        // LOC, WMC, DIT, CBO, RFC and LCOM metrics
        return metrics_list;
    }

    private static ObjectId getActualRefObjectId(Ref ref)
    {
        // Get commit Id
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
        // Add Release/Tag Id to get logs/commits for this release
        log.addRange(getActualRefObjectId(refs.get("previous")), getActualRefObjectId(refs.get("current")));
//        log.add(getActualRefObjectId(refs.get("previous")));
        // RevCommit object will contain all the commits for the release
        return log.call();
    }
}

