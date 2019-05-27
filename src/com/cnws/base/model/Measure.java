package com.cnws.base.model;

import com.cnws.util.LogUtil;

import java.util.HashSet;
import java.util.List;

public class Measure {

    private double segPredictLength = 0.0;
    private double segGoldenLength = 0.0;
    private double segCoLength = 0.0;

    private double posPredictLength = 0.0;
    private double posGoldenLength = 0.0;
    private double posCOLength = 0.0;

    public void initMeasure(){
        segPredictLength = 0;
        segGoldenLength = 0;
        segCoLength = 0;

        posPredictLength = 0;
        posGoldenLength = 0;
        posCOLength = 0;
    }

    public void compare(Sentence predict, Sentence golden){
        assert predict.getRawWords().equals(golden.getRawWords());
        List<String> predWords = predict.getWords();
        List<String> predPOS = predict.getPOS();

        List<String> goldenWords = golden.getWords();
        List<String> goldenPOS = golden.getPOS();

        segPredictLength += predWords.size();
        segGoldenLength += goldenWords.size();
        segCoLength += coInfo(predWords, goldenWords);

        posPredictLength += predPOS.size();
        posGoldenLength += goldenPOS.size();
        posCOLength += coPOSInfo(predWords, predPOS, goldenWords, goldenPOS);
    }

    public double print(){
        double segP = segCoLength / segPredictLength;
        double segR = segCoLength / segGoldenLength;
        double segF1 = 2 * segP * segR / (segP + segR);
        LogUtil.info(String.format("segmentation p %f, r %f, f1 %f", segP, segR, segF1));
        double posP = posCOLength / posPredictLength;
        double posR = posCOLength / posGoldenLength;
        double posF1 = 2 * posP * posR / (posP + posR);
        LogUtil.info(String.format("pos tag p %f, r %f, f1 %f", posP, posR, posF1));
        return segF1 + posF1;
    }

    private int coInfo(List<String> predict, List<String> golden){
        int pLen = 0;
        int gLen = 0;
        HashSet<String> pSet = new HashSet<String>();
        HashSet<String> gSet = new HashSet<String>();
        for(String p : predict){
            pSet.add(p + ":" +pLen);
            pLen += p.length();
        }
        for(String g : golden){
            gSet.add(g + ":" + gLen);
            gLen += g.length();
        }
        pSet.retainAll(gSet);
        return pSet.size();
    }

    private int coPOSInfo(List<String> predWords, List<String> predPOS, List<String> goldenWords, List<String> goldenPOS){
        int pLen = 0;
        int gLen = 0;
        HashSet<String> pSet = new HashSet<String>();
        HashSet<String> gSet = new HashSet<String>();
        for(int i=0; i < predWords.size(); i++){
            pSet.add(predPOS.get(i) + ":" + pLen);
            pLen += predWords.get(i).length();

        }
        for(int i=0; i < goldenWords.size(); i++){
            gSet.add(goldenPOS.get(i) + ":" + gLen);
            gLen += goldenWords.get(i).length();
        }
        pSet.retainAll(gSet);
        return pSet.size();
    }
}
