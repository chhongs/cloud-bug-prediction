package com.mycompany.dataextractor;

import com.mycompany.model.MatrixData;
import com.opencsv.CSVWriter;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GenerateCSV {

    public static void main(String[] args) throws IOException, GitAPIException {
        String repoPath = args[0];
        List<Integer> versions  = new ArrayList<>();
        IntStream.range(1, 2).forEach(
                versions::add
        );
        CommitsData2 commitsData2 = new CommitsData2();
        List<MatrixData> matrixDataList = commitsData2.getCommits(versions, repoPath);
        createCSV(matrixDataList );
    }

    private static void createCSV(List<MatrixData> unique){
        // Append new data to the CSV file

        File file = new File("csvfiles/matrixData.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);
            List<String[]> data = new ArrayList<>();
            data.add(new String[] {"name-pr","version","name","bug" });
            for (MatrixData matrixData:unique) {
                data.add(new String[] {matrixData.getNamePr(),matrixData.getVersion(),matrixData.getClassName(),  String.valueOf(matrixData.getBug()) });
            }
            // create a List which contains String array
            writer.writeAll(data);

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setLinesofCodes(List<MatrixData> matrixDataList,String release)
    {
        //set LOC
    }

    public void setWMC(List<MatrixData> matrixDataList)
    {
        //set WMC
    }
}


