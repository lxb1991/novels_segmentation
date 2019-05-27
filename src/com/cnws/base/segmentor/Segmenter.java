package com.cnws.base.segmentor;

import com.cnws.base.model.Measure;
import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.base.model.Weight;
import com.cnws.util.LogUtil;

import java.io.*;
import java.util.*;

public abstract class Segmenter {

    private static final int EPOCH_SIZE = 60;
    private static final int BEAM_SIZE = 16;
    private static final String[] PENN_TAG_CLOSED = {"P", "DEC", "DEG", "CC", "PN", "DT", "VC", "AS", "VE", "ETC",
            "MSP", "CS", "BA", "DEV", "SB", "SP", "LB", "DER", "PU"};
//    private static final String[] PFR_TAG_CLOSED = {"w", "u", "p", "c", "f", "r", "q", "y", "e", "o", "l", "i", "k", "h"};
    private static final String[] PFR_TAG_CLOSED = {"p", "c", "f", "r", "u", "y", "w"};
    private Set<String> mPOSClosed = new HashSet<String>();
    private String mModelPath;

    // train flag
    private boolean mPrepare = false;
    private boolean mTrainMode = false;
    int mTrainStep = 0;
    Weight mWeight = new Weight();
    private Measure mMeasure = new Measure();
    private double maxF1 = 0;
    String mModelname = "final.model";

    private Map<String, HashSet<String>> mFreqWord2POS = new HashMap<String, HashSet<String>>();
    private Map<String, HashSet<String>> mChar2POS = new HashMap<String, HashSet<String>>();
    Map<String, Integer> mPOS2Len = new HashMap<String, Integer>();
    private Set<String> mHeadCharacters = new HashSet<String>();
    private Set<String> mPOSVocab = new HashSet<String>();

    private Map<String, HashSet<String>> mClosedPOS2Word = new HashMap<String, HashSet<String>>();
    private Map<String, HashSet<String>> mClosedWord2POS = new HashMap<String, HashSet<String>>();

    protected Map<String, String> mPOS2Hash = new HashMap<String, String>();
    protected List<Sentence> mBeamSourceAgenda = new ArrayList<Sentence>();
    protected List<Sentence> mBeamTargetAgenda = new ArrayList<Sentence>();

    Segmenter(String dataType, String modelPath) {
        if (dataType.equals("PFR")) {
            mPOSClosed.addAll(Arrays.asList(PFR_TAG_CLOSED));
        } else {
            mPOSClosed.addAll(Arrays.asList(PENN_TAG_CLOSED));
        }
        mModelPath = modelPath;
    }


    public void statKnowledge(Sample trainSample) {

        long maxWordFrequency = 0;
        Map<String, HashSet<String>> word2POS = new HashMap<String, HashSet<String>>();
        Map<String, Long> word2Freq = new HashMap<String, Long>();

        for (int sampleIdx = 0; sampleIdx < trainSample.size(); sampleIdx++) {
            Sentence sentence = trainSample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {

                String word = sentence.getWordByIdx(wIdx);
                String pos = sentence.getPOSByIdx(wIdx);

                if (word2POS.containsKey(word)) {
                    word2POS.get(word).add(pos);
                } else {
                    HashSet<String> posSet = new HashSet<String>();
                    posSet.add(pos);
                    word2POS.put(word, posSet);
                }

                // count word frequency
                if (word2Freq.containsKey(word)) {
                    long wf = word2Freq.get(word);
                    word2Freq.put(word, wf + 1);
                    if (wf + 1 > maxWordFrequency) {
                        maxWordFrequency = wf + 1;
                    }
                } else {
                    word2Freq.put(word, 1L);
                }

                // fix closed POS set
                if (mPOSClosed.contains(pos)) {
                    if (mClosedWord2POS.containsKey(word)) {
                        mClosedWord2POS.get(word).add(pos);
                    } else {
                        HashSet<String> posSet = new HashSet<String>();
                        posSet.add(pos);
                        mClosedWord2POS.put(word, posSet);
                    }

                    if (mClosedPOS2Word.containsKey(pos)) {
                        mClosedPOS2Word.get(pos).add(word);
                    } else {
                        HashSet<String> wordSet = new HashSet<String>();
                        wordSet.add(word);
                        mClosedPOS2Word.put(pos, wordSet);
                    }
                }

                // stat char2pos info
                for (int cIdx = 0; cIdx < word.length(); cIdx++) {
                    String character = word.substring(cIdx, cIdx + 1);
                    if (mChar2POS.containsKey(character)) {
                        mChar2POS.get(character).add(pos);
                    } else {
                        HashSet<String> posSet = new HashSet<String>();
                        posSet.add(pos);
                        mChar2POS.put(character, posSet);
                    }
                }
                //stat head
                String head = word.substring(0, 1);
                mHeadCharacters.add(head);
                // stat pos len info
                int temp_max = 0;
                if (mPOS2Len.containsKey(pos)) {
                    temp_max = mPOS2Len.get(pos);
                }
                mPOS2Len.put(pos, Math.max(word.length(), temp_max));
                mPOSVocab.add(pos);
            }
        }

        ArrayList<String> POSVocabList = new ArrayList<String>(mPOSVocab);
        for (String c : mChar2POS.keySet()) {
            mPOS2Hash.put(c, hashPOSTag(c, POSVocabList));
        }

        long threshold = maxWordFrequency / 5000 + 5;
        for (Map.Entry<String, Long> entry : word2Freq.entrySet()) {
            String word = entry.getKey();
            long freq = entry.getValue();
            if (freq >= threshold) {
                if (mFreqWord2POS.containsKey(word)) {
                    mFreqWord2POS.get(word).addAll(word2POS.get(word));
                } else {
                    mFreqWord2POS.put(word, word2POS.get(word));
                }
            }
        }
        mPrepare = true;
        LogUtil.info(String.format("stat train sample vocab: %d, close set size %d, close word size %d," +
                        "head char size %d, pos2len size %d.", mPOSVocab.size(), mClosedPOS2Word.size(),
                mClosedWord2POS.size(), mHeadCharacters.size(), mPOS2Len.size()));
    }

    public void train(Sample trainSample, Sample devSample, Sample testSample) {

        if (!mPrepare) {
            throw new Error("must be call the function of statKnowledge before");
        } else {
            LogUtil.info(String.format("train sample size %d, dev sample size %d, test sample size %d",
                    trainSample.size(), devSample.size(), testSample.size()));
        }
        for (int epoch = 0; epoch < EPOCH_SIZE; epoch++) {

            trainSample.shuffle();
            LogUtil.info(String.format("train epoch : %s", epoch));
            long trainStartTime = System.currentTimeMillis();
            mTrainMode = true;

            for (int sampleIdx = 0; sampleIdx < trainSample.size(); sampleIdx++) {
                mTrainStep += 1;
                decode(trainSample.getSentenceById(sampleIdx));
            }
            LogUtil.info("spend " + (System.currentTimeMillis() - trainStartTime) + " ms for train");
            mWeight.averagedWeight(mTrainStep);
            LogUtil.info(String.format("param size: %d", mWeight.size()));

            long devStartTime = System.currentTimeMillis();
            dev(devSample);
            LogUtil.info("spend " + (System.currentTimeMillis() - devStartTime) + " ms for dev");

            long testStartTime = System.currentTimeMillis();
            test(testSample, false);
            LogUtil.info("spend " + (System.currentTimeMillis() - testStartTime) + " ms for test");
        }
    }

    private void dev(Sample devSample) {
        mMeasure.initMeasure();
        mTrainMode = false;
        for (int sampleIdx = 0; sampleIdx < devSample.size(); sampleIdx++) {
            Sentence goldenSentence = devSample.getSentenceById(sampleIdx);
            Sentence predSentence = decode(goldenSentence);
            mMeasure.compare(predSentence, goldenSentence);
        }
        double finalF1 = mMeasure.print();
        if (finalF1 > maxF1) {
            saveModel();
            maxF1 = finalF1;
        }
    }

    public void test(Sample testSample, boolean isTest) {
        if (isTest) {
            loadModel();
        }
        mMeasure.initMeasure();
        mTrainMode = false;
        for (int sampleIdx = 0; sampleIdx < testSample.size(); sampleIdx++) {
            Sentence goldenSentence = testSample.getSentenceById(sampleIdx);
            Sentence predSentence = decode(goldenSentence);
            mMeasure.compare(predSentence, goldenSentence);
        }
        mMeasure.print();
    }

    public void inference(Sample sample, boolean measure) {
        if (measure) {
            mMeasure.initMeasure();
        }
        loadModel();
        mTrainMode = false;
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence goldenSentence = sample.getSentenceById(sampleIdx);
            Sentence predSentence = decode(goldenSentence);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < predSentence.size(); i++) {
                sb.append(predSentence.getWordByIdx(i) + "_" + predSentence.getPOSByIdx(i) + " ");
            }
            System.out.println(sb.toString());
            if (measure) {
                mMeasure.compare(predSentence, goldenSentence);
            }
        }
        if (measure) {
            mMeasure.print();
        }
    }

    private String hashPOSTag(String character, ArrayList<String> POSVocabList) {
        StringBuilder sb = new StringBuilder();
        for (String pos : POSVocabList) {
            if (mChar2POS.get(character).contains(pos)) {
                sb.append("0");
            } else {
                sb.append("1");
            }
        }
        return sb.toString();
    }

    abstract Sentence appendAction(Sentence nowSentence, String character);

    abstract Sentence newAction(Sentence nowSentence, String character, String posTag, String characters,
                                int index, int maxIndex);

    private Sentence decode(Sentence sentence) {
        mBeamSourceAgenda.clear();
        mBeamSourceAgenda.add(new Sentence());

        String characters = sentence.getRawWords();
        for (int wIdx = 0; wIdx < characters.length(); wIdx++) {
            String character = characters.substring(wIdx, wIdx + 1);
            for (Sentence nowSentence : mBeamSourceAgenda) {
                //append action
                // pruning for pos length
                if (nowSentence.size() > 0 &&
                        mPOS2Len.get(nowSentence.getLastPOS()) > nowSentence.getLastWord().length()) {
                    mBeamTargetAgenda.add(appendAction(nowSentence, character));
                }
                //new action
                // pruning for first pos vocab
                Set<String> targetPOS = mPOSVocab;
                if (mHeadCharacters.contains(character)) {
                    targetPOS = mChar2POS.get(character);
                }

                Set<String> closedPOS = new HashSet<String>();
                for (String posTag : targetPOS) {
                    for (int l = 1; l <= mPOS2Len.get(posTag) && (wIdx + l < character.length()); l++) {
                        String w = characters.substring(wIdx, wIdx + l);
                        if (mClosedWord2POS.containsKey(w)) {
                            closedPOS.addAll(mClosedWord2POS.get(w));
                        }
                    }
                }
                if (closedPOS.size() > 0) {
                    targetPOS = closedPOS;
                }
                for (String posTag : targetPOS) {
                    mBeamTargetAgenda.add(newAction(nowSentence, character, posTag, characters, wIdx,
                            mPOS2Len.get(posTag)));
                }
            }
            pruning4CompletedWord();
            mBeamSourceAgenda.clear();
            mBeamSourceAgenda.addAll(mBeamTargetAgenda);
            mBeamTargetAgenda.clear();

            dropout();

            if (mTrainMode) {
                if (goldenExist(wIdx + 1, sentence)) {
                    return mBeamSourceAgenda.get(0);
                }
            }
        }
        if (mTrainMode) {
            if (!sentence.equals(mBeamSourceAgenda.get(0))) {
                updateWeight(characters.length() + 1, sentence);
            }
        }
        return mBeamSourceAgenda.get(0);
    }

    private void pruning4CompletedWord() {
        List<Sentence> tempAgenda = new ArrayList<Sentence>();
        for (Sentence target : mBeamTargetAgenda) {
            if (target.size() > 1) {
                String word = target.getLastPreWord();
                String pos = target.getLastPrePOS();
                if (isInClosedSet(word, pos)) {
                    tempAgenda.add(target);
                }
            } else {
                tempAgenda.add(target);
            }
        }
        mBeamTargetAgenda.clear();
        mBeamTargetAgenda.addAll(tempAgenda);
    }

    private void pruning4Signature() {
        List<Sentence> tempBeamAgenda = new ArrayList<Sentence>();
        for (Sentence s : mBeamSourceAgenda) {
            if (s.size() > 1) {
                String sKey = s.getLastPreWord() + ":" + s.getLastPrePOS() + ":" + s.getLastPOS();
                boolean dropFlag = false;
                for (Sentence temp : mBeamSourceAgenda) {
                    if (s != temp && temp.size() > 1) {
                        String tempKey = temp.getLastPreWord() + ":" + temp.getLastPrePOS() + ":" + temp.getLastPOS();
                        if (tempKey.equals(sKey) && s.getScore() < temp.getScore()) {
                            dropFlag = true;
                        }
                    }
                }
                if (!dropFlag) {
                    tempBeamAgenda.add(s);
                }
            } else {
                tempBeamAgenda.add(s);
            }
        }
        mBeamSourceAgenda.clear();
        mBeamSourceAgenda.addAll(tempBeamAgenda);
    }

    void dropout() {

        for (Sentence s : mBeamSourceAgenda) {
            double score = mWeight.computeScore(s.getFeature());
            s.setScore(score);
        }
        pruning4Signature();
        LinkedList<Sentence> scoreAgenda = new LinkedList<Sentence>();
        scoreAgenda.add(mBeamSourceAgenda.get(0));
        for (int i = 1; i < mBeamSourceAgenda.size(); i++) {

            Sentence now = mBeamSourceAgenda.get(i);
            for (int j = 0; j < scoreAgenda.size(); j++) {

                if (now.getScore() >= scoreAgenda.get(j).getScore()) {
                    scoreAgenda.add(j, now);
                    break;
                } else if (j == scoreAgenda.size() - 1) {
                    scoreAgenda.addLast(now);
                    break;
                }
            }
        }
        int sourceSize = mBeamSourceAgenda.size();
        mBeamSourceAgenda.clear();
        for (int i = 0; i < Math.min(BEAM_SIZE, sourceSize); i++) {
            mBeamSourceAgenda.add(scoreAgenda.get(i));
        }
    }

    boolean isInClosedSet(String word, String pos) {
        boolean flag = false;
        if (mFreqWord2POS.containsKey(word) && mFreqWord2POS.get(word).contains(pos)) {
            flag = true;
        }
        if (mClosedWord2POS.containsKey(word) && mClosedWord2POS.get(word).contains(pos)) {
            flag = true;
        }
        if (!mFreqWord2POS.containsKey(word) && !mClosedWord2POS.containsKey(word)) {
            flag = true;
        }
        return flag;
    }

    private void saveModel() {
        ObjectOutputStream oo;
        try {
            LogUtil.info("save model: " + mModelPath + mModelname);
            oo = new ObjectOutputStream(new FileOutputStream(new File(mModelPath + mModelname)));
            oo.writeObject(this.mWeight);
            oo.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info("save model error!");
        }
    }

    private void loadModel() {
        ObjectInputStream oi;
        try {
            LogUtil.info("load model from : " + mModelPath + mModelname);
            oi = new ObjectInputStream(new FileInputStream(new File(mModelPath + mModelname)));
            mWeight = (Weight) oi.readObject();
            oi.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.info("load model error!");
        }
    }

    abstract boolean goldenExist(int wordIdx, Sentence goldenSentence);

    abstract void updateWeight(int wordIdx, Sentence goldenSentence);

    boolean compareCandi(Sentence candi, Sentence golden) {
        int charLen = 0;
        StringBuilder candiSeg = new StringBuilder();
        StringBuilder candiPOS = new StringBuilder();
        for (int i = 0; i < candi.size(); i++) {
            charLen += candi.getWordByIdx(i).length();
            candiSeg.append(candi.getWordByIdx(i));
            candiSeg.append(" ");
            candiPOS.append(candi.getPOSByIdx(i));
            candiPOS.append(" ");
        }
        int gLen = 0;
        StringBuilder goldenSeg = new StringBuilder();
        StringBuilder goldenPOS = new StringBuilder();
        for (int i = 0; i < golden.size(); i++) {
            String word = golden.getWordByIdx(i);
            gLen += word.length();
            if (gLen > charLen) {
                word = word.substring(0, gLen - charLen);
                goldenSeg.append(word);
                goldenSeg.append(" ");
                goldenPOS.append(golden.getPOSByIdx(i));
                goldenPOS.append(" ");
                break;
            } else if (gLen == charLen) {
                goldenSeg.append(word);
                goldenSeg.append(" ");
                goldenPOS.append(golden.getPOSByIdx(i));
                goldenPOS.append(" ");
                break;
            } else {
                goldenSeg.append(word);
                goldenSeg.append(" ");
                goldenPOS.append(golden.getPOSByIdx(i));
                goldenPOS.append(" ");
            }

        }
        return candiSeg.toString().trim().equals(goldenSeg.toString().trim()) &&
                candiPOS.toString().trim().equals(goldenPOS.toString().trim());
    }

}
