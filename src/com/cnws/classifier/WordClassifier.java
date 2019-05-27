package com.cnws.classifier;

import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.util.FileUtil;
import com.cnws.util.LogUtil;
import com.cnws.util.PythonUtil;

import java.io.*;
import java.util.*;


public class WordClassifier {

    private static final String[] PUNCTUATION_DICT = {"∶", "（", "？", "××", "」", "〉", "⑤", "⑦",
            "…………", "⑵", "③", "“", "┅", "～", "％", "［", "＊", "④", "『", "———", "〈", "②", "》",
            "；", "，", "＞", "］", "──", "——", "∶", "’", "－", "‘", "……", "「", "●", "△", "⑥", "?",
            "”", "±％", "①", "·", "、", "」", "▲", "《", "＝", "』", "※", "：", "—", "／", "×",
            "〓", "！", "。", "‰", "）"};

    private Set<String> mPunctuation = new HashSet<String>(Arrays.asList(PUNCTUATION_DICT));

    private Set<String> mNouns = new HashSet<String>();
    private Map<String, Map<String, Integer>> mNoun2Pattern = new HashMap<String, Map<String, Integer>>();

    private Set<String> mNegativeNouns = new HashSet<String>();
    private Map<String, Map<String, Integer>> mNegativeNoun2Pattern = new HashMap<String, Map<String, Integer>>();

    private Set<String> mPatterns = new HashSet<String>();
    private Map<String, Integer> mNoun2Freq = new HashMap<String, Integer>();
    private Set<String> mSubWordCache = new HashSet<String>();
    private Map<String, Double> mSubWord2Freq = new HashMap<String, Double>();
    private Set<String> mNoun2Punctuation = new HashSet<String>();

    /**
     * train word classifier
     */
    public void initData(Sample sample) {
        // load noun
        loadNounData(sample);
        // load pattern
        loadPattern(sample);
        statKnowledge(sample, true);
        LogUtil.info(String.format("extract pos noun: %d, negative noun: %d, pattern: %d.",
                mNoun2Pattern.size(), mNegativeNouns.size(), mPatterns.size()));
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

                if (mNoun2Freq.containsKey(word)) {
                    mNoun2Freq.put(word, mNoun2Freq.get(word) + 1);
                } else {
                    mNoun2Freq.put(word, 1);
                }

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

        mNouns.addAll(sortMapByValues(nrWord2Freq));
        mNouns.addAll(sortMapByValues(nsWord2Freq));
        mNouns.addAll(sortMapByValues(ntWord2Freq));
        mNouns.addAll(sortMapByValues(nWord2Freq));
    }

    private void loadPattern(Sample sample) {

        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {
                String word = sentence.getWordByIdx(wIdx);
                if (mNouns.contains(word)) {
                    String pWord = "#";
                    if (wIdx > 0) {
                        pWord = sentence.getWordByIdx(wIdx - 1);
                    }
                    String aWord = "#";
                    if (wIdx + 1 < sentence.size()) {
                        aWord = sentence.getWordByIdx(wIdx + 1);
                    }
                    String pattern = pWord + "_" + aWord;
                    mPatterns.add(pattern);
                }
            }
        }
    }

    /**
     * compute PMI by mNouns, and build punctuation feature, and fill noun2pattern
     */
    private void statKnowledge(Sample sample, boolean genNegative) {
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

                    String negativeWord = word + aWord;
                    if (genNegative && !mNouns.contains(negativeWord)) {
                        // negative sample
                        String aaWord = "#";
                        if (wIdx + 2 < sentence.size()) {
                            aaWord = sentence.getWordByIdx(wIdx + 2);
                        }
                        if (!mNegativeNouns.contains(negativeWord)) {
                            // compute PMI
                            buildSubWordFreq(negativeWord.length() > 1 ? negativeWord.substring(0, 1) : null);
                            buildSubWordFreq(negativeWord.length() > 1 ? negativeWord.substring(1) : null);
                            buildSubWordFreq(negativeWord.length() > 2 ? negativeWord.substring(0, 2) : null);
                            buildSubWordFreq(negativeWord.length() > 2 ? negativeWord.substring(2) : null);
                            buildSubWordFreq(negativeWord.length() > 1 ? negativeWord.substring(0, negativeWord.length() - 1) : null);
                            buildSubWordFreq(negativeWord.length() > 1 ? negativeWord.substring(negativeWord.length() - 1) : null);
                            buildSubWordFreq(negativeWord.length() > 2 ? negativeWord.substring(0, negativeWord.length() - 2) : null);
                            buildSubWordFreq(negativeWord.length() > 2 ? negativeWord.substring(negativeWord.length() - 2) : null);
                            mNegativeNouns.add(negativeWord);
                        }

                        if (mPunctuation.contains(aWord) && mPunctuation.contains(pWord)) {
                            mNoun2Punctuation.add(negativeWord);
                        }

                        String negativePattern = pWord + "_" + aaWord;
                        if (mNegativeNoun2Pattern.containsKey(negativeWord)) {
                            if (mNegativeNoun2Pattern.get(negativeWord).containsKey(negativePattern)) {
                                mNegativeNoun2Pattern.get(negativeWord).put(negativePattern,
                                        mNegativeNoun2Pattern.get(negativeWord).get(negativePattern) + 1);
                            } else {
                                mNegativeNoun2Pattern.get(negativeWord).put(negativePattern, 1);
                            }
                        } else {
                            HashMap<String, Integer> patterns = new HashMap<String, Integer>();
                            patterns.put(negativePattern, 1);
                            mNegativeNoun2Pattern.put(negativeWord, patterns);
                        }
                    }
                }
            }
        }
        LogUtil.info(" stat know mNoun: " + mNouns.size() + " mNoun2Freq size:" + mNoun2Freq.size() +
                " mSubWordCache size :" + mSubWordCache.size());

        int lineNum = 0;

        for (String sub : mSubWordCache) {
            if (mNoun2Freq.containsKey(sub)) {
                int count = mNoun2Freq.get(sub);
                if (mSubWord2Freq.containsKey(sub)) {
                    mSubWord2Freq.put(sub, mSubWord2Freq.get(sub) + (double) count);
                } else {
                    mSubWord2Freq.put(sub, (double) count);
                }
            }
            if (genNegative && lineNum % 50000 == 0) {
                LogUtil.info("count num: " + lineNum);
            }
            lineNum += 1;
            if (!mSubWord2Freq.containsKey(sub)) {
                mSubWord2Freq.put(sub, 1.0);
            }
        }

        if (genNegative) {
            for (String neg : mNegativeNouns) {
                if (!mSubWord2Freq.containsKey(neg) || (mSubWord2Freq.containsKey(neg) && mSubWord2Freq.get(neg) == 0)) {
                    mSubWord2Freq.put(neg, 1.0);
                }
            }
        }
    }

    private void buildSubWordFreq(String subWord) {
        if (subWord != null) {
            mSubWordCache.add(subWord);
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

    public void trainWordClassifier(String featurePath, String pyEnv, String scriptPath){
        int allNounSize = 0;
        for (String noun : mNoun2Freq.keySet()) {
            allNounSize += mNoun2Freq.get(noun);
        }
        WcFeatureBuilder featureBuilder = new WcFeatureBuilder(mNouns, mNegativeNouns);
        featureBuilder.buildFeature(featurePath + "wc_train_feature.txt", allNounSize, mNoun2Punctuation,
                mPatterns, mNoun2Pattern, mNegativeNoun2Pattern, mSubWord2Freq, mPunctuation);

        String command = pyEnv + "python3 " + scriptPath + "linear.py train_wc";
        PythonUtil.runPython(command);
    }
}
