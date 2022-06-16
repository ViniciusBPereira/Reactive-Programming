package group;

import org.junit.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/* 
 * Reactive Sreams
 * 1. Asynchronus
 * 2. Non-blocking
 * 3. Backpressure
 * Publisher <- (subscribe) Subscriber
 * Subscription is created
 * Publisher (onSubscribe with subscription) -> Subscriber
 * Subscription <- (request N) Subscriber
 * Publisher -> (onNext) Subscriber
 * until:
 * 1. Publisher sends all the objects request
 * 2. Publisher sends all the objects it has. (onComplete) subscriber and subscription will be canceled
 * 3. There is an error. (onError) -> subscriber and subscription will be canceled
 */
public class MonoTest {

    /*
     * mono.subscribe(parameter 1, parameter 2, parameter 3, parameter 4);
     * parameter 1 -> Value in Subscribe 
     * parameter 2 -> Exception Case
     * parameter 3 -> Finish Processing
     * parameter 4 -> Method Subscribe
     */

/*
    @Before
    public void setUp(){
        BlockHound.install();
    }

    @Test
    public void blockHoundWorks(){
        try {
            FutureTask<?> task = new FutureTask<>(() -> {Thread.sleep(0); return "";});
            Schedulers.parallel().schedule(task);
            task.get(10, TimeUnit.SECONDS);
            
            Assert.fail("should fail");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof BlockingOperationError);
        }
    }
*/

    @Test
    public void monoSubscriber() {
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe();

        //Test -> Expected
        StepVerifier.create(mono).expectNext(name).verifyComplete();
    }

    @Test
    public void monoSubscriberConsumer() {
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe(s -> System.out.println("Value in Subscribe: " + s));
        
        //Test -> Expected
        StepVerifier.create(mono).expectNext(name).verifyComplete();
    }

    @Test
    public void monoSubscriberConsumerError() {
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log().map(s -> {throw new RuntimeException("Testing mono with error");});

        mono.subscribe((s) -> System.out.println("Value in Subscribe: " + s), s -> System.out.println("Causing purposeful error"));
        
        //Test -> Expected
        StepVerifier.create(mono).expectError(RuntimeException.class).verify();
    }

    @Test
    public void monoSubscriberConsumerComplete() {
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log().map(String::toUpperCase);

        mono.subscribe(s -> System.out.println("Value in Subscribe: " + s),
                      Throwable::printStackTrace,
                      () -> System.out.println("FINISHED!"));
        
        //Test -> Expected
        StepVerifier.create(mono).expectNext(name.toUpperCase()).verifyComplete();
    }

    @Test
    public void monoSubscriberConsumerSubscription() {
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log().map(String::toUpperCase);

        mono.subscribe(s -> System.out.println("Value in Subscribe: " + s),
                      Throwable::printStackTrace,
                      () -> System.out.println("FINISHED!"),
                      t -> t.request(1));
        
        //Test -> Expected
        StepVerifier.create(mono).expectNext(name.toUpperCase()).verifyComplete();
    }

    @Test
    public void monoDoOneMethods() {
        //Publisher
        String name = "Vinicius Begosso";
        Mono<String> mono = Mono.just(name).log().map(String::toUpperCase)
                                .doOnSubscribe(s -> System.out.println("Subscribed"))
                                .doOnRequest(l -> System.out.println("Request Received: " + l))
                                .doOnNext(s -> System.out.println("Value in Publisher: " + s))
                                .doOnSuccess(s -> System.out.println("Success!"));

        //Subscribe
        mono.subscribe(s -> System.out.println("Value in Subscribe: " + s),
                      Throwable::printStackTrace,
                      () -> System.out.println("Finished Subscribe!"),
                      t -> t.request(1));
        
    }

    @Test
    public void monoDoOnError(){
        Mono<Object> error = Mono.error(new IllegalArgumentException("Error"))
                                 .doOnError(t -> System.out.println(t.getMessage()))
                                 .doOnNext(s -> System.out.println("Executing doOnNext"))
                                 .log();

        StepVerifier.create(error).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    public void monoOnErrorResume(){
        String name = "Vinicius Begosso";
        Mono<Object> error = Mono.error(new IllegalArgumentException("Error"))
                                 .doOnError(t -> System.out.println(t.getMessage()))
                                 .onErrorResume(t -> {
                                                        System.out.println("Executing doOnNext");
                                                        return Mono.just(name);
                                                     })
                                 .log();

        StepVerifier.create(error).expectNext(name).verifyComplete();
    }

}
