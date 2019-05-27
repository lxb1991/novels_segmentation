package com.cnws.classifier;

import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.util.FileUtil;
import com.cnws.util.LogUtil;
import com.cnws.util.PythonUtil;

import java.io.*;
import java.util.*;

public class DoublePropagation {

    // the obj will write to file
    private Map<String, Set<String>> mNovelsNoun2Pattern = new HashMap<String, Set<String>>();
    private Set<String> mNovelsPatterns = new HashSet<String>();
    private Set<String> mNovelsNoun2POS = new HashSet<String>();

    private Set<String> mNouns = new HashSet<String>();
    private Map<String, Map<String, Integer>> mNoun2Pattern = new HashMap<String, Map<String, Integer>>();
    private Map<String, Integer> mWord2Freq = new HashMap<String, Integer>();

    /**
     * Double propagation
     */
    public void train(Sample trainSample, Sample novelSample, Sample testSample, String sub, String featurePath,
                      String npPath, String pyEnv, String scriptPath) {
        // add nouns and pattern
        Set<String> newNouns = new HashSet<String>();
        loadNewsNounAndPattern(trainSample, newNouns);

        Set<String> novelsAllWords = new HashSet<String>();
        loadNovelNouns(novelSample, novelsAllWords);
        LogUtil.info("news noun pattern :" + mNovelsPatterns.size() + " all word size : " + novelsAllWords.size() +
                " newNouns freq: " + newNouns.size());
        for (String noun : novelsAllWords) {
            if (!newNouns.contains(noun) && !noun.trim().isEmpty()) {
                mNouns.add(noun.trim());
            }
        }
        LogUtil.info("novel noun size: " + mNouns.size());

        int allNounSize = 0;
        for (String noun : mWord2Freq.keySet()) {
            allNounSize += mWord2Freq.get(noun);
        }
        // build feature
        statKnowledge(novelSample);
        WcFeatureBuilder featureBuilder = new WcFeatureBuilder(mNouns);
        featureBuilder.buildFeature(featurePath + sub + "_feature.txt", allNounSize, mNoun2Punctuation,
                mNovelsPatterns, mNoun2Pattern, null, mSubWord2Freq, mPunctuation);

        // build pos feature
        initPOSData(trainSample);
        mPOSNouns.clear();
        POSFeatureBuilder posFeatureBuilder = new POSFeatureBuilder(mPOSNouns);
        mPOSNoun2Feature.clear();

        boolean loop_flag = true;
        while (loop_flag) {

            String command = pyEnv + "python3 " + scriptPath + "linear.py inf_wc";
            PythonUtil.runPython(command);
            LogUtil.info(command);

            Set<String> wcNouns = loadPythonResult(scriptPath + "temp");

            if (wcNouns != null && wcNouns.size() > 0) {
                LogUtil.info("extract word size : " + wcNouns.size() + " from : " + mNouns.size());
                for (String noun : wcNouns) {
                    if (!mNouns.contains(noun)){
                        loop_flag = false;
                    }else{
                        loop_flag = true;
                    }
                    try{
                        mNovelsPatterns.addAll(mNoun2Pattern.get(noun).keySet());
                        mNovelsNoun2Pattern.put(noun, mNoun2Pattern.get(noun).keySet());
                        // pos classifier
                        mPOSNouns.put(noun, 0);
                    }catch (Exception e){
                    }
                }
                mNouns.removeAll(wcNouns);
                featureBuilder.exchangeSample(mNouns);
                featureBuilder.buildFeature(featurePath + sub + "_feature.txt", allNounSize, mNoun2Punctuation,
                        mNovelsPatterns, mNoun2Pattern, null, mSubWord2Freq, mPunctuation);

                statPOSKnowledge(novelSample);
                posFeatureBuilder.buildFeature(featurePath + sub +"_pc_feature.txt", mPOSNoun2Feature, mVocab,
                        mCharVocab, mPOSVocab);

                String pos_command = pyEnv + "python3 " + scriptPath + "linear.py inf_pc";
                PythonUtil.runPython(pos_command);

                Set<String> pcNounsResult = loadPythonResult(scriptPath + "temp_pc");
                mNovelsNoun2POS.addAll(pcNounsResult);
                mPOSNouns.clear();
                mPOSNoun2Feature.clear();
            } else {
                loop_flag = false;
            }
        }

        mNovelsPatterns.clear();
        FileUtil.writeNoun(npPath + sub + "_noun.txt", mNovelsNoun2Pattern);
        for (String noun : mNovelsNoun2Pattern.keySet()) {
            mNovelsPatterns.addAll(mNovelsNoun2Pattern.get(noun));
        }
        FileUtil.writePattern(npPath + sub + "_pattern.txt", mNovelsPatterns);
        LogUtil.info("save noun and pattern : " + mNovelsNoun2Pattern.size() + " pattern : " + mNovelsPatterns.size());

        FileUtil.writePOS(npPath + sub + "_pos.txt", mNovelsNoun2POS);
        LogUtil.info("noun2pos : " + mNovelsNoun2POS.size());

    }

    private static HashSet<String> loadPythonResult(String filePath) {
        HashSet<String> result = new HashSet<String>();
        BufferedReader mReader = null;
        String line;
        try {
            mReader = new BufferedReader(new FileReader(filePath));
            while ((line = mReader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    result.add(line.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info("temp.txt error!");
        } finally {
            if (mReader != null) {
                try {
                    mReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private void loadNovelNouns(Sample sample, Set<String> novelsAllWords) {

        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);
                novelsAllWords.add(word);
                if (mWord2Freq.containsKey(word)) {
                    mWord2Freq.put(word, mWord2Freq.get(word) + 1);
                } else {
                    mWord2Freq.put(word, 1);
                }
            }
        }
    }

    /**
     * collect wc data
     */

    private void loadNewsNounAndPattern(Sample sample, Set<String> nouns) {

        Set<String> filterNouns = new HashSet<String>();
        Map<String, Integer> nrWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> nsWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> ntWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> nWord2Freq = new HashMap<String, Integer>();
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);
                String pos = sentence.getPOSByIdx(wIdx);

                nouns.add(word);

                Map<String, Integer> target = null;
                if ("nr".equals(pos)) {
                    target = nrWord2Freq;
                } else if ("ns".equals(pos)) {
                    target = nsWord2Freq;
                } else if ("nt".equals(pos)) {
                    target = ntWord2Freq;
                } else if ("nz".equals(pos) || "n".equals(pos) || "nx".equals(pos)) {
                    target = nWord2Freq;
                }
                if (target != null) {
                    if (target.containsKey(word)) {
                        target.put(word, target.get(word) + 1);
                    } else {
                        target.put(word, 1);
                    }
                }
            }
        }

        filterNouns.addAll(sortMapByValues(nrWord2Freq));
        filterNouns.addAll(sortMapByValues(nsWord2Freq));
        filterNouns.addAll(sortMapByValues(ntWord2Freq));
        filterNouns.addAll(sortMapByValues(nWord2Freq));

        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);
                if (filterNouns.contains(word)) {
                    String pWord = "#";
                    if (wIdx > 0) {
                        pWord = sentence.getWordByIdx(wIdx - 1);
                    }
                    String aWord = "#";
                    if (wIdx + 1 < sentence.size()) {
                        aWord = sentence.getWordByIdx(wIdx + 1);
                    }
                    String pattern = pWord + "_" + aWord;
                    mNovelsPatterns.add(pattern);
                }
            }
        }
    }

    private static ArrayList<String> sortMapByValues(Map<String, Integer> aMap) {
        Set<Map.Entry<String, Integer>> mapEntries = aMap.entrySet();
        List<Map.Entry<String, Integer>> aList = new LinkedList<Map.Entry<String, Integer>>(mapEntries);
        Collections.sort(aList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> ele1, Map.Entry<String, Integer> ele2) {
                return ele2.getValue().compareTo(ele1.getValue());
            }
        });
        ArrayList<String> nouns = new ArrayList<String>();
        int maxLen = (int) (aMap.size() * 0.3);
        for (Map.Entry<String, Integer> entry : aList) {
            if (nouns.size() < maxLen) {
                nouns.add(entry.getKey());
            }
        }
        return nouns;
    }

    private Set<String> mSubWordCache = new HashSet<String>();
    private Map<String, Double> mSubWord2Freq = new HashMap<String, Double>();
    private Set<String> mNoun2Punctuation = new HashSet<String>();

    private static final String[] PUNCTUATION_DICT = {"∶", "（", "？", "××", "」", "〉", "⑤", "⑦",
            "…………", "⑵", "③", "“", "┅", "～", "％", "［", "＊", "④", "『", "———", "〈", "②", "》",
            "；", "，", "＞", "］", "──", "——", "∶", "’", "－", "‘", "……", "「", "●", "△", "⑥", "?",
            "”", "±％", "①", "·", "、", "」", "▲", "《", "＝", "』", "※", "：", "—", "／", "×",
            "〓", "！", "。", "‰", "）"};

    private Set<String> mPunctuation = new HashSet<String>(Arrays.asList(PUNCTUATION_DICT));

    /**
     * compute PMI by mNouns, and build punctuation feature, and fill noun2pattern
     */
    private void statKnowledge(Sample sample) {
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);

                if (mNouns.contains(word)) {
                    // compute PMI
                    buildSubWordFreq(word.length() > 1 ? word.substring(0, 1) : null);
                    buildSubWordFreq(word.length() > 1 ? word.substring(1) : null);
                    buildSubWordFreq(word.length() > 2 ? word.substring(0, 2) : null);
                    buildSubWordFreq(word.length() > 2 ? word.substring(2) : null);
                    buildSubWordFreq(word.length() > 1 ? word.substring(0, word.length() - 1) : null);
                    buildSubWordFreq(word.length() > 1 ? word.substring(word.length() - 1) : null);
                    buildSubWordFreq(word.length() > 2 ? word.substring(0, word.length() - 2) : null);
                    buildSubWordFreq(word.length() > 2 ? word.substring(word.length() - 2) : null);
                    buildSubWordFreq(word);

                    // compute pattern and punctuation feature
                    String pWord = "#";
                    if (wIdx > 0) {
                        pWord = sentence.getWordByIdx(wIdx - 1);
                    }
                    String aWord = "#";
                    if (wIdx + 1 < sentence.size()) {
                        aWord = sentence.getWordByIdx(wIdx + 1);
                    }
                    if (mPunctuation.contains(aWord) && mPunctuation.contains(pWord)) {
                        mNoun2Punctuation.add(word);
                    }

                    // just for build feature to get pattern
                    String pattern = pWord + "_" + aWord;
                    if (mNoun2Pattern.containsKey(word)) {
                        if (mNoun2Pattern.get(word).containsKey(pattern)) {
                            mNoun2Pattern.get(word).put(pattern, mNoun2Pattern.get(word).get(pattern) + 1);
                        } else {
                            mNoun2Pattern.get(word).put(pattern, 1);
                        }
                    } else {
                        HashMap<String, Integer> patterns = new HashMap<String, Integer>();
                        patterns.put(pattern, 1);
                        mNoun2Pattern.put(word, patterns);
                    }
                }
            }
        }
        LogUtil.info(" stat know mNoun: " + mNouns.size() + " mNoun2Freq size:" + mWord2Freq.size() +
                " mSubWordCache size :" + mSubWordCache.size());

        for (String sub : mSubWordCache) {
            if (mWord2Freq.containsKey(sub)) {
                int count = mWord2Freq.get(sub);
                if (mSubWord2Freq.containsKey(sub)) {
                    mSubWord2Freq.put(sub, mSubWord2Freq.get(sub) + (double) count);
                } else {
                    mSubWord2Freq.put(sub, (double) count);
                }
            }
            if (!mSubWord2Freq.containsKey(sub)) {
                mSubWord2Freq.put(sub, 1.0);
            }
        }
    }

    private void buildSubWordFreq(String subWord) {
        if (subWord != null) {
            mSubWordCache.add(subWord);
        }
    }

    /**
     * collect POS data
     */
    private Map<String, Integer> mTag2Id = new HashMap<String, Integer>();
    private Map<String, Integer> mPOSNouns = new HashMap<String, Integer>();
    private Map<String, List<String>> mPOSNoun2Feature = new HashMap<String, List<String>>();

    private Map<String, Integer> mVocab = new HashMap<String, Integer>();
    private Map<String, Integer> mCharVocab = new HashMap<String, Integer>();
    private Map<String, Integer> mPOSVocab = new HashMap<String, Integer>();

    public void initPOSData(Sample sample) {

        mTag2Id.put("nr", 0);
        mTag2Id.put("ns", 1);
        mTag2Id.put("nt", 2);
        mTag2Id.put("n", 3);
        // load noun
        loadPOSNounData(sample);
        statPOSKnowledge(sample);
    }

    /**
     * load noun and pattern
     */
    private void loadPOSNounData(Sample sample) {
        Map<String, Integer> nrWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> nsWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> ntWord2Freq = new HashMap<String, Integer>();
        Map<String, Integer> nWord2Freq = new HashMap<String, Integer>();
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);
                String pos = sentence.getPOSByIdx(wIdx);

                Map<String, Integer> target = null;
                if ("nr".equals(pos)) {
                    target = nrWord2Freq;
                } else if ("ns".equals(pos)) {
                    target = nsWord2Freq;
                } else if ("nt".equals(pos)) {
                    target = ntWord2Freq;
                } else if ("nz".equals(pos) || "n".equals(pos) || "nx".equals(pos)) {
                    target = nWord2Freq;
                }
                if (target != null) {
                    if (target.containsKey(word)) {
                        target.put(word, target.get(word) + 1);
                    } else {
                        target.put(word, 1);
                    }
                }
            }
        }


        addSample(sortMapByValues(nrWord2Freq), "nr");
        addSample(sortMapByValues(nsWord2Freq), "ns");
        addSample(sortMapByValues(ntWord2Freq), "nt");
        addSample(sortMapByValues(nWord2Freq), "n");
    }
    private void addSample(List<String> nouns, String tag){
        for (String noun : nouns){
            mPOSNouns.put(noun, mTag2Id.get(tag));
        }
    }

    private void statPOSKnowledge(Sample sample) {
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);

                if (mPOSNouns.containsKey(word)) {
                    String ppWord = "#";
                    if (wIdx > 1) {
                        ppWord = sentence.getWordByIdx(wIdx - 2);
                        add2vocab(ppWord, mVocab);
                    }
                    String pWord = "#";
                    String pPOS = "#";
                    if (wIdx > 0) {
                        pWord = sentence.getWordByIdx(wIdx - 1);
                        pPOS = sentence.getPOSByIdx(wIdx - 1);
                        add2vocab(pWord, mVocab);
                        add2vocab(pPOS, mPOSVocab);
                    }
                    String aWord = "#";
                    String aPOS = "#";
                    if (wIdx + 1 < sentence.size()) {
                        aWord = sentence.getWordByIdx(wIdx + 1);
                        aPOS = sentence.getPOSByIdx(wIdx + 1);
                        add2vocab(aWord, mVocab);
                        add2vocab(aPOS, mPOSVocab);
                    }
                    String aaWord = "#";
                    String aaPOS = "#";
                    if (wIdx + 2 < sentence.size()) {
                        aaWord = sentence.getWordByIdx(wIdx + 2);
                        aaPOS = sentence.getPOSByIdx(wIdx + 2);
                        add2vocab(aaWord, mVocab);
                        add2vocab(aaPOS, mPOSVocab);
                    }
                    String aaaWord = "#";
                    if (wIdx + 3 < sentence.size()) {
                        aaaWord = sentence.getWordByIdx(wIdx + 3);
                        add2vocab(aaaWord, mVocab);
                    }

                    List<String> posFeature = new ArrayList<String>();
                    posFeature.add(ppWord);
                    posFeature.add(pWord);
                    posFeature.add(aWord);
                    posFeature.add(aaWord);
                    posFeature.add(pPOS);
                    posFeature.add(aPOS);
                    posFeature.add(word.substring(0, 1));
                    posFeature.add(word.substring(word.length()-1));
                    add2vocab(word.substring(0, 1), mCharVocab);
                    add2vocab(word.substring(word.length()-1), mCharVocab);
                    mPOSNoun2Feature.put(word, posFeature);
                }
            }
        }
        LogUtil.info(" stat know: " + mNouns.size() + " : " + mCharVocab.size() + " : " + mPOSVocab.size());
    }
    private void add2vocab(String token, Map<String, Integer> vocab){
        if (!vocab.containsKey(token)){
            vocab.put(token, vocab.size());
        }
    }

}
