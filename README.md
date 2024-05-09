# opensearch-test
Amazon Open Search를 Spring Boot와 연동하여 일반 검색 및 벡터 검색을 수행

# Amazon Open Search
opensearch 공식 문서 download 항목에서 docker-compose.yml 받아서 도커로 띄우고 테스트한다.  
docker-compose.yml의 최초 비번은 변경하여야 한다.

[Amazon Open Search 다운로드 페이지](https://opensearch.org/downloads.html)

# 최초 설정
application.properties의 최초 값을 설정한다.  
opensearch.password : docker-compose.yml에서 최초 설정한 비번을 설정.  
opensearch.keystore : jdk폴더 내부 lib > security > cacerts를 설정. Amazon Open Search는 jdk 11 이상이어야 한다.  
opensearch.keystore.password : `keytool -importcert -alias opensearch -keystore ./cacerts -file root-ca.pem` 커맨드를 실행 시 설정했던 패스워드를 설정하여 준다.  
bulk.insert.json : json 파일을 파싱하여 bulk insert를 수행하고자 할 경우의 파일 경로를 명시하여 준다.  

# example 내부 파일
colors.json : 색상 rgb 값의 벡터 검색을 테스트하기 위한 기초 자료. 해당 파일을 파싱하여 bulk insert를 수행 가능하다. (bulk.insert.json 값 설정 필요)    
bulk_data.json : Open Search dashboard에 데이터를 벌크로 삽입하기 위해 수정된 파일.   
dashboard > Dev Tools에서  `POST /_bulk`를 입력하고 해당 파일의 내용을 그대로 복사하여 넣으면 벌크 삽입이 된다.  
json-to-bulk-api.py : json 파일을 벌크 api 형식으로 변환하기 위한 파이썬 스크립트  
