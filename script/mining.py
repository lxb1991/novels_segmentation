

def build_news_noun():
    dev = "/Users/lxbmain/IdeaProjects/cnws/data/novels/zx_300_dev.txt"
    test = "/Users/lxbmain/IdeaProjects/cnws/data/novels/zx.segged.txt"
    train = "/Users/lxbmain/IdeaProjects/cnws/data/pfr/train.txt"

    noun_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/noun.txt"
    pattern_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/pattern.txt"

    pun = set()
    with open(dev, encoding="utf-8") as fi:
        for line in fi.readlines():
            if line.strip():
                sentence = line.strip().split()
                for pairs in sentence:
                    word = pairs.strip().split("_")[0]
                    pos = pairs.strip().split("_")[1]
                    if "w" == pos:
                        pun.add(word)
    print(pun)

    noun = {}
    patterns = set()
    with open(dev, encoding="utf-8") as fi:
        for line in fi.readlines():
            if line.strip():
                sentence = line.strip().split()
                for i in range(len(sentence)):
                    b_pairs = sentence[i - 1] if i - 1 > 0 else "#_#"
                    a_pairs = sentence[i + 1] if i + 1 < len(sentence) else "#_#"
                    pairs = sentence[i]
                    word = pairs.strip().split("_")[0]
                    pos = pairs.strip().split("_")[1]
                    if "n" in pos:
                        pattern = b_pairs.strip().split("_")[0] + "_" + a_pairs.strip().split("_")[0]
                        patterns.add(pattern)
                        if word in noun:
                            noun[word].add(pattern)
                        else:
                            n2pattern = set()
                            n2pattern.add(pattern)
                            noun[word] = n2pattern

    print(len(noun), len(patterns))

    with open(test, encoding="utf-8") as fi:
        for line in fi.readlines():
            if line.strip():
                sentence = line.strip().split()
                for i in range(len(sentence)):
                    b_pairs = sentence[i - 1] if i - 1 > 0 else "#_#"
                    a_pairs = sentence[i + 1] if i + 1 < len(sentence) else "#_#"
                    pairs = sentence[i]
                    word = pairs.strip().split("_")[0]
                    pos = pairs.strip().split("_")[1]
                    if "n" in pos:
                        pattern = b_pairs.strip().split("_")[0] + "_" + a_pairs.strip().split("_")[0]
                        patterns.add(pattern)
                        if word in noun:
                            noun[word].add(pattern)
                        else:
                            n2pattern = set()
                            n2pattern.add(pattern)
                            noun[word] = n2pattern

    print(len(noun), len(patterns))

    with open(noun_file, 'w', encoding="utf-8") as fo:
        for n, p in noun.items():
            line = list()
            line.append(n)
            for p_item in p:
                line.append(p_item)
            fo.writelines(" ".join(line) + "\n")

    with open(pattern_file, "w", encoding="utf-8") as fo:
        for p in patterns:
            fo.writelines(p + "\n")

def compare():

    qiu_noun_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/noun.txt"
    li_noun_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/tmp_noun.txt"
    noun_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/noun.txt.bak"

    qiu_nouns = set()
    li_nouns = set()
    nouns = set()
    with open(qiu_noun_file, encoding="utf-8") as fi:
        for pairs in fi.readlines():
            word = pairs.strip().split()[0]
            qiu_nouns.add(word.strip())
    with open(li_noun_file, encoding="utf-8") as fi:
        for word in fi.readlines():
            li_nouns.add(word.strip())
    with open(noun_file, encoding="utf-8") as fi:
        for pairs in fi.readlines():
            word = pairs.strip().split()[0]
            nouns.add(word.strip())
    print(len(qiu_nouns))
    print(len(li_nouns))
    print(len(nouns))

    print(len(qiu_nouns & nouns))
    print(len(li_nouns & nouns))


def mining_puncuation():
    dev = "/Users/lxbmain/IdeaProjects/cnws/data/novels/zx_300_dev.txt"
    test = "/Users/lxbmain/IdeaProjects/cnws/data/novels/zx.segged.txt"
    train = "/Users/lxbmain/IdeaProjects/cnws/data/pfr/train.txt"

    files = [dev, test, train]
    puncuation = set()
    for file in files:
        for sentence in open(file, encoding="utf-8").readlines():
            for pairs in sentence.strip().split():
                word = pairs.strip().split("_")[0]
                pos = pairs.strip().split("_")[1]
                if pos == "w":
                    puncuation.add(word)
    print(puncuation)


if __name__ == '__main__':
    compare()
    # mining_puncuation()
