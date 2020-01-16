package com.mycompany.dataextractor;

import com.github.mauricioaniche.ck.util.LOCCalculator;
import com.google.common.base.Strings;
import com.mycompany.model.MatrixData;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class CommitsData {

    private static Repository repo;
    private static Git git;
    private static String[] bug_keywords;
    private final static String BUG_YES = "yes";
    private final static String BUG_NO = "no";

    //filtering Keywords


    public List<MatrixData> getCommits(List<Integer> versions, Repository repository, Git r,  String[] bugs_keywords) throws IOException, GitAPIException {
        repo = repository;
        git = r;
        bug_keywords = bugs_keywords;
        // version list stores the repository version tags
        List<String> version = new ArrayList<>();
        // commit_names stores all commits in a revision
        List<RevCommit> commit_names = new ArrayList<>();
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
        //System.out.println(tags.size());
        // Currently we need only one release so first_refs will contain one release name tag
        List<Ref> first_refs = new ArrayList<>();
        versions.forEach((i) -> {
            first_refs.add(tags.get(i));
        });
        //System.out.println(first_refs.get(0).getName().substring(10));
        // Loop over the release to get commits
        for (Ref ref : first_refs) {
            version.add(ref.getName().substring(10));
            //System.out.println("Tag: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            Iterable<RevCommit> logs = getRevCommits(ref);
            int mycount = 0;
            for (RevCommit rev : logs) {
                mycount++; //temporary break
                //System.out.println("Commit No.: " + mycount);
                //System.out.println("Commit message.: " + rev.getFullMessage());
                commit_names.add(rev);
                //System.out.println("Commit: " + rev.getName());
                String bugStatus = getBugStatus(rev);
                bug.add(bugStatus);
            }

            //System.out.println("Total Commits in this release is: " + commit_names.size());

            extractData(commit_names, filenames, total_files_in_commit);
            //System.out.println("total files in commit " + total_files_in_commit.size());
        }
        //System.out.println("Total Commits in this release is: " + commit_names.size());
        //System.out.println("Exit from loops. Creating CSV.");
        List<MatrixData> matrixDataList = setBasicData(filenames, total_files_in_commit, project_name, version, bug);
        return matrixDataList;
    }

    public void extractData(List<RevCommit> commit_names, Map<Integer, List<String>> filenames, Map<Integer, Integer> total_files_in_commit) throws IOException {
        // Added a list to store file names(used to avoid duplicate filenames)
        ArrayList<String> AllFileNames = new ArrayList<>();
        String temp_filepath;
        //System.out.println(" Total commits are: "+commit_names.size());
        for (int k = 0; k < commit_names.size() - 1; k++) {
            ObjectId treeId1 = commit_names.get(k).getTree().getId();
            ObjectId treeId2 = commit_names.get(k + 1).getTree().getId();
            List<DiffEntry> entries = getDiffEntries(treeId1, treeId2);

            //System.out.println("Entry count: " + entries.size());
            //System.out.println("Entry size: " + entries.size());

            // Iterate all changed(modified, deleted etc) files between these commits
            for (DiffEntry entry : entries) {
                //To store file name/Package name for current file in the loop
                ArrayList<String> file_list_for_commit = new ArrayList<>();
                //Counter for total files in commit
                int total_files = 0;
                //System.out.println(entry);
                //Find Java files
                if (entry.getNewPath().endsWith(".java")) {
                    temp_filepath = entry.getNewPath();
                    //System.out.println(temp_filepath);
                    temp_filepath = temp_filepath.substring(temp_filepath.lastIndexOf("/") + 1);
                    // remove extension of file name
                    temp_filepath = FilenameUtils.removeExtension(temp_filepath);
//                  System.out.println("File class name: "+temp_filepath);
                    //System.out.println("Filename getNewPath(): " + entry.getNewPath());
                    // If file is not DELETED, get its LOC and package name
                    if (!entry.getChangeType().toString().equals("DELETE")) {
                        total_files++;
//                      System.out.println("file count: "+total_files_in_commit);
                        //System.out.println("File not deleted: " + entry.getNewPath());
                        //System.out.println("File not deleted: " + entry.getNewId().toObjectId());
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

                                    //Check if filename already exists
                                    Boolean fileNameExists = AllFileNames.contains(splitted[1].replaceAll(";", ".").concat(temp_filepath));

                                    // Add file names only once/ Ignore duplicate entries
                                    if (!fileNameExists){           // If file name does not exists in our file list (AllFileNames)
                                        // Append package name to add file name
                                        file_list_for_commit.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                        AllFileNames.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                        //System.out.println(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                    }

                                    // Code to replace duplicate file with previous one(For testing only)
//                                    else{
//                                        int DuplicateFileIndex = file_list_for_commit.indexOf(splitted[1].replaceAll(";", ".").concat(temp_filepath));
//                                        System.out.println(" Index: "+DuplicateFileIndex);
//                                        file_list_for_commit.add(DuplicateFileIndex,splitted[1].replaceAll(";", ".").concat(temp_filepath));
//                                    }

//                                    file_list_for_commit.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));
//                                    duplicateFiles.add(splitted[1].replaceAll(";", ".").concat(temp_filepath));
//                                    System.out.println(splitted[1].replaceAll(";", ".").concat(temp_filepath));
                                    // Stop reading next lines
                                    break;
                                } else {
                                    break;
                                }
                            }
                            // Close file reader object
                            reader.close();
                            //System.out.println("Reader closed");
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }
                    } else {
                        continue;
                    }
                }

                //Add file_list_for_commit list to HashMap where key is 'k'
                filenames.put(k, file_list_for_commit);
                //Add total_files for a commit to HashMap where key is 'k'
                total_files_in_commit.put(k, total_files);
            } //diff entry loop between two commits
        }
    }

    public List<MatrixData> setBasicData(Map<Integer, List<String>> filenames, Map<Integer, Integer> total_files_in_commit, String proj_name, List<String> version, ArrayList<String> bug) {
        List<MatrixData> matrixDataList = new ArrayList<>();
        Set<Integer> keys = filenames.keySet();
        //System.out.println("file names size " + keys.size());
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
            //Get files_per_commit from hashmap 'total_files_in_commit'
            int files_per_commit = total_files_in_commit.get(i);
            //System.out.println("total files in commit " + files_per_commit);
            // Store data into CSV
            for (int j = 0; j < files_per_commit; j++) {
                //System.out.println("j value: " + j);
                MatrixData matrixData = new MatrixData();
                matrixData.setNamePr(proj_name);
                matrixData.setVersion(version.get(0));
                // 'name' stores name of file(java package) for current commit and current file in this commit
                matrixData.setClassName(temp_file_names.get(j));
                //System.out.println(temp_file_names.get(j));
                // 'b' stores bug for current commit
                String b = bug.get(i);
                if (b.equals(BUG_YES)) matrixData.setBug(Boolean.TRUE);
                else matrixData.setBug(Boolean.FALSE);
                matrixDataList.add(matrixData);
            }
        }
        return matrixDataList;
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

    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        System.out.println("input string "+inputStr);
        boolean check = Arrays.stream(items).parallel().anyMatch(inputStr::contains);
        if(check)
            System.out.println("check "+check);
        return check;
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
            //if (commitmessage.contains(ERROR) || commitmessage.contains(FIX) || commitmessage.contains(BUG) || commitmessage.contains(FAILURE) || commitmessage.contains(CRASH) || commitmessage.contains(WRONG) || commitmessage.contains(UNEXPECTED)) {
//                    System.out.println("Commit message: " + rev.getFullMessage());
            if(stringContainsItemFromList(commitmessage, bug_keywords))
            {
                return BUG_YES;
            } else {
                return BUG_NO;
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
            //System.out.println("Peeled");
            log.add(peeledRef.getPeeledObjectId());
//                log.addRange(peeledRef.getPeeledObjectId(), peeledRef.getPeeledObjectId());
        } else {
            log.add(ref.getObjectId());
//                log.addRange(ref.getObjectId(), ref.getObjectId());
            //System.out.println(" not Peeled: " + ref.getObjectId());
        }

        // RevCommit object will contain all the commits for the release
        return log.call();
    }

}
