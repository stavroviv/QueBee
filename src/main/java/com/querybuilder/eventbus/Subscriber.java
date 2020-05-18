package com.querybuilder.eventbus;

import java.util.Map;

public interface Subscriber {
    void initData(Map<String, Object> userData);
}
