package guru.springframework.reactivebeerclient.client;

import guru.springframework.reactivebeerclient.config.WebClientConfig;
import guru.springframework.reactivebeerclient.model.BeerDto;
import guru.springframework.reactivebeerclient.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeerClientImplTest {

    BeerClientImpl beerClient;

    @BeforeEach
    void setUp() {
        //Dependecy injection by own, not relying on Spring context hence no Spring boot test
        beerClient = new BeerClientImpl(new WebClientConfig().getWebClient());
    }

    @Test
    void listBeers() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();
        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isGreaterThan(0);
        beerPagedList.forEach(System.out::println);

    }

    @Test
    void listBeersOnPageNumber1_5() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 5, null, null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();
        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isEqualTo(5);
        beerPagedList.forEach(System.out::println);

    }

    @Test
    void listBeersOnPageNumber10_20() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(10, 20, null, null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();
        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isEqualTo(0);
        beerPagedList.forEach(System.out::println);

    }

    @Test
    void getBeerById() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 1, null, null, null);
        BeerPagedList beerPagedList = beerPagedListMono.block();
        UUID beerId = beerPagedList.getContent().get(0).getId();
        Mono<BeerDto> beerById = beerClient.getBeerById(beerId, false);
        BeerDto beerDto = beerById.block();
        assertThat(beerDto).isNotNull();
        assertThat(beerDto.getId()).isEqualTo(beerId);
        System.out.println(beerDto);
        }


    @Test
    void createBeer() {

        BeerDto beerDto= BeerDto.builder()
                .beerName("Kingfisher")
                .beerStyle("IPA")
                .upc("12142353656")
                .price(new BigDecimal("10.99"))
                .build();

        Mono<ResponseEntity<Void>> entityMono = beerClient.createBeer(beerDto);
        ResponseEntity responseEntity= entityMono.block();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateBeer() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 1, null, null, null);
        BeerPagedList beerPagedList = beerPagedListMono.block();
        UUID beerId = beerPagedList.getContent().get(0).getId();
        Mono<BeerDto> beerById = beerClient.getBeerById(beerId, false);
        BeerDto beerDto = beerById.block();


        BeerDto toBeUpdatedbeerDto= BeerDto.builder().beerName("Royal Magnum").beerStyle(beerDto.getBeerStyle()).upc(beerDto.getUpc())
                .price(beerDto.getPrice()).build();
        System.out.println("BeerDto to be updated:"+ toBeUpdatedbeerDto);
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.updateBeer(beerDto.getId(), toBeUpdatedbeerDto);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    }

    @Test
    void deleteBeerById() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 1, null, null, null);
        BeerPagedList beerPagedList = beerPagedListMono.block();
        BeerDto beerDto = beerPagedList.getContent().get(0);
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(beerDto.getId());
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getBeerByUPC() {

        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 1, null, null, null);
        BeerPagedList beerPagedList = beerPagedListMono.block();
        String UPC = beerPagedList.getContent().get(0).getUpc();
        Mono<BeerDto> beerByUPC = beerClient.getBeerByUPC(UPC);
        BeerDto beerDto = beerByUPC.block();
        assertThat(beerDto).isNotNull();
        assertThat(beerDto.getUpc()).isEqualTo(UPC);
        System.out.println(beerDto);
    }
    @Test
    void deleteBeerByIdNotFound() {
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(UUID.randomUUID());

        assertThrows(WebClientResponseException.class, () -> {
            ResponseEntity<Void> responseEntity = responseEntityMono.block();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        });

    }
    @Test
    void testDeleteBeerHandleException() {
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(UUID.randomUUID());

        ResponseEntity<Void> responseEntity = responseEntityMono.onErrorResume(throwable -> {
            if(throwable instanceof WebClientResponseException){
                WebClientResponseException exception = (WebClientResponseException) throwable;
                return Mono.just(ResponseEntity.status(exception.getStatusCode()).build());
            } else {
                throw new RuntimeException(throwable);
            }
        }).block();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    ///////////////////////////////////Functional style//////////////////////////////////////////



    @Test
    void getBeerByIdFunctionalStyle() throws InterruptedException {

        AtomicReference beerName= new AtomicReference();
        CountDownLatch countDownLatch= new CountDownLatch(1);
        beerClient.listBeers(null, null, null, null, null)
                .map(beerPageList->beerPageList.getContent().get(0).getId())
                .map(beerId->beerClient.getBeerById(beerId,false))
                .flatMap(mono-> mono)
                .subscribe(beerDto -> {
                    System.out.println(beerDto.getBeerName());
                    beerName.set(beerDto.getBeerName());
                    assertThat(beerDto.getBeerName()).isEqualTo("Mango Bobs");
                    countDownLatch.countDown();  // Dont mobe up this line before 2nd line. it will work so fast it will get completed
                    //before beerName is set

                });

             //   Thread.sleep(1000);
        countDownLatch.await();
        assertThat(beerName.get()).isEqualTo("Mango Bobs");
        /*Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 1, null, null, null);
        BeerPagedList beerPagedList = beerPagedListMono.block();
        UUID beerId = beerPagedList.getContent().get(0).getId();
        Mono<BeerDto> beerById = beerClient.getBeerById(beerId, false);
        BeerDto beerDto = beerById.block();
        assertThat(beerDto).isNotNull();
        assertThat(beerDto.getId()).isEqualTo(beerId);
        System.out.println(beerDto);*/
    }






}