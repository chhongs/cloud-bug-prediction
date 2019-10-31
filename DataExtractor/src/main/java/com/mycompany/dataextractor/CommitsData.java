/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.dataextractor;

//import com.github.mauricioaniche.ck.util.*;
//import java.io.File;
import java.io.IOException;
//import java.nio.file.Paths;
//import java.util.Collection;
import java.util.List;
//import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
//import org.eclipse.jgit.revwalk.RevWalk;
import java.io.BufferedReader;
//import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
//import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;



public class CommitsData {
    // URL of remote repository
//    public static final String REMOTE_URL = "https://github.com/apache/hadoop.git";
    public static void main(String[] args) throws IOException, GitAPIException {
        // Create a repository object to hold current repository references (Local repository)
        Repository repo = new FileRepository("C:/HadoopProject/hadoop/.git");
        Git git = new Git(repo);
//        LOCCalculator obj = new LOCCalculator();
        List version = new ArrayList();
       // tags list will contain all the releases tags. For ex 0.92RC0
        List<Ref> tags = git.tagList().call();
        System.out.println(tags.size());
        // Currently we need only one release so first_refs will contain one release name tag
        List<Ref> first_refs =new ArrayList();
        first_refs.add(tags.get(0));
//        System.out.println(first_refs.get(0).getName().substring(10));
        version.add(first_refs.get(0).getName().substring(10));
//        System.out.println(version.get(0));
        
        // Loop over the release to get commits
        for (Ref ref : first_refs) {
            
//            version.add(ref.getName().substring(10));
            System.out.println("Tag: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            
            // get a logcommand object to call commits
            LogCommand log = git.log();
////
////            Ref peeledRef = repo.peel(ref);
////            if(peeledRef.getPeeledObjectId() != null) {
////                log.add(peeledRef.getPeeledObjectId());
////            } else {
////                log.add(ref.getObjectId());
////            }
            // RevCommit object will contain all the commits for the release
            Iterable<RevCommit> logs = log.call();
            for (RevCommit rev : logs) {
                 
//                System.out.println("Commit: " + rev);
                ArrayList<String> bug = new ArrayList<String>();
                 
                 // Get commit message to know about bug
                String commitmessage= rev.getFullMessage();
                commitmessage.toLowerCase();
                if (commitmessage.contains("error")||commitmessage.contains("fix")||commitmessage.contains("bug")||commitmessage.contains("failure")||commitmessage.contains("crash")||commitmessage.contains("wrong")||commitmessage.contains("unexpected")){
                    System.out.println("Commit message: " + rev.getFullMessage());
                    bug.add("yes");
                    
                }else{
                    bug.add("no");
                }
                // Store package name of a file in filename
//                List filenames = new ArrayList();
                ArrayList<String> filenames = new ArrayList<String>();
                // RevTree object to store the tree of files contained in a commit.
                RevTree tree = rev.getTree();
                // Get a treeWalk object to iterate over all files found in this commit
                TreeWalk treeWalk = new TreeWalk(repo);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    // Get all the Java files
                    if (treeWalk.getPathString().endsWith(".java")){
                        // treeWalk.getPathString() will give the path of java file in the commit.
                        System.out.println("found: " + treeWalk.getPathString());
//                        filenames.add(treeWalk.getNameString());
                        // objectId stores the first file Id
                        ObjectId objectId = treeWalk.getObjectId(0);
                        // loader object will open the file with given ID(objectId)
                        ObjectLoader loader = repo.open(objectId);
                        // Open stream for the file to read its contents
                        ObjectStream loaderstream = loader.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(loaderstream));
                        String line = reader.readLine();
                        while(line!=null){
                            // Skip the comments and empty lines from the Java file
                            if (line.isEmpty()||line.trim().startsWith("/*")||line.trim().startsWith("//")||line.trim().startsWith("*")){
                                line = reader.readLine();
                            }
                            // If line contains package info, get that line and extract package name. Example. com.mycompany.dataextractor
                            else if (line.trim().startsWith("package")){
                                String[] splitted = line.split("\\s+");
                                System.out.println(splitted[1]);
                                filenames.add(splitted[1]);
                                // Stop reading next lines
                                break;
                            }
                            else{
                                break;
                            }
                        }
                        // Close file reader object
                        reader.close();
                        System.out.println("Reader closed");
                        
                          // Code to get LOC of files, to be worked on later
//                        BufferedReader reader_loc = new BufferedReader(new InputStreamReader(loaderstream));
//                        int loc = obj.calculate(reader_loc);
//                        System.out.println("Lines of Code: "+loc);
//                        reader.close();
//                        System.out.println("Reader closed");
                        
                    }
                }
            }
            
        }
        

 
}
    
}
