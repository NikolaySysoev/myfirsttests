package generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomData {
    private RandomData(){}

    public static String getUserName(){
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getUserPassword(){
        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
                RandomStringUtils.randomNumeric(3) +
                "$";
    }

    /**
     * Generator for random number from 1 to 5000
     * used for BigDecimals
     * @return random number from 1 to 5000 as a string
     */
    public static String getRandomAmountAsString() {
        int value = ThreadLocalRandom.current().nextInt(1,5001);
        return String.valueOf(value);
    }
}
