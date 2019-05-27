package com.cnws.util;

import com.cnws.base.model.Sample;

import java.io.*;
import java.util.*;


public class FileUtil {

    public static Sample loadSample(String filePath) {
        BufferedReader mReader = null;
        Sample sample = new Sample();
        String line = "";
        int lineNum = 0;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lineNum += 1;
                    List<String> sentence = new ArrayList<String>();
                    List<String> pos = new ArrayList<String>();
                    for (String word : line.trim().split(" ")) {
                        String[] arr = word.trim().split("_");
                        sentence.add(arr[0].trim());
                        pos.add(arr[1]);
                    }
                    sample.extendSentence(sentence, pos);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info(lineNum + " -> " + line);
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sample;
    }


    public static Sample loadRawSample(String filePath) {
        BufferedReader mReader = null;
        Sample sample = new Sample();
        String line = "";
        int lineNum = 0;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lineNum += 1;
                    List<String> sentence = new ArrayList<String>();
                    List<String> pos = new ArrayList<String>();
                    for (int i = 0; i < line.trim().length(); i++) {
                        String word = line.trim().substring(i, i + 1);
                        if (!word.trim().isEmpty()) {
                            sentence.add(word);
                        }
                        sentence.add(word);
                        pos.add("");
                    }
                    sample.extendSentence(sentence, pos);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info(lineNum + " -> " + line);
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sample;
    }

    public static Map<String, String> readConfig(String filePath) {
        BufferedReader mReader = null;
        Map<String, String> config = new HashMap<String, String>();
        String line = "";
        int lineNum = 0;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    if (line.trim().startsWith("#")){
                        continue;
                    }
                    lineNum += 1;
                    String[] arr = line.split(" ");
                    if (arr.length != 2) {
                        throw new Error("config param must be a pair (key and value)");
                    }
                    config.put(arr[0], arr[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info(lineNum + " -> " + line);
            throw new Error("config param error!");
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return config;
    }

    public static void writeNoun(String filePath, Map<String, Set<String>> noun2pattern) {
        BufferedWriter mWriter = null;
        try {
            mWriter = new BufferedWriter(new FileWriter(filePath));
            for (String noun : noun2pattern.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(noun).append(" ");
                for (String p : noun2pattern.get(noun)) {
                    sb.append(p).append(" ");
                }
                mWriter.write(sb.toString().trim() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("writeNoun error!");
        } finally {
            if (mWriter != null) {
                try {
                    mWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String, Set<String>> readNoun(String filePath) {
        BufferedReader mReader = null;
        Map<String, Set<String>> noun2pattern = new HashMap<String, Set<String>>();
        String line;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] arr = line.trim().split(" ");
                    Set<String> pattern = new HashSet<String>();
                    for (int i = 1; i < arr.length; i++) {
                        pattern.add(arr[i]);
                    }
                    noun2pattern.put(arr[0], pattern);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("readNoun error!");
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return noun2pattern;
    }

    public static void writePattern(String filePath, Set<String> patterns) {
        BufferedWriter mWriter = null;
        try {
            mWriter = new BufferedWriter(new FileWriter(filePath));
            for (String pattern : patterns) {
                mWriter.write(pattern + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("writePattern error!");
        } finally {
            if (mWriter != null) {
                try {
                    mWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Set<String> readPattern(String filePath) {
        BufferedReader mReader = null;
        Set<String> patterns = new HashSet<String>();
        String line;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    patterns.add(line.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("readPattern error!");
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return patterns;
    }

    public static void writePOS(String filePath, Set<String> noun2pos) {
        BufferedWriter mWriter = null;
        try {
            mWriter = new BufferedWriter(new FileWriter(filePath));
            for (String noun : noun2pos) {
                mWriter.write(noun + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("writeNoun error!");
        } finally {
            if (mWriter != null) {
                try {
                    mWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String, String> readPOS(String filePath) {
        BufferedReader mReader = null;
        Map<String, String> noun2pos = new HashMap<String, String>();
        String line;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] arr = line.trim().split(" ");
                    noun2pos.put(arr[0], arr[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("readNoun error!");
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return noun2pos;
    }
}
