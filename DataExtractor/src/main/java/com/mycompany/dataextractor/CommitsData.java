/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.dataextractor;


import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.mauricioaniche.ck.metric.RFC;
import com.github.mauricioaniche.ck.util.LOCCalculator;
import com.github.mauricioaniche.ck.util.ResultWriter;

import java.io.*;


import com.google.common.base.Strings;

import java.util.*;

import com.opencsv.CSVReader;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
//import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.concurrent.Callable;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;




public class CommitsData {

    private static Repository repo;
    private static Git git;

    // URL of remote repository
//    public static final String REMOTE_URL = "https://github.com/apache/hadoop.git";
    public static void main(String[] args) throws IOException, GitAPIException {
        String repoPath = "/Users/hadoop";
        // Create a repository object to hold current repositorßy references (Local repository)
        repo = new FileRepository(repoPath+"/.git");
        git = new Git(repo);

        // commit_names stores all commits in a revision
        List<RevCommit> commit_names = new ArrayList<>();

        // bug list will store "yes" if there is a bug in a commit, otherwise "no"
        ArrayList<String> bug = new ArrayList<>();

        Map<Integer, List<String>> filenames = new HashMap<>();

        // Hashmap for total files in each commit
        Map<Integer, Integer> total_files_in_commit = new HashMap<>();

        // Temporarily stores a file path
        String temp_filepath;
        Map<String, String> filepaths = new HashMap<>();

        ArrayList<Integer> loc_list_for_commit = new ArrayList<>();
        // To store WMC metric value for current file in a commit
        ArrayList<Integer> wmc_list_for_commit = new ArrayList<>();

        // To store CBO metric value for current file in a commit
        ArrayList<Integer> cbo_list_for_commit = new ArrayList<>();

        // To store DIT metric value for current file in a commit
        ArrayList<Integer> dit_list_for_commit = new ArrayList<>();

        // To store LCOM metric value for current file in a commit
        ArrayList<Integer> lcom_list_for_commit = new ArrayList<>();

        //get a certain release
        List<Ref> list = git.tagList().call();
        Ref release = list.get(0);
        String version = release.getName().replace("refs/tags/", "");
        System.out.println("Release: " + version);

        Iterable<RevCommit> logs = getRevCommits(release);
        for (RevCommit rev : logs) {
            commit_names.add(rev);
            String bugStatus = getBugStatus(rev);
            bug.add(bugStatus);
        }

        System.out.println("Commit numbers: " +commit_names.size());

        List<ObjectId> obj_ids = new ArrayList<>();

        for (int k = 0; k < commit_names.size()-1 ; k++) {
            ObjectId treeId1 = commit_names.get(k).getTree().getId();
            ObjectId treeId2 = commit_names.get(k + 1).getTree().getId();
            List<DiffEntry> entries = getDiffEntries(treeId1, treeId2);

            for (DiffEntry entry : entries) {
                //To store file name/Package name for current file in the loop
                ArrayList<String> file_list_for_commit = new ArrayList<>();
                String class_name_of_file =  null;
                int total_files = 0;
                if (entry.getNewPath().endsWith(".java")) {
                    temp_filepath = entry.getNewPath();
                    filepaths.put(repoPath+"/"+temp_filepath, repoPath+"/"+temp_filepath);
                    temp_filepath = temp_filepath.substring(temp_filepath.lastIndexOf("/") + 1);
                    String filepath = repoPath+"/"+temp_filepath;
                    // remove extension of file name
                    temp_filepath = FilenameUtils.removeExtension(temp_filepath);

                    //System.out.println("Filename getNewPath(): " + entry.getNewPath());
                    // If file is not DELETED, get its LOC and package name
                    if (!entry.getChangeType().toString().equals("DELETE")) {
                        total_files++;
                        //System.out.println("File not deleted: " + entry.getNewPath());
                        //System.out.println("File ID: " + entry.getNewId().toObjectId());

                        // objectId stores ID of current file
                        ObjectId objectId = entry.getNewId().toObjectId();

                        // loader object will open the file with given ID(objectId)
                        ObjectLoader loader = repo.open(objectId);


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
                                    class_name_of_file = splitted[1].replaceAll(";", ".").concat(temp_filepath);
                                    file_list_for_commit.add(class_name_of_file);
//                                        file_list_for_commit.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));

                                    //System.out.println(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                    // Stop reading next lines
                                    break;
                                } else {
                                    break;
                                }

                            }
//
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }
                        try {

                            String class_name = file_list_for_commit.get(total_files-1);
                            List<Integer> mertics_value_list = calculateMetrics(filepath, class_name);
                            wmc_list_for_commit.add(mertics_value_list.get(0));
                            cbo_list_for_commit.add(mertics_value_list.get(1));
                            dit_list_for_commit.add(mertics_value_list.get(2));
                            lcom_list_for_commit.add(mertics_value_list.get(3));
                        } catch(Exception ex){
                            // Skip files that are not found on the release directory.
                            System.out.println(" Exception for file occurred.");
                            loc_list_for_commit.remove(total_files-1);
//                                file_list_for_commit.remove(total_files-1);
                            total_files -= 1;
                            continue;
                        }


                    }
                }

                filenames.put(k, file_list_for_commit);
                total_files_in_commit.put(k, total_files);
            }

        }
        System.out.println("All commit messages done.");

        String csvPath = "csvfiles/hadoop_"+version+".csv";

        try (FileWriter csvWriter = new FileWriter(csvPath, false)) {
            // Add column headings to CSV file
            csvWriter.append("class-name");
            csvWriter.append(",");
            csvWriter.append("bug");
            csvWriter.append(",");
            csvWriter.append("loc");
            csvWriter.append(",");
            csvWriter.append("wmc");
            csvWriter.append(",");
            csvWriter.append("dit");
            csvWriter.append(",");
            csvWriter.append("cbo");
            csvWriter.append(",");
            csvWriter.append("rfc");
            csvWriter.append(",");
            csvWriter.append("lcom");
            csvWriter.append("\n");
            csvWriter.flush();
            System.out.println(csvPath);
            System.out.println("Create CSV file at "+csvPath);
        } catch (IOException e) {
            System.err.println("Can't create CSV file. Maybe the required file is open.");
        }


        Set<Integer> keys = filenames.keySet();
//                int size=filenames.size();

        Map<String, String> class_map = new HashMap<String, String>();
        //Iterate on keys to get data for CSV file
        for (int i : keys) {

            boolean value = filenames.get(i).isEmpty();
            if (value) {
                continue;
            }
            List<String> temp_file_names = filenames.get(i);
            int files_per_commit = total_files_in_commit.get(i);
        // Store data into CSV
            for (int j = 0; j < files_per_commit; j++) {
                String name = temp_file_names.get(j);
                String b = bug.get(i);
                String yes = "yes";
                String computed = "false";

                class_map.put(name, b);
                if (b.equals(yes)) {
                    break;
                }

            }
        }

        System.out.println("Number of classes: "+class_map.size());
        System.out.println("Number of file paths: "+filepaths.size());

        for (String key : filepaths.keySet()){
                for (Map.Entry<String, String> entry : class_map.entrySet()) {
                    new CK().calculate(key, result -> {
                    String name = entry.getKey();
                    String isBug = entry.getValue();
                    if (result.getClassName().equals(name)) {
                        StringBuilder row_data = new StringBuilder();
                        try (FileWriter csvWriter = new FileWriter(csvPath, true)) {
                            System.out.println("Class name: " + result.getClassName());
                            System.out.println("Bug: " + isBug);
                            System.out.println("LOC value: " + result.getLoc());
                            System.out.println("WMC value: " + result.getWmc());
                            System.out.println("DIT value: " + result.getDit());
                            System.out.println("CBO value: " + result.getCbo());
                            System.out.println("WMC value: " + result.getRfc());
                            System.out.println("LCOM value: " + result.getLcom());


                            row_data.append(result.getClassName());
                            row_data.append(",");
                            row_data.append(isBug);
                            row_data.append(",");
                            row_data.append(result.getLoc());
                            row_data.append(",");
                            row_data.append(result.getWmc());
                            row_data.append(",");
                            row_data.append(result.getDit());
                            row_data.append(",");
                            row_data.append(result.getCbo());
                            row_data.append(",");
                            row_data.append(result.getRfc());
                            row_data.append(",");
                            row_data.append(result.getLcom());
                            csvWriter.append(row_data.toString());
                            csvWriter.append("\n");
                            row_data.setLength(0);
                            csvWriter.flush();

                        } catch(IOException err){
                            System.err.println("Can't create CSV file. May be the required file is open.");
                        }

                    }
                });
            }
        }


        //use this code to compute all metrics for classes, variables and methods
        //doesn't compute the bug status for each class
        /*ResultWriter writer = new ResultWriter("csvfiles/class.csv", "csvfiles/method.csv", "csvfiles/variable.csv", "csvfiles/field.csv");

        CK ck = new CK();
        ck.calculate(repoPath, result -> {
            try {
                writer.printResult(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/

        //writer.flushAndClose();

    }

    /**
     * Get files that have been modified between two commits.
     */
    private static List<DiffEntry> getDiffEntries(ObjectId treeId1, ObjectId treeId2) throws IOException {
        CanonicalTreeParser treeParser1 = new CanonicalTreeParser();

        try (ObjectReader reader = repo.newObjectReader()) {
            treeParser1.reset(reader, treeId1);
        }
        CanonicalTreeParser treeParser2 = new CanonicalTreeParser();

        try (ObjectReader reader1 = repo.newObjectReader()) {
            treeParser2.reset(reader1, treeId2);
        }
        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream()); // use NullOutputStream.INSTANCE if you don't need the diff output
        df.setRepository(git.getRepository());

        return df.scan(treeParser1, treeParser2);
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

    private static List<Integer> calculateMetrics(String filepath, String class_name) {
        Boolean useJars = true;
        List<Integer> metrics_list = new ArrayList<>();
        new CK().calculate(filepath, result -> {
            if (result.getClassName().equals(class_name)) {
                metrics_list.add(result.getWmc());
                metrics_list.add(result.getCbo());
                metrics_list.add(result.getDit());
                metrics_list.add(result.getLcom());
                System.out.println("Class name: " + result.getClassName());
                System.out.println("DIT value: " + result.getDit());
                System.out.println("CBO value: " + result.getCbo());
                System.out.println("WMC value: " + result.getWmc());
                System.out.println("LCOM value: " + result.getLcom());
            }
        });
        // WMC, CBO, DIT and LCOM metrics
        return metrics_list;
    }
}
