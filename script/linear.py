import numpy as np
from sklearn import linear_model
from sklearn.neural_network import MLPClassifier
import pickle
import sys
import os


def train_wc(script_path):
    mix_sample = []
    for lines in open(script_path + "/../data/feature/wc_train_feature.txt", encoding="utf-8").readlines():
        feat = lines.strip().split()
        assert len(feat) == 26
        mix_sample.append(([float(value) for value in feat[:-2]], float(feat[-2])))
    np.random.shuffle(mix_sample)
    logreg = linear_model.LogisticRegression(max_iter=1000)
    print("wc train sample size" + str(len(mix_sample)))
    train_x = []
    train_y = []
    for i in range(165412):
        train_x.append(mix_sample[i][0])
        train_y.append(mix_sample[i][1])
    test_x = []
    test_y = []
    for i in range(20000):
        test_x.append(mix_sample[-i][0])
        test_y.append(mix_sample[-i][1])
    logreg.fit(train_x, train_y)
    pickle.dump(logreg, open(script_path + '/../data/model/wc.model', "wb"))
    pred_y = logreg.predict(train_x)
    count = 1
    for y_p, y_temp in zip(pred_y, train_y):
        if y_temp == 1 and y_p == 1:
            count += 1
    print("train result: ", logreg.score(train_x, train_y))
    print("p: " + str(count / sum(pred_y)) + " r: " + str(count / sum(train_y)))

    y = logreg.predict(test_x)
    count = 1
    for y_i, test_y_1 in zip(y, test_y):
        if y_i == test_y_1 == 1:
            count += 1
    print("test result:", logreg.score(test_x, test_y))
    print("p: " + str(count / sum(y)) + " r: " + str(count / sum(test_y)))


def train_pc(script_path):
    mix_sample = []
    for lines in open(script_path + "/../data/feature/pc_train_feature.txt", encoding="utf-8").readlines():
        feat = lines.strip().split()
        assert len(feat) == 10
        mix_sample.append(([float(value) for value in feat[:-2]], float(feat[-2])))
    np.random.shuffle(mix_sample)
    logreg = MLPClassifier(hidden_layer_sizes=(64, 16), activation="tanh")
    print("pc train sample size " + str(len(mix_sample)))
    train_x = []
    train_y = []
    for i in range(9528):
        train_x.append(mix_sample[i][0])
        train_y.append(mix_sample[i][1])
    test_x = []
    test_y = []
    for i in range(1000):
        test_x.append(mix_sample[-i][0])
        test_y.append(mix_sample[-i][1])
    logreg.fit(train_x, train_y)
    pickle.dump(logreg, open(script_path + '/../data/model/pc.model', "wb"))
    print("train result: ", logreg.score(train_x, train_y))
    print("test result:", logreg.score(test_x, test_y))


def inference_wc(script_path):
    result = []
    sample = []
    sample_word = []
    for lines in open(script_path + "/../data/feature/zx_feature.txt", encoding="utf-8").readlines():
        feat = lines.strip().split()
        if len(feat) != 25:
            continue
        assert len(feat) == 25
        sample.append([float(value) for value in feat[:-1]])
        sample_word.append(feat[-1].strip())
    logreg = pickle.load(open(script_path + '/../data/model/wc.model', 'rb'))
    test_x = []
    for i in range(len(sample)):
        test_x.append(sample[i])
    y = logreg.predict(test_x)
    for y_flag, word in zip(y, sample_word):
        if y_flag > 0.0:
            result.append(word.strip() + "\n")
    open(script_path + "/temp", 'w', encoding="utf-8").writelines(result)
    print(len(sample), sum(y))


def inference_pc(script_path):
    result = []
    sample = []
    sample_word = []
    for lines in open(script_path + "/../data/feature/zx_pc_feature.txt", encoding="utf-8").readlines():
        feat = lines.strip().split()
        if len(feat) != 10:
            continue
        assert len(feat) == 10
        sample.append([float(value) for value in feat[:-2]])
        sample_word.append(feat[-1])
    logreg = pickle.load(open(script_path + '/../data/model/pc.model', 'rb'))
    test_x = []
    for i in range(len(sample)):
        test_x.append(sample[i])
    y = logreg.predict(test_x)
    for y_flag, word in zip(y, sample_word):
        y_tag = "n"
        if y_flag == 0:
            y_tag = 'nr'
        elif y_flag == 1:
            y_tag = 'ns'
        elif y_flag == 2:
            y_tag = 'nt'
        result.append(word.strip() + " " + y_tag + "\n")
    open(script_path + "/temp_pc", 'w', encoding="utf-8").writelines(result)
    print(len(sample))


if __name__ == '__main__':

    script_path = sys.path[0]
    mode = sys.argv[1]
    if mode == "train_wc":
        train_wc(script_path)
    elif mode == 'train_pc':
        train_pc(script_path)
    elif mode == "inf_wc":
        inference_wc(script_path)
    elif mode == "inf_pc":
        inference_pc(script_path)
