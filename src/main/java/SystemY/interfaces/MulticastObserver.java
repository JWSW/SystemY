package SystemY.interfaces;

import java.io.IOException;

public interface MulticastObserver {
    void onMessageReceived(String message) throws IOException;
}
