package org.eduami.spring;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreetingController {
    private String cache = null;

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/greeting")
    @RateLimiter(name = "greetingRateLimit", fallbackMethod = "greetingFallBack")
    @CircuitBreaker(name = "greetingCircuit", fallbackMethod = "greetingFallBack")
    public ResponseEntity greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        ResponseEntity responseEntity = restTemplate.getForEntity("http://localhost:8081/serviceBgreeting?name=" + name, String.class);
        //update cache
        cache = responseEntity.getBody().toString();
        return responseEntity;
    }

    //Invoked when circuit is in open state
    public ResponseEntity greetingFallBack(String name, io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
        System.out.println("Circuit is in open state no further calls are accepted");
        //return data from cache
        return ResponseEntity.ok().body(cache);
    }

    //Invoked when call to serviceB failed
    public ResponseEntity greetingFallBack(String name, HttpServerErrorException ex) {
        System.out.println("Exception occurred when call calling service B");
        //return data from cache
        return ResponseEntity.ok().body(cache);
    }

    public ResponseEntity greetingFallBack(String name, io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
        System.out.println("Rate limit applied no further calls are accepted");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Retry-After", "1"); //retry after one second

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(responseHeaders) //send retry header
                .body("Too many request - No further calls are accepted");
    }


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


}