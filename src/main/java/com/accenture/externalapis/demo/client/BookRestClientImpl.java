package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);
            if (response == null) {
                throw new ClientException("Empty response from external service for id " + id);
            }
            return mapToDto(response);
        } catch (HttpClientErrorException e) {
            throw new ClientException("Book not found or bad request for id " + id, e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error for id " + id, e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unavailable (timeout) for id " + id, e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error from external service for id " + id, e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);
            if (responses == null) {
                throw new ClientException("Empty response when fetching all books");
            }
            return Arrays.stream(responses)
                    .map(this::mapToDto)
                    .toList();
        } catch (HttpClientErrorException e) {
            throw new ClientException("Bad request when fetching all books", e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error when fetching all books", e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unavailable (timeout) when fetching all books", e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error when fetching all books", e);
        }
    }

    private BookDto mapToDto(BookApiResponse response) {
        return new BookDto(response.title(), response.author(), response.genre(), response.price());
    }
}
