package com.example.cashcard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD) // * Use at
// method level
class CashcardApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldNotReturnACashCardWhenUsingBadCredentials() {
        var response = restTemplate
                .withBasicAuth("random-user", "random-password")
                .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        var response = restTemplate
                .withBasicAuth("tim-owns-no-cards", "def456")
                .getForEntity("/cashcards/99", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/102", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnACashCardWhenDataIsSaved() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("@.id");
        assertThat(id).isEqualTo(99);
        Double amount = documentContext.read("@.amount");
        assertThat(amount).isEqualTo(123.45);
        String owner = documentContext.read("@.owner");
        assertThat(owner).isEqualTo("sarah1");
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequested() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        int cashcardCount = documentContext.read("$.length()");
        assertThat(cashcardCount).isEqualTo(3);
        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);
        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00);
        JSONArray owners = documentContext.read("$..owner");
        assertThat(owners).containsExactlyInAnyOrder("sarah1", "sarah1", "sarah1");
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/1000", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() {
        var newCashCard = new CashCard(null, 250.0, null); // Take owner from Principle
        var createResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .postForEntity("/cashcards", newCashCard, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var locationURIofNewCard = createResponse.getHeaders().getLocation();
        var getResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity(locationURIofNewCard, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("@.id");
        assertThat(id).isNotNull().isInstanceOf(Number.class);
        Double amount = documentContext.read("@.amount");
        assertThat(amount).isEqualTo(250.00);
        String owner = documentContext.read("@.owner");
        assertThat(owner).isEqualTo("sarah1");
    }

    @Test
    void shouldReturnAPageOfCashCards() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnASortedPageOfCashCards() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);
        double amount = documentContext.read("$[0].amount");
        assertThat(amount).isEqualTo(150.00);
    }

    @Test
    void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);
        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
    }

    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        var cashcardUpdate = new CashCard(null, 99.00, null);
        var request = new HttpEntity<CashCard>(cashcardUpdate);
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("@.id");
        Double amount = documentContext.read("@.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(99.00);
    }

    @Test
    void shouldNotUpdateACashCardThatDoesNotExist() {
        var unknownCard = new CashCard(null, 19.99, null);
        var request = new HttpEntity<>(unknownCard);
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/9999", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
        var kumarsCard = new CashCard(null, 19.99, null);
        var request = new HttpEntity<>(kumarsCard);
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCashCard() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    }

    @Test
    @DirtiesContext
    void shouldNotDeleteACashCardThatDoesNotExist() {
        var response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/9999", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
        var deleteResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var getResponse = restTemplate
                .withBasicAuth("kumar2", "ghi789")
                .getForEntity("/cashcards/102", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void contextLoads() {
    }

}
