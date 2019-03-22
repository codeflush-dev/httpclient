package dev.codeflush.httpclient.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dev.codeflush.httpclient.Endpoint;
import dev.codeflush.httpclient.Response;
import dev.codeflush.httpclient.parser.NoOpResponseParser;
import dev.codeflush.httpclient.request.body.RequestBody;
import dev.codeflush.httpclient.parser.ResponseParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class SimpleHTTPClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort(), false);

    private Endpoint baseEndpoint;
    private HTTPClient client;
    private ResponseParser<Void> parser;

    @Before
    public void setupMockServer() {
        this.baseEndpoint = Endpoint.forHostAndPort(Endpoint.HTTP, "localhost", this.wireMockRule.port());
        this.client = new SimpleHTTPClient();
        this.parser = new NoOpResponseParser();

        this.wireMockRule.resetAll();
    }

    @Test
    public void simpleHeadRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .head()
                .execute(this.client, this.parser);

        verify(exactly(1), headRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void simpleGetRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .get()
                .execute(this.client, this.parser);

        verify(exactly(1), getRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void simplePostRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .post()
                .execute(this.client, this.parser);

        verify(exactly(1), postRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void simplePutRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .put()
                .execute(this.client, this.parser);

        verify(exactly(1), putRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void simplePatchRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .patch()
                .execute(this.client, this.parser);

        verify(exactly(1), patchRequestedFor(urlEqualTo("/test")));
    }


    @Test
    public void simpleDeleteRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .delete()
                .execute(this.client, this.parser);

        verify(exactly(1), deleteRequestedFor(urlEqualTo("/test")));
    }


    @Test
    public void requestWithBody() throws Exception {
        // I dont want to test the RequestBody-Implementations here
        // I only want to test if the SimpleHTTPClient sends the implemented RequestBody correctly
        RequestBody body = new RequestBody() {
            @Override
            public String getContentType() {
                return "my-content-type";
            }

            @Override
            public void write(OutputStream out) throws IOException {
                out.write(new byte[]{-128, 12, 0, 69, 125});
            }
        };

        this.baseEndpoint.resolve("test")
                .post()
                .body(body)
                .execute(this.client, this.parser);

        verify(exactly(1), postRequestedFor(urlEqualTo("/test"))
                .withHeader("Content-Type", equalTo("my-content-type"))
                .withRequestBody(binaryEqualTo(new byte[]{-128, 12, 0, 69, 125})));
    }

    @Test
    public void requestHeadersOnRequest() throws Exception {
        this.baseEndpoint.resolve("test")
                .get()
                .header("some-header", "some-value")
                .execute(this.client, this.parser);

        verify(exactly(1), getRequestedFor(urlEqualTo("/test"))
                .withHeader("some-header", equalTo("some-value")));
    }

    @Test
    public void requestHeadersOnClient() throws Exception {
        HTTPClient client = new SimpleHTTPClient(Collections.singletonMap("some-header", "some-value"));

        this.baseEndpoint.resolve("test")
                .get()
                .execute(client, this.parser);

        verify(exactly(1), getRequestedFor(urlEqualTo("/test"))
                .withHeader("some-header", equalTo("some-value")));
    }

    @Test
    public void requestHeadersOnRequestOverrideClientHeaders() throws Exception {
        HTTPClient client = new SimpleHTTPClient(Collections.singletonMap("some-header", "some-value"));

        this.baseEndpoint.resolve("test")
                .get()
                .header("some-header", "another-value")
                .execute(client, this.parser);

        verify(exactly(1), getRequestedFor(urlEqualTo("/test"))
                .withHeader("some-header", equalTo("another-value")));
    }

    @Test
    public void responseCode() throws Exception {
        stubFor(get(urlEqualTo("/test")).willReturn(aResponse().withStatus(210)));

        Response<Void> response = this.baseEndpoint.resolve("test")
                .get()
                .execute(this.client, this.parser);

        assertEquals(210, response.getResponseCode());

        verify(exactly(1), getRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void responseHeaders() throws Exception {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(
                        aResponse()
                                .withHeader("some-header", "some-value")
                                .withHeader("another-header", "value0", "value1", "value2")
                )
        );

        Response<Void> response = this.baseEndpoint.resolve("test")
                .get()
                .execute(this.client, this.parser);

        Map<String, List<String>> headers = response.getHeaders();

        assertNotNull(headers);
        assertNotNull(headers.get("some-header"));
        assertEquals(1, headers.get("some-header").size());
        assertEquals("some-value", headers.get("some-header").get(0));
        assertNotNull(headers.get("another-header"));
        assertEquals(3, headers.get("another-header").size());
        assertEquals("value2", headers.get("another-header").get(0));
        assertEquals("value1", headers.get("another-header").get(1));
        assertEquals("value0", headers.get("another-header").get(2));

        verify(exactly(1), getRequestedFor(urlEqualTo("/test")));
    }

    @Test
    public void parseContentType1() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain");
        assertArrayEquals(new String[]{"text/plain", null}, result);
    }

    @Test
    public void parseContentType2() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; charset=UTF-8");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType3() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; charset=\"UTF-8\"");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType4() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; charset=UTF-8; some more values");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType5() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; charset=\"UTF-8\"; some more values");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType6() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; some more values; charset=UTF-8");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType7() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; some more values; charset=\"UTF-8\"");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType8() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; some more values; charset=UTF-8; some more values");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType9() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; some more values; charset=\"UTF-8\"; some more values");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }

    @Test
    public void parseContentType10() {
        String[] result = SimpleHTTPClient.parseContentType("text/plain; key=value; charset=\"UTF-8\"; some more values");
        assertArrayEquals(new String[]{"text/plain", "UTF-8"}, result);
    }
}