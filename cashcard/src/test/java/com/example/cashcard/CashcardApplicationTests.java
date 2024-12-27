package com.example.cashcard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CashcardApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnACashCardWhenDataIsSaved() {
        var response = restTemplate.getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("@.id");
        assertThat(id).isEqualTo(99);
        Double amount = documentContext.read("@.amount");
        assertThat(amount).isEqualTo(123.45);
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequested() {
        var response = restTemplate.getForEntity("/cashcards", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() {
        var response = restTemplate.getForEntity("/cashcards/1000", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
    void shouldCreateANewCashCard() {
        var newCashCard = new CashCard(null, 250.0);
        var createResponse = restTemplate.postForEntity("/cashcards", newCashCard, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var locationURIofNewCard = createResponse.getHeaders().getLocation();
        var getResponse = restTemplate.getForEntity(locationURIofNewCard, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("@.id");
        assertThat(id).isNotNull().isInstanceOf(Number.class);
        Double amount = documentContext.read("@.amount");
        assertThat(amount).isEqualTo(250.00);
    }

    @Test
    void contextLoads() {
    }

}
