package iteration2;

public class TestUtils {
    public static void repeat(int times, Runnable action){
        for (int i = 0; i < times; i++){
            action.run();
        }
    }
}
