package com.cnws.base.segmentor;

import com.cnws.base.model.BaseSentence;
import com.cnws.base.model.Sentence;

public class BaseSegmenter extends Segmenter {

    public BaseSegmenter(String dataType, String modelPath) {
        super(dataType, modelPath);
        mModelname = "BaseModel.obj";
    }

    @Override
    Sentence appendAction(Sentence nowSentence, String character) {
        BaseSentence appendSentence = new BaseSentence(nowSentence.getWords(), nowSentence.getPOS(),
                nowSentence.getFeature());
        appendSentence.updateLastWord(character);
        appendSentence.genFeatureAppend(appendSentence.getLastWord(), appendSentence.getLastPOS());
        return appendSentence;
    }

    @Override
    Sentence newAction(Sentence nowSentence, String character, String posTag, String characters, int index,
                       int maxIndex) {
        int now_len = nowSentence.size();
        String ppWord = now_len > 1 ? nowSentence.getWordByIdx(now_len - 2) : "#";
        String ppPOS = now_len > 1 ? nowSentence.getPOSByIdx(now_len - 2) : "#";
        String pWord = now_len > 0 ? nowSentence.getWordByIdx(now_len - 1) : "#";
        String pPOS = now_len > 0 ? nowSentence.getPOSByIdx(now_len - 1) : "#";

        BaseSentence separateSentence = new BaseSentence(nowSentence.getWords(), nowSentence.getPOS(),
                nowSentence.getFeature());
        separateSentence.addWord(character);
        separateSentence.addPOS(posTag);

        separateSentence.genFeatureSeperate(character, posTag, pWord, pPOS, ppWord, ppPOS, mPOS2Hash);
        return separateSentence;
    }

    @Override
    boolean goldenExist(int wordIdx, Sentence goldenSentence) {
        Sentence golden = BaseSentence.genGoldenFeature(goldenSentence, wordIdx, mPOS2Hash);
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
        Sentence golden = BaseSentence.genGoldenFeature(goldenSentence, wordIdx, mPOS2Hash);
        mWeight.updateWeight(mBeamSourceAgenda.get(0).getFeature(), -1.0, mTrainStep);
        mWeight.updateWeight(golden.getFeature(), 1.0, mTrainStep);
    }
}
