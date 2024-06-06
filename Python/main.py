import hashlib
import json
import os
import re
import time

import requests

count = 0
result = []
SHA256 = "0394d5d3e5890e646d44d53e5eb4c3f806fec01cc0f9e68a9c7a5e87cbc44dfc"
url = "https://hf-mirror.com/datasets/AdaptLLM/finance-tasks/resolve/main/Headline/test.json"
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
}


def checkFile():
    inputfile = os.path.isfile("test.json")
    if inputfile:
        print("开始校验原始数据")
        sha256 = hashlib.sha256()
        with open("test.json", 'rb') as file:
            while True:
                filedata = file.read(65536)
                if not filedata:
                    break
                sha256.update(filedata)
        if sha256.hexdigest() == SHA256:
            print("原始数据校验完成")
            return True
        else:
            print("原始数据校验失败，尝试重新下载")
            os.remove("test.json")
    else:
        print("原始数据丢失，尝试重新下载")
    return True if downloadFile() else False


def downloadFile():
    response = requests.get(url+"?download=true", headers, stream=True)
    if response.status_code == 200:
        with open('test.json', 'wb') as finput:
            for chunk in response.iter_content(chunk_size=1024):
                if chunk:
                    finput.write(chunk)
                    finput.flush()
    print("原始数据下载完成")
    return True if checkFile() else False


def getAnswer(text):
    global count, result
    tempText = text.split("\n\n")
    for item in tempText:
        lines = item.split('\n')
        last_line = lines[-1]
        question = item
        answer = ""
        if "No" in last_line.split() or "Yes" in last_line.split():
            if re.search(r"(Yes|No)\?? No", last_line):
                question = item.replace("- Yes No", "- Yes").replace("- No No", "- No").replace("Yes? No", "Yes?").replace("No? No", "No?")
                answer = "No"
            elif re.search(r"(Yes|No)\?? Yes", last_line):
                question = item.replace("- Yes Yes", "- Yes").replace("- No Yes", "- No").replace("Yes? Yes", "Yes?").replace("No? Yes", "No?")
                answer = "Yes"
            elif last_line.endswith("- Yes") or last_line.endswith("No or Yes?"):
                answer = ""
            elif "Yes" in last_line:
                question = item.replace("? Yes", "?")
                answer = "Yes"
            elif "No" in last_line:
                question = item.replace("? No", "?")
                answer = "No"
        result.append({"id": count, "Question": question, "Answer": answer})
        count += 1


if __name__ == '__main__':
    if checkFile():
        start_time = time.perf_counter_ns()
        print("数据清洗开始")
        out = open("out.json", "w", encoding="utf-8")
        with open("test.json", "rt", encoding="utf-8") as f:
            data = eval(f.read())
        for i in range(0, len(data)):
            getAnswer(data[i]["input"])
        print(f"数据清洗完成, 共耗时{(time.perf_counter_ns()-start_time)/1000000}ms, 共清洗{count}条数据")
        print(json.dumps(result, indent="\t"), file=out)
    else:
        print("运行失败")
