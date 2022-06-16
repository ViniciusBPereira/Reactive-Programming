package group;

import java.time.Duration;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscription;

import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class FluxTest {
    
    @Test
    public void fluxSubscriber(){
        String name = "Vinicius Begosso";
        String age = "19";
        String telephone = "15996891423";
        Flux<String> flux = Flux.just(name, age, telephone).log();

        //Test -> Expected    
        StepVerifier.create(flux).expectNext(name, age, telephone).verifyComplete();
    }

    @Test
    public void fluxSubscriberNumber(){
        Flux<Integer> flux = Flux.range(1,5).log();
    
        flux.subscribe();

        //Test -> Expected
        StepVerifier.create(flux).expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void fluxSubscriberFromList(){
        Flux<Integer> flux = Flux.fromIterable(List.of(1,2,3,4,5)).log();
    
        flux.subscribe();

        //Test -> Expected
        StepVerifier.create(flux).expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersError(){
        Flux<Integer> flux = Flux.range(1, 5).log()
                                 .map(i -> {
                                    if(i == 4){
                                        throw new IndexOutOfBoundsException("index error");
                                    }
                                    return i;
                                });
    
                                
        flux.subscribe(null,
                       Throwable::printStackTrace,
                       () -> System.out.println("FINISHED!"),
                       t -> t.request(3));

    

        //Test -> Expected
        //StepVerifier.create(flux).expectNext(1,2,3,4,5).verifyComplete();
    }



    @Test
    public void fluxSubscriberNumbersBackpressure(){
        Flux<Integer> flux = Flux.range(1, 10).log();
        
        flux.subscribe(new ImpSubscriber(2));

        //Test -> Expected
        //StepVerifier.create(flux).expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void fluxSubscriberBackpressure(){
        Flux<Integer> flux = Flux.range(1, 10).log().limitRate(3);
        
        flux.subscribe();

        //Test -> Expected
        //StepVerifier.create(flux).expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void fluxSubscriberIntervalOne() throws Exception{
        Flux<Long> interval = Flux.interval(Duration.ofMillis(100))
                                  .take(2)
                                  .log();
    
        interval.subscribe(i -> System.out.println(i));

        Thread.sleep(600);
    }

    @Test
    public void fluxSubscriberIntervalTwo() throws Exception{
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofDays(1))
                                                                 .take(10)
                                                                 .log())
                    .expectSubscription()
                    .expectNoEvent(Duration.ofHours(24))
                    .thenAwait(Duration.ofDays(2))
                    .expectNext(0l)
                    .expectNext(1l)
                    .thenCancel()
                    .verify();
    }

    @Test
    public void connectableFlux() throws InterruptedException{
        ConnectableFlux<Integer> publisher = Flux.range(1, 10)
                                                 .log()
                                                 .delayElements(Duration.ofMillis(100))
                                                 .publish();


    
        //Test -> Expected
        StepVerifier.create(publisher).then(publisher::connect).expectNext(1,2,3,4,5,6,7,8,9,10).expectComplete().verify();
    }

    @Test
    public void connectableFluxAutoConnect() throws InterruptedException{
        Flux<Integer> fluxAutoConnect = Flux.range(1, 5)
                                                 .log()
                                                 .delayElements(Duration.ofMillis(100))
                                                 .publish()
                                                 .autoConnect(2);


    
        //Test -> Expected
        StepVerifier.create(fluxAutoConnect).then(fluxAutoConnect::subscribe).expectNext(1,2,3,4,5).expectComplete().verify();
    }

}






class ImpSubscriber extends BaseSubscriber<Integer>{
    
    private int count = 0;
    private int requestCount;

    public ImpSubscriber(int requestCount){
        this.requestCount = requestCount;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(requestCount);
    }
    
    @Override
    protected void hookOnNext(Integer value) {
        count++;
        if(count >= requestCount){
            count = 0;
            request(requestCount);
        }
    }

}
