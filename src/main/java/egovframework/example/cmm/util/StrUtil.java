package egovframework.example.cmm.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StrUtil {
	
	public static List<String> readWordsFromFile(String filePath) {
        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }
	
	public static String cleanString(String input) {
	    if (input == null) {
	        return null;
	    }
	    return input.replace("\r", " ")
	                .replace("\n", " ")
	                .replace("\\", " ")
	                .replace("&lt;", "<")
	                .replace("&gt;", ">")
	                .replace("&amp;", "&")
	                .replace("&quot;", "\"")
	                .replace("&nbsp;", " ")
	                .replace("&#39;", "'")
	                .replace("&#34;", "\"");
	}

}
