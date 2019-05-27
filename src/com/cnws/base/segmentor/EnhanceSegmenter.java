package com.cnws.base.segmentor;

import com.cnws.base.model.EnhanceSentence;
import com.cnws.base.model.Sample;
import com.cnws.base.model.Sentence;
import com.cnws.util.FileUtil;
import com.cnws.util.LogUtil;

import java.util.*;

public class EnhanceSegmenter extends Segmenter {

    private Map<String, Set<String>> mNewNoun = new HashMap<String, Set<String>>();
    private Set<String> mNewPattern = new HashSet<String>();
    private Map<String, String> mNewNoun2POS = new HashMap<String, String>();

    private Set<String> mFixedNounTag = new HashSet<String>();
    private static final String[] PFR_TAG = {"nr", "ns", "nt", "nz", "nx", "n"};
    private String mNounPath;
    private String mPatternPath;
    private String mPOSPath;
    private String mMode;

    public EnhanceSegmenter(String dataType, String modelPath, String npPath, String mode, String subName) {
        super(dataType, modelPath);
        mFixedNounTag.addAll(Arrays.asList(PFR_TAG));
        mModelname = "EnhanceModel.obj";
        mNounPath = npPath + "./" + subName  + "_noun.txt";
        mPatternPath = npPath + "./" + subName  + "_pattern.txt";
        mPOSPath = npPath + "./" + subName  + "_pos.txt";
        mMode = mode;
    }

    @Override
    Sentence appendAction(Sentence nowSentence, String character) {
        EnhanceSentence appendSentence = new EnhanceSentence(nowSentence.getWords(), nowSentence.getPOS(),
                nowSentence.getFeature());
        appendSentence.updateLastWord(character);
        appendSentence.genFeatureAppend(appendSentence.getLastWord(), appendSentence.getLastPOS());
        return appendSentence;
    }

    @Override
    Sentence newAction(Sentence nowSentence, String character, String posTag, String characters, int index, int maxIndex) {
        int now_len = nowSentence.size();
        String ppWord = now_len > 1 ? nowSentence.getWordByIdx(now_len - 2) : "#";
        String ppPOS = now_len > 1 ? nowSentence.getPOSByIdx(now_len - 2) : "#";
        String pWord = now_len > 0 ? nowSentence.getWordByIdx(now_len - 1) : "#";
        String pPOS = now_len > 0 ? nowSentence.getPOSByIdx(now_len - 1) : "#";

        EnhanceSentence separateSentence = new EnhanceSentence(nowSentence.getWords(), nowSentence.getPOS(),
                nowSentence.getFeature());
        separateSentence.addWord(character);
        separateSentence.addPOS(posTag);

        separateSentence.genFeatureSeperate(character, posTag, pWord, pPOS, ppWord, ppPOS, mPOS2Hash,
                mNewNoun, mNewPattern, mNewNoun2POS, characters, index, maxIndex);
        return separateSentence;
    }

    @Override
    boolean goldenExist(int wordIdx, Sentence goldenSentence) {

        Sentence golden = EnhanceSentence.genGoldenFeature(goldenSentence, wordIdx, mPOS2Hash, mNewNoun,
                mNewPattern, mNewNoun2POS, mPOS2Len);
        for (Sentence candi : mBeamSourceAgenda) {
            if (compareCandi(candi, golden)) {
                return false;
            }
        }
        earlyUpdate(golden);
        return true;
    }

    private void earlyUpdate(Sentence golden) {
        mWeight.updateWeight(mBeamSourceAgenda.get(0).getFeature(), -1.0, mTrainStep);
        mWeight.updateWeight(golden.getFeature(), 1.0, mTrainStep);
    }

    @Override
    void updateWeight(int wordIdx, Sentence goldenSentence) {
        Sentence golden = EnhanceSentence.genGoldenFeature(goldenSentence, wordIdx, mPOS2Hash,
                mNewNoun, mNewPattern, mNewNoun2POS, mPOS2Len);
        mWeight.updateWeight(mBeamSourceAgenda.get(0).getFeature(), -1.0, mTrainStep);
        mWeight.updateWeight(golden.getFeature(), 1.0, mTrainStep);
    }

    @Override
    public void statKnowledge(Sample trainSample) {
        super.statKnowledge(trainSample);
        loadNewsNounPattern(trainSample);
        if (!"train".equals(mMode)){
            loadNovelNounPattern(mNounPath, mPatternPath, mPOSPath);
        }
        LogUtil.info("load noun size: " + mNewNoun.size() + " pattern size: " + mNewPattern.size() + " pos size: "
                + mNewNoun2POS.size());
    }

    private void loadNovelNounPattern(String nounPath, String patternPath, String posPath){
        mNewPattern.addAll(FileUtil.readPattern(patternPath));
        mNewNoun.putAll(FileUtil.readNoun(nounPath));
        mNewNoun2POS.putAll(FileUtil.readPOS(posPath));
    }

    private void loadNewsNounPattern(Sample sample){
        Map<String, HashSet<String>> tempNewNoun = new HashMap<String, HashSet<String>>();
        Map<String, Integer> tempNewPattern2Freq = new HashMap<String, Integer>();
        Map<String, String> tempNewNoun2POS = new HashMap<String, String>();
        for (int sampleIdx = 0; sampleIdx < sample.size(); sampleIdx++) {
            Sentence sentence = sample.getSentenceById(sampleIdx);
            for (int wIdx = 0; wIdx < sentence.size(); wIdx++) {

                String word = sentence.getWordByIdx(wIdx);
                String pos = sentence.getPOSByIdx(wIdx);

                String pWord;
                if (wIdx > 0){
                    pWord = sentence.getWordByIdx(wIdx - 1);
                }else{
                    pWord = "#";
                }

                String aWord;
                if (wIdx < sentence.size() - 1){
                    aWord = sentence.getWordByIdx(wIdx + 1);
                }else{
                    aWord = "#";
                }

                if(mFixedNounTag.contains(pos)){
                    if (tempNewNoun.containsKey(word)){
                        tempNewNoun.get(word).add(pWord + "_" + aWord);
                    }else{
                        HashSet<String> pattern = new HashSet<String>();
                        pattern.add(pWord + "_" + aWord);
                        tempNewNoun.put(word, pattern);
                    }

                    String pattern = pWord + "_" + aWord;
                    if (tempNewPattern2Freq.containsKey(pattern)){
                        tempNewPattern2Freq.put(pattern, tempNewPattern2Freq.get(pattern) + 1);
                    }else{
                        tempNewPattern2Freq.put(pattern, 1);
                    }

                    if ("nx".equals(pos) || "nz".equals(pos)){
                        tempNewNoun2POS.put(word, "n");
                    }else{
                        tempNewNoun2POS.put(word, pos);
                    }
                }
            }
        }

        for (String pat : tempNewPattern2Freq.keySet()){
            if (tempNewPattern2Freq.get(pat) >= 10){
                mNewPattern.add(pat);
            }
        }

        for (String noun : tempNewNoun.keySet()){
            int count = 0;
            for (String pat : tempNewNoun.get(noun)){
                if (mNewPattern.contains(pat)){
                    count += 1;
                }
            }
            if (count > 1){
                mNewNoun.put(noun, tempNewNoun.get(noun));
                mNewNoun2POS.put(noun, tempNewNoun2POS.get(noun));
            }
        }
    }
}
