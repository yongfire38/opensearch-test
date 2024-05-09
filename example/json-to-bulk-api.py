# -*- coding: utf-8 -*-
"""
Created on Thu Apr 25 17:09:52 2024

@author: platformtech
"""

import json

def convert_to_bulk_format(json_data, index_name, doc_type):
    bulk_data = ""
    doc_id = 1
    for color_name, color_data in json_data.items():
        bulk_data += f'{{"index":{{"_index":"{index_name}","_id":"{doc_id}"}}}}\n'
        bulk_data += json.dumps(color_data) + '\n'
        doc_id += 1
    return bulk_data

def save_bulk_data(bulk_data, output_file):
    with open(output_file, 'w') as file:
        file.write(bulk_data)
        
def read_json_file(file_path):
    with open(file_path, 'rt', encoding='UTF8') as file:
        return json.load(file)

# 파일에서 JSON 데이터 읽기
input_file = 'colors.json'
json_data = read_json_file(input_file)

# 인덱스 이름과 문서 타입 지정
index_name = 'colors-knn'
doc_type = '_doc'

bulk_data = convert_to_bulk_format(json_data, index_name, doc_type)

# 결과를 파일로 저장
output_file = 'bulk_data.json'
save_bulk_data(bulk_data, output_file)

print(f"Bulk API 데이터를 {output_file}에 성공적으로 저장했습니다.")