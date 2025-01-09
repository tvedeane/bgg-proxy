package dev.tvedeane;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Requires(classes = {InvalidIdsStringException.class, ExceptionHandler.class})
public class InvalidIdsStringExceptionHandler
    implements ExceptionHandler<InvalidIdsStringException, HttpResponse<String>> {

    @Override
    public HttpResponse<String> handle(HttpRequest request, InvalidIdsStringException exception) {
        return HttpResponse.badRequest("Integers separated by commas expected");
    }
}
