package guru.springframework.reactivebeerclient.config;

public class WebClientProperties {

    public static final String BASE_URL="http://localhost:8080";
    public static final String BEER_V1_PATH = "/api/v1/beer";
    public static final String BEER_V1_PATH_GETBY_ID = "/api/v1/beer/{id}";
    public static final String BEER_V1_UPC_PATH ="/api/v1/beerUpc/{upc}" ;
}
