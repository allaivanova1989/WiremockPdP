import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;

public class WireMockTest {
    WireMockServer wireMockServer;

    @BeforeEach
    public void setup () {
        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = 8089;
        setupStub();

    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop();
    }

    public void setupStub() {
        configureFor("127.0.0.1", 8089);
        stubFor(get(urlEqualTo("/unsuccessful/endpoint"))
                .withHeader("Accept", matching("text/plain"))
                .willReturn(aResponse().
                        withStatus(503).
                        withHeader("Content-Type", "text/html").
                        withBody("Service Not Available"))
        );

        stubFor(get(urlEqualTo("/successful/endpoint"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse().
                        withStatus(200).
                        withHeader("Content-Type", "application/json")
                        .withBody("{\"serviceStatus\": \"running\"}")
                        .withFixedDelay(2500))
        );

        stubFor(get(urlEqualTo("/body-file"))
                .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                        .withStatus(200)
                        .withBodyFile("json/example.json"))
        );
    }

    @Test
    public void negativeTest() {
        Response r = given()
                .header(new Header("Accept", "text/plain"))
                .when()
                .get("/unsuccessful/endpoint");
        Assert.assertEquals(r.statusCode(), 503);
    }


    @Test
    public void successTest() {
        Response r = given()
                .header(new Header("Accept", "application/json"))
                .when()
                .get("/successful/endpoint");
        Assert.assertEquals(r.statusCode(), 200);
        Assert.assertEquals(r.jsonPath().getString("serviceStatus"), "running");
    }

    @Test
    public void notFoundTest() {
        Response r = given()
                .header(new Header("Accept", "application/json"))
                .when()
                .get("/not/found/this/endpoint");
        Assert.assertEquals(r.statusCode(), 404);
    }

    @Test
    public void testResponseContents() {
        Response r =  given()
                .when()
                .get("/body-file");
        String title = r.jsonPath().get("first_name");
        System.out.println(title);
        Assert.assertEquals("Sammy", title);
    }
}
