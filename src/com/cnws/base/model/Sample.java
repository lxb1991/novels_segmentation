package com.cnws.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sample {

    private List<Sentence> mSentences;

    public Sample() {
        mSentences = new ArrayList<Sentence>();
    }

    public int size() {
        return mSentences.size();
    }

    public void extendSentence(List<String> sentence, List<String> posSeq) {
        /**
         * just use for load sample
         */
        mSentences.add(new Sentence(sentence, posSeq, true));
    }

    public Sentence getSentenceById(int idx) {
        return mSentences.get(idx);
    }

    public void shuffle(){
        Collections.shuffle(mSentences);
    }

    public void sub(int index){
        mSentences = mSentences.subList(0, index);
    }
}

