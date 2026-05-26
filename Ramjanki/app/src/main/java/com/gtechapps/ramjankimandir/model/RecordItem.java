package com.gtechapps.ramjankimandir.model;

public class RecordItem {
    public final String recordId;
    public final String title;
    public final String subtitle;
    public final String detail;
    public final String status;

    public RecordItem(String recordId, String title, String subtitle, String detail, String status) {
        this.recordId = recordId;
        this.title = title;
        this.subtitle = subtitle;
        this.detail = detail;
        this.status = status;
    }
}
