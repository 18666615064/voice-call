package com.voicecall.demo.bean;

import java.util.Map;
import java.util.Set;

public class EventData {
    private String eventName;
    private Map<String, Object> data;
    private String list;
    private String userID;

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


}
