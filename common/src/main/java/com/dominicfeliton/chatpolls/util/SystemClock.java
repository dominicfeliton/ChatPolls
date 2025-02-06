package com.dominicfeliton.chatpolls.util;

import java.time.LocalDateTime;

public class SystemClock implements Clock {
    @Override
    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
}
