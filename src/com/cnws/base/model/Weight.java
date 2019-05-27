package com.cnws.base.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;


public class Weight implements Serializable {

    private HashMap<String, Double> mWeight = new HashMap<String, Double>();
    private HashMap<String, Double> mDeltaWeight = new HashMap<String, Double>();
    private HashMap<String, Integer> mDeltaWeightStep = new HashMap<String, Integer>();


    public void updateWeight(List<String> features, double updateValue, int step){
        for(String feat : features){
            if(!mWeight.containsKey(feat)){
                mWeight.put(feat, 0.0);
                mDeltaWeight.put(feat, 0.0);
            }else{
                mDeltaWeight.put(feat, mDeltaWeight.get(feat) + (step - mDeltaWeightStep.get(feat)) * mWeight.get(feat));
            }
            mDeltaWeightStep.put(feat, step);
            mWeight.put(feat, mWeight.get(feat) + updateValue);
        }
    }

    public void averagedWeight(int step){
        for(String key : mDeltaWeight.keySet()){
            mDeltaWeight.put(key, mDeltaWeight.get(key) + (step - mDeltaWeightStep.get(key)) * mWeight.get(key));
            mDeltaWeightStep.put(key, step);
            mWeight.put(key, mDeltaWeight.get(key) / step);
        }

    }

    private double getWeight(String feat){
        if(mWeight.containsKey(feat)){
            return mWeight.get(feat);
        }else{
            return 0.0;
        }
    }

    public double computeScore(List<String> feature){
        double resultScore = 0;
        for(String feat : feature){
            resultScore += getWeight(feat);
        }
        return resultScore;
    }

    public int size(){
        return mWeight.size();
    }

    public int delta_size(){
        return mDeltaWeight.size();
    }
}
