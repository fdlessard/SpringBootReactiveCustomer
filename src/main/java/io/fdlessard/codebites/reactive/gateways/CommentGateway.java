package io.fdlessard.codebites.reactive.gateways;

import io.fdlessard.codebites.reactive.domain.Comment;
import io.fdlessard.codebites.reactive.domain.ErrorResponse;
import io.fdlessard.codebites.reactive.domain.Response;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.fdlessard.codebites.reactive.configurations.ReactiveConfiguration.buildIds;

@Service
public class CommentGateway {


    // https://www.callicoder.com/spring-5-reactive-webclient-webtestclient-examples/

    private WebClient webClient;

    public CommentGateway(WebClient webClient) {
        this.webClient = webClient;
    }

    public Comment getComment(@PathVariable int id) {

        return webClient.get()
                .uri("/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Comment.class)
                .block();
    }

    public List<Response<Comment, ErrorResponse>> getAllComments() {

        List<String> ids = buildIds();
        ids.add(10, "toto");

        return Flux.fromIterable(ids)
                .flatMap(id -> webClient.get()
                        .uri("/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(new GatewayException()))
                        .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(new GatewayException()))
                        .bodyToMono(Comment.class)
                        .flatMap(c -> buildResponse(c))
                        .onErrorResume(e -> buildResponse(e)), 256)
                .collectList().block();
    }

    private Mono<Response<Comment, ErrorResponse>> buildResponse(Comment comment) {
        return Mono.just(new Response<Comment, ErrorResponse>(HttpStatus.OK, comment, null));
    }

    private Mono<Response<Comment, ErrorResponse>> buildResponse(Throwable e) {
        return Mono.just(new Response<Comment, ErrorResponse>(HttpStatus.BAD_REQUEST, null, new ErrorResponse()));
    }

}
