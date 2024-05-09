package egovframework.example.config;

import java.io.File;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Component
@Slf4j
public class OpenSearchClientConfig {

	@Value("${opensearch.protocol}")
    public String protocol;
    @Value("${opensearch.url}")
    public String url;
    @Value("${opensearch.port}")
    public int port;
    @Value("${opensearch.username}")
    public String username;
    @Value("${opensearch.password}")
    public String password;
    
    // HTTPS Settings
    @Value("${opensearch.keystore}")
    private String keystorePath;
    @Value("${opensearch.keystore.password}")
    private String keystorePassword;
    
    @Bean
    public OpenSearchClient openSearchClient() {
    	final HttpHost host = new HttpHost(protocol, url, port);
    	final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    	credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(username, password.toCharArray()));
    	
    	// HTTPS 통신을 위한 KeyStore 환경 설정
        KeyStore keyStore;
        // HTTPS 통신을 위한 SSLContext 환경 설정
        SSLContext sslContext;
        
        try {
            keyStore = KeyStore.getInstance(new File(keystorePath), keystorePassword.toCharArray());
            sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException("Failed to get KeyStore instance...");
        }
        
        // Apache HttpClient 5의 Transport를 사용하기 위한 Builder
        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(host);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            // SSL 설정
            final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(new NoopHostnameVerifier())
                    .build();
            // Pooling 설정
            final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setTlsStrategy(tlsStrategy)
                    .build();

            return httpClientBuilder
            		.setDefaultCredentialsProvider(credentialsProvider)
                    .setConnectionManager(connectionManager);
        });
        
        final OpenSearchTransport transport = builder.build();
        return new OpenSearchClient(transport);
    }
}
