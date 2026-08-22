package fileidea.rack.common;

public final class NotImplemented {

    private NotImplemented() {
    }

    public static <T> T yet(String feature) {
        throw new UnsupportedOperationException(feature + " is not implemented");
    }
}
