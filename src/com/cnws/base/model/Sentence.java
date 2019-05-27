package com.cnws.base.model;


import java.util.*;

public class Sentence {

    public static final String[] PUNCUATION = {"∶", "（", "？", "××", "」", "〉", "⑤", "⑦",
            "…………", "⑵", "③", "“", "┅", "～", "％", "［", "＊", "④", "『", "———", "〈", "②", "》",
            "；", "，", "＞", "］", "──", "——", "∶", "’", "－", "‘", "……", "「", "●", "△", "⑥", "?",
            "”", "±％", "①", "·", "、", "」", "▲", "《", "＝", "』", "※", "：", "—", "／", "×",
            "〓", "！", "。", "‰", "）"};
    public static Set<String> mPuncuation = new HashSet<String>(Arrays.asList(PUNCUATION));

    List<String> mWords;
    List<String> mPOS;
    boolean GOLDEN = false;
    List<String> mFeatures;
    double mScore = 0.0;

    public Sentence() {
        mWords = new ArrayList<String>();
        mPOS = new ArrayList<String>();
        mFeatures = new ArrayList<String>();
    }

    public Sentence(List<String> words, List<String> pos, List<String> feature) {
        mFeatures = new ArrayList<String>(feature);
        mWords = new ArrayList<String>(words);
        mPOS = new ArrayList<String>(pos);
    }

    public Sentence(List<String> words, List<String> pos, boolean golden) {
        GOLDEN = golden;
        mWords = words;
        mPOS = pos;
    }

    public int size() {
        return mWords.size();
    }

    public List<String> getFeature() {
        return mFeatures;
    }

    public void setScore(double score) {
        mScore = score;
    }

    public double getScore() {
        return mScore;
    }

    public String getRawWords() {
        StringBuilder rawString = new StringBuilder();
        for (String word : mWords) {
            rawString.append(word);
        }
        return rawString.toString();
    }

    public String getSegWords() {

        StringBuilder segString = new StringBuilder();
        for (String word : mWords) {
            segString.append(word);
            segString.append(" ");
        }
        return segString.toString();
    }

    public List<String> getWords() {
        return mWords;
    }

    public List<String> getPOS() {
        return mPOS;
    }

    public void addWord(String word) {
        if (!GOLDEN) {
            mWords.add(word);
        } else {
            throw new Error("golden sample can't add item");
        }
    }

    public void addPOS(String pos) {
        if (!GOLDEN) {
            mPOS.add(pos);
        } else {
            throw new Error("golden sample can't add item");
        }
    }

    public String getWordByIdx(int idx) {
        return mWords.get(idx);
    }

    public String getPOSByIdx(int idx) {
        return mPOS.get(idx);
    }


    public String getLastWord() {
        return mWords.get(mWords.size() - 1);
    }

    public String getLastPOS() {
        return mPOS.get(mPOS.size() - 1);
    }

    public String getLastPreWord() {
        return mWords.get(mWords.size() - 2);
    }

    public String getLastPrePOS() {
        return mPOS.get(mPOS.size() - 2);
    }

    public void updateLastWord(String word) {
        String lastWord = getLastWord();
        mWords.remove(mWords.size() - 1);
        mWords.add(lastWord + word);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentence) {
            Sentence targetSentence = (Sentence) obj;
            for (int i = 0; i < size(); i++) {
                if (!mWords.get(i).equals(targetSentence.getWordByIdx(i))) {
                    return false;
                }
                if (!mPOS.get(i).equals(targetSentence.getPOSByIdx(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
