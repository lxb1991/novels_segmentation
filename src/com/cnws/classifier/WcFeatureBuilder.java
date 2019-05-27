package com.cnws.classifier;

import com.cnws.util.LogUtil;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class WcFeatureBuilder {

    private Set<String> mPositiveSample;
    private Set<String> mNegativeSample;

    public WcFeatureBuilder(Set<String> pSample) {
        mPositiveSample = pSample;
    }

    public WcFeatureBuilder(Set<String> pSample, Set<String> nSample) {
        mPositiveSample = pSample;
        mNegativeSample = nSample;
    }

    public void exchangeSample(Set<String> rSample){
        mPositiveSample = rSample;
    }

    public void buildFeature(String outputFile, int nounSize, Set<String> nounPunctuation, Set<String> allPatterns,
                             Map<String, Map<String, Integer>> posNoun2Pattern,
                             Map<String, Map<String, Integer>> negNoun2Pattern,
                             Map<String, Double> subWord2Freq, Set<String> PUNCTUATION) {

        int featureNum = 24;
        ArrayList<String> outWord = new ArrayList<String>();
        int sampleNum;
        if (mNegativeSample != null) {
            sampleNum = mPositiveSample.size() + mNegativeSample.size();
        } else {
            sampleNum = mPositiveSample.size();
        }
        double[][] mFeature = new double[sampleNum][featureNum];
        double[] mLabel = new double[sampleNum];

        int sampleIdx = 0;
        for (String noun : mPositiveSample) {

            outWord.add(noun.trim());

            mFeature[sampleIdx][0] = nounPunctuation.contains(noun) ? 1 : 0;

            Map<String, Integer> patterns = posNoun2Pattern.get(noun);
            // count pattern
            int patternsSize = 0;
            for (String pattern : patterns.keySet()) {
                if (allPatterns.contains(pattern)) {
                    patternsSize += 1;
                }
            }
            mFeature[sampleIdx][1] = patternsSize == 1 ? 1 : 0;
            mFeature[sampleIdx][2] = patternsSize > 1 && patternsSize < 10 ? 1 : 0;
            mFeature[sampleIdx][3] = patternsSize > 10 && patternsSize <= 20 ? 1 : 0;
            mFeature[sampleIdx][4] = patternsSize > 20 ? 1 : 0;

            // freq pattern
            int allPattenSize = 0;
            for (String pattern : patterns.keySet()) {
                if (allPatterns.contains(pattern)) {
                    allPattenSize += patterns.get(pattern);
                }
            }

            mFeature[sampleIdx][5] = allPattenSize < 5 ? 1 : 0;
            mFeature[sampleIdx][6] = allPattenSize >= 5 && allPattenSize < 20 ? 1 : 0;
            mFeature[sampleIdx][7] = allPattenSize >= 20 && allPattenSize < 50 ? 1 : 0;
            mFeature[sampleIdx][8] = allPattenSize >= 50 ? 1 : 0;

            double freq = subWord2Freq.get(noun);

            if (noun.length() > 1) {

                mFeature[sampleIdx][9] = Math.log(freq * nounSize / subWord2Freq.get(noun.substring(0, 1))
                        * subWord2Freq.get(noun.substring(1)));

                mFeature[sampleIdx][10] = Math.log((freq * nounSize) /
                        subWord2Freq.get(noun.substring(0, noun.length() - 1))
                        * subWord2Freq.get(noun.substring(noun.length() - 1)));

                if (Double.isNaN(mFeature[sampleIdx][9]) || Double.isInfinite(mFeature[sampleIdx][9])) {
                    mFeature[sampleIdx][9] = 0.0;
                }
                if (Double.isNaN(mFeature[sampleIdx][10]) || Double.isInfinite(mFeature[sampleIdx][10])) {
                    mFeature[sampleIdx][10] = 0.0;
                }

                mFeature[sampleIdx][14] = subWord2Freq.get(noun.substring(0, 1));
                mFeature[sampleIdx][15] = subWord2Freq.get(noun.substring(1));
                mFeature[sampleIdx][16] = subWord2Freq.get(noun.substring(0, noun.length() - 1));
                mFeature[sampleIdx][17] = subWord2Freq.get(noun.substring(noun.length() - 1));
            } else {
                mFeature[sampleIdx][9] = 0.0;
                mFeature[sampleIdx][10] = 0.0;

                mFeature[sampleIdx][14] = 0.0;
                mFeature[sampleIdx][15] = 0.0;
                mFeature[sampleIdx][16] = 0.0;
                mFeature[sampleIdx][17] = 0.0;
            }

            if (noun.length() > 2) {

                String subC2 = noun.substring(0, noun.length() - 2);
                String subCn2 = noun.substring(noun.length() - 2);

                mFeature[sampleIdx][11] = Math.log((freq * nounSize) / subWord2Freq.get(noun.substring(0, 2))
                        * subWord2Freq.get(noun.substring(2)));

                mFeature[sampleIdx][12] = Math.log((freq * nounSize) / subWord2Freq.get(subC2)
                        * subWord2Freq.get(subCn2));

                if (Double.isNaN(mFeature[sampleIdx][11]) || Double.isInfinite(mFeature[sampleIdx][11])) {
                    mFeature[sampleIdx][11] = 0.0;
                }
                if (Double.isNaN(mFeature[sampleIdx][12]) || Double.isInfinite(mFeature[sampleIdx][12])) {
                    mFeature[sampleIdx][12] = 0.0;
                }

                mFeature[sampleIdx][18] = subWord2Freq.get(noun.substring(0, 2));
                mFeature[sampleIdx][19] = subWord2Freq.get(noun.substring(2));
                mFeature[sampleIdx][20] = subWord2Freq.get(subC2);
                mFeature[sampleIdx][21] = subWord2Freq.get(subCn2);
            } else {
                mFeature[sampleIdx][11] = 0.0;
                mFeature[sampleIdx][12] = 0.0;

                mFeature[sampleIdx][18] = 0.0;
                mFeature[sampleIdx][19] = 0.0;
                mFeature[sampleIdx][20] = 0.0;
                mFeature[sampleIdx][21] = 0.0;
            }
            mFeature[sampleIdx][13] = nounPunctuation.contains(noun) ? 1.0 : 0.0;
            mFeature[sampleIdx][22] = noun.length();
            mFeature[sampleIdx][23] = PUNCTUATION.contains(noun) ? 1.0 : 0.0;
            mLabel[sampleIdx] = 1;
            sampleIdx += 1;
        }

        if (mNegativeSample != null) {
            for (String noun : mNegativeSample) {
                outWord.add(noun);

                mFeature[sampleIdx][0] = nounPunctuation.contains(noun) ? 1 : 0;

                Map<String, Integer> patterns = negNoun2Pattern.get(noun);
                int patternsSize = 0;
                for (String pattern : patterns.keySet()) {
                    if (allPatterns.contains(pattern)) {
                        patternsSize += 1;
                    }
                }
                mFeature[sampleIdx][1] = patternsSize == 1 ? 1 : 0;
                mFeature[sampleIdx][2] = patternsSize > 1 && patternsSize < 10 ? 1 : 0;
                mFeature[sampleIdx][3] = patternsSize > 10 && patternsSize <= 20 ? 1 : 0;
                mFeature[sampleIdx][4] = patternsSize > 20 ? 1 : 0;

                int allPattenSize = 0;
                for (String pattern : patterns.keySet()) {
                    if (allPatterns.contains(pattern)) {
                        allPattenSize += patterns.get(pattern);
                    }
                }
                mFeature[sampleIdx][5] = allPattenSize < 5 ? 1 : 0;
                mFeature[sampleIdx][6] = allPattenSize >= 5 && allPattenSize < 20 ? 1 : 0;
                mFeature[sampleIdx][7] = allPattenSize >= 20 && allPattenSize < 50 ? 1 : 0;
                mFeature[sampleIdx][8] = allPattenSize >= 50 ? 1 : 0;

                double freq = subWord2Freq.get(noun);

                if (noun.length() > 1) {

                    mFeature[sampleIdx][9] = Math.log(freq * nounSize / subWord2Freq.get(noun.substring(0, 1))
                            * subWord2Freq.get(noun.substring(1)));

                    mFeature[sampleIdx][10] = Math.log((freq * nounSize) /
                            subWord2Freq.get(noun.substring(0, noun.length() - 1))
                            * subWord2Freq.get(noun.substring(noun.length() - 1)));

                    if (Double.isNaN(mFeature[sampleIdx][9]) || Double.isInfinite(mFeature[sampleIdx][9])) {
                        mFeature[sampleIdx][9] = 0.0;
                    }
                    if (Double.isNaN(mFeature[sampleIdx][10]) || Double.isInfinite(mFeature[sampleIdx][10])) {
                        mFeature[sampleIdx][10] = 0.0;
                    }

                    mFeature[sampleIdx][14] = subWord2Freq.get(noun.substring(0, 1));
                    mFeature[sampleIdx][15] = subWord2Freq.get(noun.substring(1));
                    mFeature[sampleIdx][16] = subWord2Freq.get(noun.substring(0, noun.length() - 1));
                    mFeature[sampleIdx][17] = subWord2Freq.get(noun.substring(noun.length() - 1));
                } else {
                    mFeature[sampleIdx][9] = 0.0;
                    mFeature[sampleIdx][10] = 0.0;

                    mFeature[sampleIdx][14] = 0.0;
                    mFeature[sampleIdx][15] = 0.0;
                    mFeature[sampleIdx][16] = 0.0;
                    mFeature[sampleIdx][17] = 0.0;
                }

                if (noun.length() > 2) {

                    String subC2 = noun.substring(0, noun.length() - 2);
                    String subCn2 = noun.substring(noun.length() - 2);

                    mFeature[sampleIdx][11] = Math.log((freq * nounSize) / subWord2Freq.get(noun.substring(0, 2))
                            * subWord2Freq.get(noun.substring(2)));

                    mFeature[sampleIdx][12] = Math.log((freq * nounSize) / subWord2Freq.get(subC2)
                            * subWord2Freq.get(subCn2));

                    if (Double.isNaN(mFeature[sampleIdx][11]) || Double.isInfinite(mFeature[sampleIdx][11])) {
                        mFeature[sampleIdx][11] = 0.0;
                    }
                    if (Double.isNaN(mFeature[sampleIdx][12]) || Double.isInfinite(mFeature[sampleIdx][12])) {
                        mFeature[sampleIdx][12] = 0.0;
                    }

                    mFeature[sampleIdx][18] = subWord2Freq.get(noun.substring(0, 2));
                    mFeature[sampleIdx][19] = subWord2Freq.get(noun.substring(2));
                    mFeature[sampleIdx][20] = subWord2Freq.get(subC2);
                    mFeature[sampleIdx][21] = subWord2Freq.get(subCn2);
                } else {
                    mFeature[sampleIdx][11] = 0.0;
                    mFeature[sampleIdx][12] = 0.0;

                    mFeature[sampleIdx][18] = 0.0;
                    mFeature[sampleIdx][19] = 0.0;
                    mFeature[sampleIdx][20] = 0.0;
                    mFeature[sampleIdx][21] = 0.0;
                }

                mFeature[sampleIdx][13] = nounPunctuation.contains(noun) ? 1.0 : 0.0;
                mFeature[sampleIdx][22] = noun.length();
                mFeature[sampleIdx][23] = PUNCTUATION.contains(noun) ? 1.0 : 0.0;

                mLabel[sampleIdx] = 0;
                sampleIdx += 1;
            }
        }

        LogUtil.info("build feature completed!");

        try {
            FileWriter fw = new FileWriter(outputFile);
            int idx = 0;
            for (double[] feat : mFeature) {
                for (double value : feat) {
                    fw.write(value + " ");
                }

                if (mNegativeSample != null) {
                    fw.write(mLabel[idx] + " " + outWord.get(idx) + "\n");
                } else {
                    fw.write(outWord.get(idx) + "\n");
                }
                idx += 1;
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
