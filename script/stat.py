novel_file = "/Users/lxbmain/IdeaProjects/cnws/data/novels/zx_origin.txt"
noun_file = "/Users/lxbmain/IdeaProjects/cnws/data/np/zx_noun.txt"

word2req = {}
for lines in open(novel_file, encoding="utf-8").readlines():
    arr = lines.strip().split()
    for pairs in arr:

        word = pairs.split("_")[0]
        if len(pairs.split("_")) == 2 and "nr" in pairs.split("_")[1]:
            if word in word2req:
                word2req[word] += 1
            else:
                word2req[word] = 1

word2freq = [(word, value) for word, value in word2req.items()]

nouns = set()
for lines in open(noun_file, encoding="utf-8").readlines():
    noun = lines.strip().split()[0]
    nouns.add(noun)

freq_nouns = sorted(word2freq, key=lambda x: x[1], reverse=True)[:100]
print(len(freq_nouns))
print(freq_nouns)

print("---------")
count = 0
for noun in nouns:
    if noun in set([words[0] for words in freq_nouns]):
        print(noun)
        count += 1
print(count)
