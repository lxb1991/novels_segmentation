package com.cnws.base.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnhanceSentence extends Sentence{

    public EnhanceSentence() {
        super();
    }

    public EnhanceSentence(List<String> words, List<String> pos, List<String> feature) {
        super(words, pos, feature);
    }

    public void genFeatureAppend(String word, String pos) {
        String wordLast = word.substring(word.length() - 1);
        String wordPreLast = word.substring(word.length() - 2, word.length() - 1);
        mFeatures.add("f_a7: " + word.substring(word.length() - 2, word.length() - 2) + "_" +
                word.substring(word.length() - 1));
        mFeatures.add("f_sa25: " + "_" + pos + "_" + wordLast);
        mFeatures.add("f_a26: " + "_" + wordLast + "_" + pos + "_" + word.substring(0, 1));
        mFeatures.add("f_a31: " + "_" + wordLast + "_" + pos + "_" + wordPreLast);
    }

    public void genFeatureSeperate(String word, String pos, String pWord, String pPOS, String ppWord, String ppPOS,
                                   Map<String, String> pos2hash, Map<String, Set<String>> newNoun,
                                   Set<String> newPattern, Map<String, String> noun2POS , String characters, int index,
                                   int maxIndex) {
        // 1
        mFeatures.add("f_s0: " + (mPuncuation.contains(word) ? "sep" : "app"));
        mFeatures.add("f_s1: " + pWord);
        mFeatures.add("f_s2: " + ppWord + "_" + pWord);
        if (pWord.length() == 1) {
            mFeatures.add("f_s3: " + pWord);
        }
        mFeatures.add("f_s4: " + pWord.substring(0, 1) + "_" + pWord.length());
        mFeatures.add("f_s5: " + pWord.substring(pWord.length() - 1) + "_" + pWord.length());
        mFeatures.add("f_s6: " + pWord.substring(pWord.length() - 1) + "_" + word);
        mFeatures.add("f_s8: " + pWord.substring(0, 1) + "_" + pWord.substring(pWord.length() - 1));
        mFeatures.add("f_s9: " + pWord + "_" + word);
        mFeatures.add("f_s10: " + ppWord.substring(ppWord.length() - 1) + "_" + pWord);
        mFeatures.add("f_s11: " + pWord.substring(0, 1) + "_" + word);
        mFeatures.add("f_s12: " + ppWord.substring(ppWord.length() - 1) + "_" + pWord.substring(pWord.length() - 1));
        mFeatures.add("f_s13: " + ppWord + "_" + pWord.length());
        mFeatures.add("f_s14: " + ppWord.length() + "_" + pWord);
        // pos tag info features
        mFeatures.add("f_s15: " + pWord + "_" + pPOS);
        mFeatures.add("f_s16: " + pPOS + "_" + pos);
        mFeatures.add("f_s17: " + ppPOS + "_" + pPOS + "_" + pos);
        mFeatures.add("f_s18: " + pWord + "_" + pos);
        mFeatures.add("f_s19: " + ppPOS + "_" + pWord);
        mFeatures.add("f_s20: " + pWord + "_" + pPOS + "_" + ppWord.substring(ppWord.length() - 1));
        mFeatures.add("f_s21: " + pWord + "_" + pPOS + "_" + word);
        if (pWord.length() == 1) {
            mFeatures.add("f_s22: " + ppWord.substring(ppWord.length() - 1) + "_" + pWord + "_" + word + "_" + pPOS);
        }
        mFeatures.add("f_s23: " + word + "_" + pos);
        mFeatures.add("f_s24: " + pPOS + "_" + pWord.substring(0, 1));
        mFeatures.add("f_sa25: " + pos + "_" + word);
        for (int i = 0; i < pWord.length() - 1; i++) {
            mFeatures.add("f_s27: " + pWord.substring(i, i + 1) + "_" + pPOS + "_" + pWord.substring(pWord.length() - 1));
        }
        mFeatures.add("f_s28: " + word + "_" + pos + "_" + pos2hash.get(word));
        for (int i = 0; i < pWord.length() - 1; i++) {
            mFeatures.add("f_s29: " + pWord.substring(i, i + 1) + "_" + pPOS + "_" +
                    pos2hash.get(pWord.substring(pWord.length() - 1)));
        }
        mFeatures.add("f_s30: " + word + "_" + pos + "_" + pWord.substring(pWord.length() - 1) + "_" + pPOS);
        mFeatures.add("f_s32: " + ppWord + "_" + pPOS + "_" + pos);
        mFeatures.add("f_s33: " + ppPOS + "_" + pWord + "_" + pos);
        mFeatures.add("f_s34: " + ppPOS + "_" + word + "_" + pos);
        mFeatures.add("f_s35: " + ppPOS + "_" + pPOS + "_" + word + "_" + pos);
        mFeatures.add("f_s36: " + pPOS + "_" + pWord.length() + "_" + pos);
        mFeatures.add("f_s37: " + ppPOS + "_" + pPOS + "_" + pWord.length());
        mFeatures.add("f_s38: " + ppPOS + "_" + pPOS + "_" + pWord.length() + "_" + pos);

        if (newNoun.containsKey(pWord)) {
            mFeatures.add("f_s39: " + noun2POS.get(pWord) + "_" + pWord.length());
        }
        if (newNoun.containsKey(ppWord)) {
            mFeatures.add("f_s40: " + noun2POS.get(ppWord) + "_" + ppWord.length());
        }

        for (int l=1; index + l < characters.length() && l <= maxIndex; l ++){
            String tempWord = characters.substring(index, index + l);

            if (newNoun.containsKey(tempWord)){
                mFeatures.add("f_s41: " + noun2POS.get(tempWord) + "_" + tempWord.length());
            }
            if (newNoun.containsKey(pWord) && newNoun.get(pWord).contains(ppWord +"_"+ tempWord)){
                mFeatures.add("f_s42: " + noun2POS.get(pWord) + "_" + pWord.length());
            }
            if (newPattern.contains(ppWord +"_"+ tempWord)){
                mFeatures.add("f_s43: " + pPOS + "_" + pWord.length());
            }
        }
    }

    public static Sentence genGoldenFeature(Sentence golden, int wordLen, Map<String, String> pos2hash,
                                            Map<String, Set<String>> newNoun, Set<String> newPattern,
                                            Map<String, String> noun2POS ,Map<String, Integer> pos2len) {
        EnhanceSentence goldenSentence = new EnhanceSentence();
        int nowLen = 0;
        String ppWord = "#";
        String ppPOS = "#";
        String pWord = "#";
        String pPOS = "#";

        for (int j = 0; j < golden.size(); j++) {
            String word = golden.getWordByIdx(j);
            String pos = golden.getPOSByIdx(j);

            goldenSentence.addWord(word.substring(0, 1));
            goldenSentence.addPOS(pos);
            goldenSentence.genFeatureSeperate(word.substring(0, 1), pos, pWord, pPOS, ppWord, ppPOS, pos2hash,
                    newNoun, newPattern, noun2POS, golden.getRawWords(), nowLen, pos2len.get(pos));

            nowLen += 1;
            if (nowLen == wordLen) {
                return goldenSentence;
            }
            for (int i = 1; i < word.length(); i++) {
                goldenSentence.updateLastWord(word.substring(i, i + 1));
                goldenSentence.genFeatureAppend(word.substring(0, i + 1), pos);
                nowLen += 1;
                if (nowLen == wordLen) {
                    return goldenSentence;
                }
            }
            ppWord = pWord;
            ppPOS = pPOS;
            pWord = word;
            pPOS = pos;
        }
        return goldenSentence;
    }
}
