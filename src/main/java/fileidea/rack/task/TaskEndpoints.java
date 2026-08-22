package fileidea.rack.task;

public final class TaskEndpoints {

    public static final String LENS = "google_lens";
    public static final String PRICE = "price_item";
    public static final String PHOTOGRAPH = "photograph";
    public static final String PUBLISH = "publish";

    private TaskEndpoints() {
    }

    public static String lensKey(Long itemId) {
        return "lens:" + itemId;
    }

    public static String priceKey(Long itemId) {
        return "price:" + itemId;
    }

    public static String photographKey(Long itemId) {
        return "photo:" + itemId;
    }

    public static String publishKey(Long itemId) {
        return "publish:" + itemId;
    }
}
