package com.cnws.classifier;

import com.cnws.util.LogUtil;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class POSFeatureBuilder {
    private Map<String, Integer> mPositiveSample;

    public POSFeatureBuilder(Map<String, Integer> pSample) {
        mPositiveSample = pSample;
    }


    public void resetPOS(Map<String, Integer> pSample) {
        mPositiveSample = pSample;
    }

    public void buildFeature(String outputFile, Map<String, List<String>> pos2Feature, Map<String, Integer> vocab,
                             Map<String, Integer> charVocab,Map<String, Integer> POSVocab) {

        int featureNum = 8;
        ArrayList<String> outWord = new ArrayList<String>();

        int sampleNum = mPositiveSample.size();
        double[][] mFeature = new double[sampleNum][featureNum];
        double[] mLabel = new double[sampleNum];

        int sampleIdx = 0;
        for (String noun : mPositiveSample.keySet()) {

            outWord.add(noun);
            List<String> feats = pos2Feature.get(noun);
            mFeature[sampleIdx][0] = vocab.containsKey(feats.get(0)) ? vocab.get(feats.get(0)) : -1.0;
            mFeature[sampleIdx][1] = vocab.containsKey(feats.get(1)) ? vocab.get(feats.get(1)) : -1.0;
            mFeature[sampleIdx][2] = vocab.containsKey(feats.get(2)) ? vocab.get(feats.get(2)) : -1.0;
            mFeature[sampleIdx][3] = vocab.containsKey(feats.get(3)) ? vocab.get(feats.get(3)) : -1.0;
            mFeature[sampleIdx][4] = POSVocab.containsKey(feats.get(4)) ? POSVocab.get(feats.get(4)) : -1.0;
            mFeature[sampleIdx][5] = POSVocab.containsKey(feats.get(5)) ? POSVocab.get(feats.get(5)) : -1.0;
            mFeature[sampleIdx][6] = charVocab.containsKey(feats.get(6)) ? charVocab.get(feats.get(6)) : -1.0;
            mFeature[sampleIdx][7] = charVocab.containsKey(feats.get(7)) ? charVocab.get(feats.get(7)) : -1.0;

            mLabel[sampleIdx] = mPositiveSample.get(noun);
            sampleIdx += 1;
        }

        LogUtil.info("build pos feature completed!");

        try {
            FileWriter fw = new FileWriter(outputFile);
            int idx = 0;
            for (double[] feat : mFeature) {
                for (double value : feat) {
                    fw.write(value + " ");
                }

                fw.write(mLabel[idx] + " " + outWord.get(idx) + "\n");
                idx += 1;
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.info("write pos  feature to : " +  outputFile);
    }
}
