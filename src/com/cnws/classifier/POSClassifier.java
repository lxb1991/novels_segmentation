package com.cnws.classifier;

import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.util.LogUtil;
import com.cnws.util.PythonUtil;

import java.util.*;

public class POSClassifier {


    private Map<String, Integer> mTag2Id = new HashMap<String, Integer>();
    private Map<String, Integer> mNouns = new HashMap<String, Integer>();
    private Map<String, List<String>> mNoun2Feature = new HashMap<String, List<String>>();

    private Map<String, Integer> mVocab = new HashMap<String, Integer>();
    private Map<String, Integer> mCharVocab = new HashMap<String, Integer>();
    private Map<String, Integer> mPOSVocab = new HashMap<String, Integer>();

    /**
     * train word classifier
     */
    public void initData(Sample sample) {

        mTag2Id.put("nr", 0);
        mTag2Id.put("ns", 1);
        mTag2Id.put("nt", 2);
        mTag2Id.put("n", 3);
        // load noun
        loadNounData(sample);
        statKnowledge(sample);
    }

    /**
     * load noun and pattern
     */
    private void loadNounData(Sample sample) {
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

        LogUtil.info(String.format("nr %d  ns %d  nt %d n %d", nrWord2Freq.size(), nsWord2Freq.size(),
                ntWord2Freq.size(), nWord2Freq.size()));
        addSample(sortMapByValues(nrWord2Freq), "nr");
        addSample(sortMapByValues(nsWord2Freq), "ns");
        addSample(sortMapByValues(ntWord2Freq), "nt");
        addSample(sortMapByValues(nWord2Freq), "n");
    }
    private void addSample(List<String> nouns, String tag){
        for (String noun : nouns){
            mNouns.put(noun, mTag2Id.get(tag));
        }
    }

    private void statKnowledge(Sample sample) {
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);

                if (mNouns.containsKey(word)) {
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
                    mNoun2Feature.put(word, posFeature);
                }
            }
        }
        LogUtil.info(" stat know mNoun positive sample : " + mNouns.size());
    }

    private void add2vocab(String token, Map<String, Integer> vocab){
        if (!vocab.containsKey(token)){
            vocab.put(token, vocab.size());
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

    public void trainPOSClassifier(String featurePath, String pyEnv, String scriptPath){
        POSFeatureBuilder featureBuilder = new POSFeatureBuilder(mNouns);
        featureBuilder.buildFeature(featurePath + "pc_train_feature.txt", mNoun2Feature, mVocab,
                mCharVocab, mPOSVocab);
        String command = pyEnv + "python3 " + scriptPath + "linear.py train_pc";
        PythonUtil.runPython(command);
    }
}
