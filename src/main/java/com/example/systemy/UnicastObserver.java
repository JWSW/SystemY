package com.example.systemy;

import java.io.IOException;

public interface UnicastObserver {
    void onMessageReceived(String message) throws IOException;
}
