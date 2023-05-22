package SystemY.interfaces;

import java.io.IOException;

public interface Observer {
    void onMessageReceived(String type, String message) throws IOException;


}
