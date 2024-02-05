package org.myorg.quickstart;


import lombok.Data;

@Data
public class CountWithTimestamp {
    private static final long serialVersionUID = 1L;

    public String key;
    public long count;
    public long lastModified;

    @Override
    public String toString() {
        return "CountWithTimestamp{" +
                "key='" + key + '\'' +
                ", count=" + count +
                ", lastModified=" + lastModified +
                '}';
    }

}