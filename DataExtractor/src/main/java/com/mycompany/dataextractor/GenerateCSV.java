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

    public static void main(String[] args) throws IOException, GitAPIException {
        String repoPath = args[0];
        repo = new FileRepository(repoPath);
        git = new Git(repo);
        List<Integer> versions = new ArrayList<>();
        IntStream.range(323, 324).forEach(
                versions::add
        );
        CommitsData2 commitsData2 = new CommitsData2();
        List<MatrixData> matrixDataList = commitsData2.getCommits(versions, repo,git);
        MatrixComputation matrixComputation = new MatrixComputation();
        matrixDataList = matrixComputation.computeMatrix(versions,  repo,git, matrixDataList);
        if (matrixDataList == null) {
            System.out.println("Version is not correct");
            return;
        }
        createCSV(matrixDataList);

    }

    private static void createCSV(List<MatrixData> unique) {
        // Append new data to the CSV file

        File file = new File("csvfiles/matrixData.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);
            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"name-pr", "version", "name", "loc", "bug"});
            for (MatrixData matrixData : unique) {
                data.add(new String[]{matrixData.getNamePr(), matrixData.getVersion(), matrixData.getClassName(), matrixData.getLoc(), String.valueOf(matrixData.getBug())});
            }
            // create a List which contains String array
            writer.writeAll(data);
            System.out.println("CSV generation is successful.");
            // closing writer connection
            writer.close();
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


