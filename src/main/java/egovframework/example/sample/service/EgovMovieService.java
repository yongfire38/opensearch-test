/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package egovframework.example.sample.service;

import java.io.IOException;

import org.opensearch.client.opensearch.core.SearchResponse;

import egovframework.example.sample.index.Movie;

/**
 * @Class Name : EgovSampleService.java
 * @Description : EgovSampleService Class
 * @Modification Information
 * @
 * @  수정일      수정자              수정내용
 * @ ---------   ---------   -------------------------------
 * @ 2009.03.16           최초생성
 *
 * @author 개발프레임웍크 실행환경 개발팀
 * @since 2009. 03.16
 * @version 1.0
 * @see
 */
public interface EgovMovieService {

	public void createIndex(String indexName) throws IOException;
	
	public void insertData(String indexName, Movie movie, String id) throws IOException;
	
	public void updateData(String indexName, Movie movie, String id) throws IOException;
	
	public SearchResponse<Movie> search(String indexName) throws IOException;
	
	public void deleteData(String indexName, String id) throws IOException;
	
	public void deleteIndex(String indexName) throws IOException;

}
