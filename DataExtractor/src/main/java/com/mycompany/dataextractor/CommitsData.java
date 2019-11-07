/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.dataextractor;


import com.github.mauricioaniche.ck.util.LOCCalculator;
import com.google.common.base.Strings;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;


public class CommitsData {

    private static Repository repo;
    private static Git git;

    // URL of remote repository
//    public static final String REMOTE_URL = "https://github.com/apache/hadoop.git";
    public static void main(String[] args) throws IOException, GitAPIException {
        String repoPath = args[0];

        // Create a repository object to hold current repository references (Local repository)
        repo = new FileRepository(repoPath);
        git = new Git(repo);
        LOCCalculator obj = new LOCCalculator();

        // version list stores the repository version tags
        List<String> version = new ArrayList<>();

        // commit_names stores all commits in a revision
        List<RevCommit> commit_names = new ArrayList<>();

        // loc_list stores Lines of Code for all files of a specific commit
        Map<Integer, List<Integer>> loc_list = new HashMap<>();

        // bug list will store "yes" if there is a bug in a commit, otherwise "no"
        ArrayList<String> bug = new ArrayList<>();

        //filenames stores name of Java file with its package name
        Map<Integer, List<String>> filenames = new HashMap<>();

        // Hashmap for total files in each commit
        Map<Integer, Integer> total_files_in_commit = new HashMap<>();

        // Temporarily stores a file path
        String temp_filepath;

        // Project name is static for now
        String project_name = "hadoop";

        // tags list will contain all the releases tags. For ex 0.92RC0
        List<Ref> tags = git.tagList().call();
        System.out.println(tags.size());

        // Currently we need only one release so first_refs will contain one release name tag
        List<Ref> first_refs = new ArrayList<>();
        first_refs.add(tags.get(0));
//        System.out.println(first_refs.get(0).getName().substring(10));
//        version.add(first_refs.get(0).getName().substring(10));
//        System.out.println(version.get(0));

        // Loop over the release to get commits
        for (Ref ref : first_refs) {
            version.add(ref.getName().substring(10));
            System.out.println("Tag: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            Iterable<RevCommit> logs = getRevCommits(ref);

            //            List<RevCommit> commitList = Lists.newArrayList(logs);
//            List<RevCommit> first_commit = new ArrayList();
//            first_commit.add(commitList.get(0));
//            System.out.println(first_commit.size());

            // temporary counter for testing(take 100 commits for testing purpose)
            int mycount = 0;
            for (RevCommit rev : logs) {
                mycount++; //temporary break
                System.out.println("Commit No.: " + mycount);
                System.out.println("Commit message.: " + rev.getFullMessage());
//                System.out.println("Tag for this commit: " + ref + " " + ref.getName());
                commit_names.add(rev);
//                if (mycount==11){
//                    break;
//                }
                System.out.println("Commit: " + rev.getName());
                String bugStatus = getBugStatus(rev);
                bug.add(bugStatus);

                //                ArrayList<String> filenames = new ArrayList<String>();
//                ArrayList<Integer> loc_list = new ArrayList<>();

                // RevTree object to store the tree of files contained in a commit.
//                RevTree tree = commitid.getTree();
//                System.out.println("Tree: " +tree.getName());
//                int counter = 0;
            }

            System.out.println("Total Commits in this release is: " + commit_names.size());

            // Iterate on commits to extract data(Files, LOC etc.)
            for (int k = 0; k < commit_names.size() - 1; k++) {
                //Get modifications between two commits
                CanonicalTreeParser treeParser1 = new CanonicalTreeParser();
                ObjectId treeId1 = commit_names.get(k).getTree().getId();
                try (ObjectReader reader = repo.newObjectReader()) {
                    treeParser1.reset(reader, treeId1);
                }
                CanonicalTreeParser treeParser2 = new CanonicalTreeParser();
                ObjectId treeId2 = commit_names.get(k + 1).getTree().getId();
                try (ObjectReader reader1 = repo.newObjectReader()) {
                    treeParser2.reset(reader1, treeId2);
                }
                DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream()); // use NullOutputStream.INSTANCE if you don't need the diff output
                df.setRepository(git.getRepository());
                // Entries stores all the files with path which have MODIFIED, DELETED, or ADDED
                List<DiffEntry> entries = df.scan(treeParser1, treeParser2);
                System.out.println("Entry count: " + entries.size());
                System.out.println("Entry size: " + entries.size());

                // records number of files for a specific commit
//            ArrayList<Integer> total_files_in_commit = new ArrayList<>();

                // Iterate all changed(modified, deleted etc) files between these commits
                for (DiffEntry entry : entries) {
                    //To store file name/Package name for current file in the loop
                    ArrayList<String> file_list_for_commit = new ArrayList<>();
                    //To store Lines of Code for current file in the loop
                    ArrayList<Integer> loc_list_for_commit = new ArrayList<>();
                    //Counter for total files in commit
                    int total_files = 0;
                    System.out.println(entry);
                    //Find Java files
                    if (entry.getNewPath().endsWith(".java")) {
                        temp_filepath = entry.getNewPath();
                        temp_filepath = temp_filepath.substring(temp_filepath.lastIndexOf("/") + 1);
                        // remove extension of file name
                        temp_filepath = FilenameUtils.removeExtension(temp_filepath);
//                  System.out.println("File class name: "+temp_filepath);
                        System.out.println("Filename getNewPath(): " + entry.getNewPath());
                        // If file is not DELETED, get its LOC and package name
                        if (!entry.getChangeType().toString().equals("DELETE")) {

                            total_files++;
//                      System.out.println("file count: "+total_files_in_commit);
                            System.out.println("File not deleted: " + entry.getNewPath());
                            System.out.println("File not deleted: " + entry.getNewId().toObjectId());


                            // objectId stores ID of current file
                            ObjectId objectId = entry.getNewId().toObjectId();

                            // loader object will open the file with given ID(objectId)
                            ObjectLoader loader = repo.open(objectId);

//                        String fileContent = new String(loader.getBytes());

                            // Open stream for the file to read its contents
                            ObjectStream loaderstream = loader.openStream();

                            try ( // Read contents of file to get package name
                                  BufferedReader reader = new BufferedReader(new InputStreamReader(loaderstream))) {
                                String line = reader.readLine();
                                while (line != null) {


                                    // Skip the comments and empty lines from the Java file
                                    if (line.isEmpty() || line.trim().startsWith("/*") || line.trim().startsWith("//") || line.trim().startsWith("*") || line.trim().startsWith("@")) {
                                        line = reader.readLine();
                                    }
                                    // If line contains package info, get that line and extract package name. Example. com.mycompany.dataextractor
                                    else if (line.trim().startsWith("package")) {
                                        // Split line defining package name. Ex "package org.apache.hadoop;"
                                        String[] splitted = line.split("\\s+");
                                        // Append package name to add file name
                                        file_list_for_commit.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));

                                        System.out.println(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                        // Stop reading next lines
                                        break;
                                    } else {
                                        break;
                                    }

                                }
//                          while(line!=null){
//                              // add line to stringBuilder sb
//                              sb.append(line);
//                              sb.append('\n');
//                              line=reader.readLine();
//                          }
                                // Build a string out of file contents
//                          fileContent = sb.toString();

                                // Close file reader object
                                reader.close();
                                System.out.println("Reader closed");
                            } catch (IOException ex) {
                                System.out.println(ex);
                            }


                            // Calculate LOC of file
                            int loc = obj.calculate(loaderstream);
                            loc_list_for_commit.add(loc);

                            // metrics
//                        List<String> list_of_metrics= new ArrayList<String>();
//                        list_of_metrics.add("CBO");
//                        List<String> methodlevel_metrics= new ArrayList<String>();
//                        methodlevel_metrics.add("WMC");
//                        MetricsExecutor me= new MetricsExecutor(list_of_metrics,methodlevel_metrics,);

                            // Calculate class metrics

//                        CKClassResult metric_result= new CKClassResult(fileContent, temp_filepath,"java.");
//                        System.out.println("CBO is: "+ metric_result.getCbo());

//                        DIT dit_obj= new DIT();
//                        dit_obj.setResult(metric_result);
//                        System.out.println("DIT is: "+ metric_result.getDit());
                            System.out.println("Lines of Code: " + loc);
                        } else {   // If no java files in current commit, then continue to the next commit
//                      file_list_for_commit.add("No file");
//                      loc_list_for_commit.add(0);
                            continue;

                        }
                    }
                    //Add file_list_for_commit list to HashMap where key is 'k'
                    filenames.put(k, file_list_for_commit);
                    //Add loc_list_for_commit list to HashMap where key is 'k'
                    loc_list.put(k, loc_list_for_commit);
                    //Add total_files for a commit to HashMap where key is 'k'
                    total_files_in_commit.put(k, total_files);
//              file_list_for_commit.clear();
//              loc_list_for_commit.clear();
                } //diff entry loop between two commits
            }
        }
// To remove null pointers from the following HashMaps
//        filenames.values().removeAll(Collections.singleton(null));
//        loc_list.values().removeAll(Collections.singleton(null));
//        total_files_in_commit.values().removeAll(Collections.singleton(null));
//        while(filenames.values().remove(null));
//        while(loc_list.values().remove(null));
//        while(total_files_in_commit.values().remove(null));
        System.out.println("Total Commits in this release is: " + commit_names.size());
        System.out.println("Exit from loops. Creating CSV.");
//       try{

        //create a CSV file under "csvfiles" folder in current path
        try (FileWriter csvWriter = new FileWriter("csvfiles/hadoop.csv", false)) {
            // Add column headings to CSV file
            csvWriter.append("name-pr");
            csvWriter.append(",");
            csvWriter.append("version");
            csvWriter.append(",");
            csvWriter.append("name");
            csvWriter.append(",");
            csvWriter.append("loc");
            csvWriter.append(",");
            csvWriter.append("bug");
            csvWriter.append("\n");
            csvWriter.flush();
        } catch (IOException e) {
            System.err.println("Can't create CSV file. May be the required file is open.");
        }

        // Append new data to the CSV file
        try (FileWriter csvWriter = new FileWriter("csvfiles/hadoop.csv", true)) {
            //row_data stores data for each row in CSV
            StringBuilder row_data = new StringBuilder();
//        for( int i=0; i<version.size();i++){
            // Get all keys from HashMap "filenames"
            Set<Integer> keys = filenames.keySet();
//                int size=filenames.size();

            //Iterate on keys to get data for CSV file
            for (int i : keys) {
//                for (int i=0;i<commit_names.size()-1;i++){
                // Check for Null pointer(Jump to next key if "Null" value)
                boolean value = filenames.get(i).isEmpty();
                if (value) {
                    continue;
                }
                //Get list of file names(for a commit) from hashmap 'filenames'
                List<String> temp_file_names = filenames.get(i);

                //Get list of file LOC(for a commit) from hashmap 'loc_list'
                List<Integer> temp_file_loc = loc_list.get(i);

                //Get files_per_commit from hashmap 'total_files_in_commit'
                int files_per_commit = total_files_in_commit.get(i);
                // Store data into CSV
                for (int j = 0; j < files_per_commit; j++) {
                    System.out.println("j value: " + j);
                    // 'v' is release name
                    String v = version.get(0);
                    // 'pr' is project name for this commit
                    String pr = project_name;
                    // 'name' stores name of file(java package) for current commit and current file in this commit
                    String name = temp_file_names.get(j);
                    // 'b' stores bug for current commit
                    String b = bug.get(i);
                    // 'l' stores lines of code for current commit and current file in this commit
                    int l = temp_file_loc.get(j);
                    row_data.append(pr);
                    row_data.append(",");
                    row_data.append(v);
                    row_data.append(",");
                    row_data.append(name);
                    row_data.append(",");
                    row_data.append(l);
                    row_data.append(",");
                    row_data.append(b);
                    csvWriter.append(row_data.toString());
                    csvWriter.append("\n");
                    row_data.setLength(0);
                }

                csvWriter.flush();
                System.out.println("CSV file created successfully!");
            }
        } catch (IOException e) {
            System.err.println("Can't create CSV file. May be the required file is open.");
        }

    }

    /**
     * Return whether commit handles/fixes a bug.
     */
    private static String getBugStatus(RevCommit rev) {
        // Get commit message to know about bug
        String commitmessage = rev.getFullMessage();
        if (Strings.isNullOrEmpty(commitmessage)) {
            return "-";
        } else {
            commitmessage = commitmessage.toLowerCase();
            if (commitmessage.contains("error") || commitmessage.contains("fix") || commitmessage.contains("bug") || commitmessage.contains("failure") || commitmessage.contains("crash") || commitmessage.contains("wrong") || commitmessage.contains("unexpected")) {
//                    System.out.println("Commit message: " + rev.getFullMessage());
                return "yes";

            } else {
                return "no";
            }

        }
    }

    /**
     * Get all commits of a release
     */
    private static Iterable<RevCommit> getRevCommits(Ref ref) throws IOException, GitAPIException {
        // get a logcommand object to call commits
        LogCommand log = git.log();

        // Get commit Id in peeledRef, also add Release/Tag Id to get logs/commits for this release
        Ref peeledRef = repo.getRefDatabase().peel(ref);
        if (peeledRef.getPeeledObjectId() != null) {
            System.out.println("Peeled");
            log.add(peeledRef.getPeeledObjectId());
//                log.addRange(peeledRef.getPeeledObjectId(), peeledRef.getPeeledObjectId());
        } else {
            log.add(ref.getObjectId());
//                log.addRange(ref.getObjectId(), ref.getObjectId());
            System.out.println(" not Peeled: " + ref.getObjectId());
        }

        // RevCommit object will contain all the commits for the release
        return log.call();
    }

}
