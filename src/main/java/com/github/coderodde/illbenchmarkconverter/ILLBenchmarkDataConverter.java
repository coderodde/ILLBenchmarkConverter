package com.github.coderodde.illbenchmarkconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ILLBenchmarkDataConverter {
    
    private static final String DATA_FILE_NAME_PLACEHOLDER = 
            "DATA_FILE_NAME_PLACEHOLDER";

    private static final String OUTPUT_FILE_NAME_PLACEHOLDER = 
            "OUTPUT_FILE_NAME_PLACEHOLDER";
    
    private static final String PLOT_TITLE = "PLOT_TITLE";
    
    private static final class FolderNames {
        private static final String DATA     = "data";
        private static final String GNUPLOTS = "gnuplots";
        private static final String PNGS     = "pngs";
    }
    
    private static final String[] METHOD_NAMES = {
        "AddAtBeginning",
        "AddAtEnd",
        "AddRandom",
        "AppendCollection",
        "GetRandom",
        "InsertCollection",
        "Iterate",
        "IterateAndModify",
        "PrependCollection",
        "RemoveFromBeginning",
        "RemoveFromEnd",
        "RemoveRandom",
        "RemoveRange",
    };
    
    private static final String[] PLOT_TITLES = {
        "Add at beginning",
        "Add at end",
        "Add at random spot",
        "Append collection",
        "Get random element",
        "Insert collection",
        "Iterate",
        "Iterate and modify",
        "Prepend collection",
        "Remove from beginning",
        "Remove from end",
        "Remove random element",
        "Remove range",
    };
    
    private static final Map<String, String> METHOD_NAME_TO_TITLE = 
            new HashMap<>(METHOD_NAMES.length);
    
    static {
        for (int i = 0; i < METHOD_NAMES.length; i++) {
            METHOD_NAME_TO_TITLE.put(METHOD_NAMES[i], PLOT_TITLES[i]);
        }
    }
    
    private static final String[] LIST_TYPE_NAMES = {
        "arrayList",
        "indexedLinkedList",
        "linkedList",
        "treeList",
    };
    
    private static final int[] LIST_SIZES = {
        100_000,
        200_000,
        300_000,
        400_000,
        500_000,
        600_000,
        700_000,
        800_000,
        900_000,
        1_000_000,
    };
    
    private static final String[] LIST_SIZE_STRINGS = {
        "100000 ",
        "200000 ",
        "300000 ",
        "400000 ",
        "500000 ",
        "600000 ",
        "700000 ",
        "800000 ",
        "900000 ",
        "1000000",
    };
    
    public static void main(String[] args) throws IOException, 
                                                    InterruptedException {
        if (args.length != 3) {
            return;
        }
        
        String benchmarkDataFilePath = args[0];
        String gnuplotScriptFilePath = args[1];
        String resultFilesOutputDir = args[2];
        
        List<String> benchmarkLines = getBenchmarkLines(benchmarkDataFilePath);
        String gnuplotScriptFileTemplate = 
                getGnuplotScriptFileTemplate(gnuplotScriptFilePath);
        
        File resultFilesOutputDirectoryFile = 
                new File(resultFilesOutputDir);
        
        File dataFolder = new File(
                resultFilesOutputDir 
                        + File.separator 
                        + FolderNames.DATA);
        
        File gnuplotsFolder = new File(
                resultFilesOutputDir
                        + File.separator 
                        + FolderNames.GNUPLOTS);
        
        File pngsFolder = new File(
                resultFilesOutputDir
                        + File.separator
                        + FolderNames.PNGS);
        
        createDir(resultFilesOutputDirectoryFile);
        createDir(dataFolder);
        createDir(gnuplotsFolder);
        createDir(pngsFolder);
        
        generateFiles(benchmarkLines, 
                      gnuplotScriptFileTemplate, 
                      dataFolder,
                      gnuplotsFolder,
                      pngsFolder);
    }
    
    private static void createDir(File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        } else if (dir.isFile()) {
            throw new IllegalArgumentException(
                    dir.getName() + " exists but is a regular file.");
        }
    }
    
    private static List<String> 
        getBenchmarkLines(String benchmarkDataFilePath) throws IOException {
        return Files.readAllLines(new File(benchmarkDataFilePath).toPath());
    }
        
    private static String 
        getGnuplotScriptFileTemplate(String gnuplotScriptFilePath) 
                throws IOException {
        return Files.readString(new File(gnuplotScriptFilePath).toPath());
    }
        
    private static void generateFiles(List<String> benchmarkLines, 
                                      String gnuplotScriptFileTemplate,
                                      File dataFolder,
                                      File gnuplotsFolder,
                                      File pngsFolder)
            throws IOException, InterruptedException {
        
        for (String methodName : METHOD_NAMES) {
            generateSingleFile(methodName, 
                               benchmarkLines, 
                               gnuplotScriptFileTemplate,
                               dataFolder,
                               gnuplotsFolder,
                               pngsFolder);
        }
    }
    
    private static void 
        generateSingleFile(String methodName,
                           List<String> benchmarkLines, 
                           String gnuplotScriptFileTemplate,
                           File dataFolder,
                           File gnuplotsFolder,
                           File pngsFolder) 
                throws IOException, InterruptedException {
            
        List<String> entries = 
                benchmarkLines.stream()
                              .filter((String e) -> { 
                                  return e.contains(methodName);
                              }).toList();
        
        // Maps the list type name to the list of benchmark results on different
        // loads:
        Map<String, List<Integer>> map = new TreeMap<>();
        
        for (String listTypeName : LIST_TYPE_NAMES) {
            map.put(listTypeName, new ArrayList<>());
        }
        
        for (String entry : entries) {
            int splitIndex = entry.indexOf(methodName);
            String listTypeName = entry.substring(0, splitIndex);
            String effortString = entry.substring(2 + entry.indexOf(": "));
            int effort = Integer.parseInt(effortString);
            map.get(listTypeName).add(effort);
        }
        
        generateDataFile(map, 
                         methodName + ".dat", 
                         dataFolder);
        
        generateGnuplotScriptFile(gnuplotScriptFileTemplate, 
                                  methodName + ".plt",
                                  methodName + ".dat",
                                  methodName + ".png",
                                  METHOD_NAME_TO_TITLE.get(methodName),
                                  gnuplotsFolder);

        runGnuplotScripts(methodName + ".plt", gnuplotsFolder);
    }
        
    private static void generateDataFile(Map<String, List<Integer>> map, 
                                         String fileName, 
                                         File resultFilesOutputDirectoryFile) 
            throws IOException {
        
        StringBuilder sb = new StringBuilder();
        boolean firstIteration = true;
        
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            if (firstIteration) {
                firstIteration = false;
            } else {
                sb.append("\n\n\n");
            }
            
            load(sb, entry.getValue());
        }
        
        File dataFile = new File(resultFilesOutputDirectoryFile, fileName);
        Files.write(dataFile.toPath(), sb.toString().getBytes());
    }
    
    private static void load(StringBuilder sb, List<Integer> benchmarkData) {
        for (int i = 0; i < LIST_SIZES.length; i++) {
            sb.append(LIST_SIZE_STRINGS[i])
              .append(" ")
              .append(benchmarkData.get(i));
               
            if (i < LIST_SIZES.length - 1) {
                sb.append("\n");
            }
        }
    }
    
    private static void 
        generateGnuplotScriptFile(String gnuplotScriptFileTemplate, 
                                  String gnuplotScriptFileName,
                                  String dataFileName, 
                                  String plotFileName,
                                  String plotTitle,
                                  File resultFilesOutputDirectoryFile)
                throws IOException {
            
        String fileString = 
            gnuplotScriptFileTemplate
                .replace(DATA_FILE_NAME_PLACEHOLDER, 
                         "../" + FolderNames.DATA + "/" + dataFileName)
                .replace(OUTPUT_FILE_NAME_PLACEHOLDER,
                         "../" + FolderNames.PNGS + "/" + plotFileName)
                .replace(PLOT_TITLE, plotTitle);
        
        File plotFile =
                new File(resultFilesOutputDirectoryFile.getAbsolutePath(), 
                         gnuplotScriptFileName);
        
        Files.write(plotFile.toPath(), fileString.getBytes());
    }
        
    private static void runGnuplotScripts(String plotFileName, 
                                          File resultFilesOutputDirectoryFile)
            throws IOException, InterruptedException {
        String[] commands = { "gnuplot", plotFileName };
        Process process = Runtime.getRuntime()
               .exec(commands, 
                     null, 
                     resultFilesOutputDirectoryFile);
        
        process.waitFor();
        
        System.out.println(process);
    }
}
