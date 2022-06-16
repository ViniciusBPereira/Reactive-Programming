package group;

import static org.junit.Assert.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class OperatorsTest {

    @Test
    public void subscribeOnSimple(){
        Flux<Integer> flux = Flux.range(1, 5)
                                 .map(i -> {System.out.println("Map 1 -> " + i + " Thread " + Thread.currentThread().getName()); return i;})
                                 .subscribeOn(Schedulers.boundedElastic())
                                 .map(i -> {System.out.println("Map 2 -> " + i + " Thread " + Thread.currentThread().getName()); return i;});

        //Test -> Expected
        StepVerifier.create(flux).expectSubscription().expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void multPublishOnSimple(){
        Flux<Integer> flux = Flux.range(1, 5)
                                 .publishOn(Schedulers.single())
                                 .map(i -> {System.out.println("Map 1 -> " + i + " Thread " + Thread.currentThread().getName()); return i;})
                                 .publishOn(Schedulers.boundedElastic())
                                 .map(i -> {System.out.println("Map 2 -> " + i + " Thread " + Thread.currentThread().getName()); return i;});

        //Test -> Expected
        StepVerifier.create(flux).expectSubscription().expectNext(1,2,3,4,5).verifyComplete();
    }

    @Test
    public void subscribeOnIO() throws Exception{
        Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Path.of("Texto.txt")))
                                      .log()
                                      .subscribeOn(Schedulers.boundedElastic());
        list.subscribe(l -> {for(String s : l){System.out.println(s);}});
    
        Thread.sleep(2000);

        //Test -> Expected
        StepVerifier.create(list).expectSubscription().thenConsumeWhile(l -> {assertFalse(l.isEmpty()); System.out.println("Size: " + l.size()); return true;}).verifyComplete();
    }

    @Test
    public void switchIfEmptyOperator(){
        Flux<Object> flux = emptyFlux().switchIfEmpty(Flux.just("not empty anymore")).log();

        //Test -> Expected
        StepVerifier.create(flux).expectSubscription().expectNext("not empty anymore").verifyComplete();
    }

    @Test
    public void deferOperator() throws InterruptedException{
        Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

        defer.subscribe(l -> System.out.println(l));
        Thread.sleep(100);
        defer.subscribe(l -> System.out.println(l));
        Thread.sleep(100);
        defer.subscribe(l -> System.out.println(l));
    }

    @Test
    public void concatOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> fluxConcat = Flux.concat(flux1, flux2).log();
        //Flux<String> fluxConcat2 = flux1.concatWith(flux2).log();
        
        //Test -> Expected
        StepVerifier.create(fluxConcat).expectSubscription().expectNext("a","b","c","d").verifyComplete();
    }

    @Test
    public void combineLastOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> fluxCombine = Flux.combineLatest(flux1, flux2, (f1, f2) -> f1 + f2).log();
        
        //Test -> Expected
        StepVerifier.create(fluxCombine).expectSubscription().expectNext("bc", "bd").verifyComplete();
    }

    @Test
    public void mergeOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
    
        Flux<String> fluxMerge = Flux.merge(flux1, flux2).log();
        //Flux<String> fluxMerge2 = flux1.mergeWith(flux2);

        StepVerifier.create(fluxMerge).expectSubscription().expectNext("a","b","c","d").verifyComplete();
    }

    @Test
    public void mergeSequentialOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
    
        Flux<String> fluxSeqMerge = Flux.mergeSequential(flux1, flux2, flux1).log();

        //Test -> Expected
        StepVerifier.create(fluxSeqMerge).expectSubscription().expectNext("a","b","c","d","a","b").verifyComplete();
    }

    @Test
    public void concatOperatorError(){
        Flux<String> flux1 = Flux.just("a", "b").map(s -> {if(s.equals("b")){throw new IllegalArgumentException();} return s;});
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> fluxConcat = Flux.concatDelayError(flux1, flux2).log();
        
        //Test -> Expected
        StepVerifier.create(fluxConcat).expectSubscription().expectNext("a","c","d").expectError().verify();
    }

    @Test
    public void mergeOperatorError(){
        Flux<String> flux1 = Flux.just("a", "b").map(s -> {if(s.equals("b")){throw new IllegalArgumentException();} return s;});
        Flux<String> flux2 = Flux.just("c", "d");
    
        Flux<String> fluxMerge = Flux.mergeDelayError(1, flux1, flux2, flux1).log();

        //Test -> Expected
        StepVerifier.create(fluxMerge).expectSubscription().expectNext("a","c","d","a").expectError().verify();
    }

    @Test
    public void flatMapOperator() throws Exception{
        Flux<String> flux = Flux.just("a","b");
        Flux<String> fluxFlatMap = flux.map(String::toUpperCase)
            .flatMap(s -> {return s.equals("A") ? Flux.just("name A1") : Flux.just("name B1");})
            .log();
        fluxFlatMap.subscribe();

    }

    @Test
    public void zipOperator(){
        Flux<String> fluxTxt = Flux.just("texto A", "texto B");
        Flux<Integer> fluxNum = Flux.just(1, 2);

        Flux<Entity> zip = Flux.zip(fluxTxt, fluxNum)
            .flatMap(t -> Flux.just(new Entity(t.getT1(), t.getT2())))
            .log();

        zip.subscribe();
    }

    private Flux<Object> emptyFlux(){
        return Flux.empty();
    }

}

@AllArgsConstructor
@NoArgsConstructor
@Data
class Entity{
    private String txt;
    private Integer num;
}
