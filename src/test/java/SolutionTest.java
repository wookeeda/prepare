import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SolutionTest {

    Solution sol;

    @Before
    public void setUp() throws IOException {
        sol = new Solution();
    }

    @Ignore
    @Test
    public void 기본테스트() throws IOException {
        // given
        CloseableHttpClient client = HttpClientBuilder.create().build();

        // when
        CloseableHttpResponse res = client.execute(new HttpGet("https://nghttp2.org/httpbin/get"));
        String body = EntityUtils.toString(res.getEntity());
        JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();

        //then
        assertThat(jsonObject.get("url").toString(), containsString("https://nghttp2.org/httpbin/get"));
        assertEquals(jsonObject.get("url").toString(), "https://nghttp2.org/httpbin/get");
    }

    @Test
    public void startAPI_테스트() throws IOException {
        // expect
        assertTrue(sol.startApi());
        assertThat(sol.res.token, notNullValue());
    }

    @Test
    public void onCallsAPI_테스트() throws IOException {
        // given
        sol.startApi();

        // expect
        assertTrue(sol.onCalls());
        assertThat(sol.res.token, notNullValue());

    }

}