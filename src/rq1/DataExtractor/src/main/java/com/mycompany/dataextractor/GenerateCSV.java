package com.mycompany.dataextractor;

import com.mycompany.model.MatrixData;
import com.opencsv.CSVWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GenerateCSV {

    private static Repository repo;
    private static Git git;

    private static String[] all_types_bugs = {"error", "fix","bug","failure","crash","wrong","unexpected"};

    private static String[] general_bugs = {"logic","error handling","optimization","configuration","data race conditions","hang","space","load"};
    private static String[] cloud_specific_bugs = {"distributed concurrency","performance","single-point-of-failure"};
    private static String[] cloud_concurrency_bugs = {"blocked","locked","race","dead-lock","deadlock","starvation","suspension","order violation","atomicity violation","single variable atomicity violation","multi variable atomicity violation","livelock", "live-lock"};
    private static String[] optimization_bugs = {"optimization","optimize"};
    private static String[] logical_bugs = {"logic","logical","programming logic","wrong logic"};
    private static String[] performance_bugs = {"performance","load balancing","cloud bursting","performance implications"};
    private static String[] configuration_bugs = {"configuration"};
    private static String[] error_handling_bugs = {"error handling", "exception", "exceptions"};
    private static String[] hang_bugs = {"hang","freeze","unresponsive","blocking","deadlock","infinite loop","user operation error"};


    public static void main(String[] args) throws IOException, GitAPIException {
        String repoPath = args[0];

//        String repoPath = "C:/HadoopProject/hadoop/.git";
        repo = new FileRepository(repoPath);
        git = new Git(repo);
        List<MatrixData> MatrixListVersions = new ArrayList<>();
        for(int i=0;i<=10;i++)
        {
            List<Integer> versions = new ArrayList<>();
            // Test for version 323(submarine-0.2.0-RC0)
            IntStream.range(250-i, 251-i).forEach(
                    versions::add
            );
            CommitsData commitsData2 = new CommitsData();
            // Get file_names_for_commit and total_files_in_commit for all commits in this version
            List<MatrixData> matrixDataList = commitsData2.getCommits(versions, repo, git, all_types_bugs);
            MatrixComputation matrixComputation = new MatrixComputation();
            //Compute metrics for the files(for each commit in this release) we get in matrixDataList
            matrixDataList = matrixComputation.computeMatrix(versions, repo, git, matrixDataList, repoPath);
            System.out.println("versions complted"+i);
            if (matrixDataList != null) {
                MatrixListVersions.addAll(matrixDataList);
            }
        }
        if (MatrixListVersions == null) {
            System.out.println("Version is not correct");
            return;
        }
        System.out.println("size"+MatrixListVersions.size());
        createCSV(MatrixListVersions);

    }

    private static void createCSV(List<MatrixData> unique) {
        // Append new data to the CSV file

        File file = new File("csvfiles/matrixData_cloud_specific_bugs.csv");
        File fileNoLabels = new File("csvfiles/matrixDataNoLabels_all_types_bugs_10.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);
            FileWriter outputfileNo = new FileWriter(fileNoLabels);
            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);
            CSVWriter writerNo = new CSVWriter(outputfileNo);
            List<String[]> data = new ArrayList<>();
            List<String[]> dataNo = new ArrayList<>();
            data.add(new String[]{"name-pr", "version", "name", "loc", "wmc", "dit", "cbo", "rfc", "lcom", "bug"});
            dataNo.add(new String[]{"loc", "wmc", "dit", "cbo", "rfc", "lcom", "bug"});

//            for (MatrixData matrixData : unique) {
//                data.add(new String[]{matrixData.getNamePr(), matrixData.getVersion(), matrixData.getClassName(),
//                        matrixData.getLoc(), matrixData.getWmc(), matrixData.getDit(), matrixData.getCbo(),
//                        matrixData.getRfc(), matrixData.getLcom(),String.valueOf(matrixData.getBug())});
//            }
            for (MatrixData matrixData : unique) {
                // Ignore the files not found
                if (matrixData.getLoc() != null && matrixData.getLoc() !=""){
                    data.add(new String[]{matrixData.getNamePr(), matrixData.getVersion(), matrixData.getClassName(),
                            matrixData.getLoc(), matrixData.getWmc(), matrixData.getDit(), matrixData.getCbo(),
                            matrixData.getRfc(), matrixData.getLcom(), String.valueOf(matrixData.getBug())});
                    dataNo.add(new String[]{matrixData.getLoc(), matrixData.getWmc(), matrixData.getDit(), matrixData.getCbo(),
                            matrixData.getRfc(), matrixData.getLcom(), String.valueOf(matrixData.getBug())});

                }
            }
            // create a List which contains String array
            writer.writeAll(data);
            writerNo.writeAll(dataNo);
            System.out.println("CSV generation is successful.");
            // closing writer connection
            writer.close();
            writerNo.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setLinesofCodes(List<MatrixData> matrixDataList, String release) {
        //set LOC
    }

    public void setWMC(List<MatrixData> matrixDataList) {
        //set WMC
    }
}


