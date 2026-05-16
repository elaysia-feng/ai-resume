package com.airesumeforge.mq;


import java.util.Arrays;
import java.util.List;
import lombok.Data;

@Data
public class MultiDelayMessage<T> {
    // 消息体
    private T data;
    // 记录延迟时间的集合
    private List<Long> delays;

    public MultiDelayMessage(final T data, final List<Long> delays) {
        this.data = data;
        this.delays = delays;
    }

    public static <T> MultiDelayMessage<T> of(T data,Long ... delays) {
        return new MultiDelayMessage<>(data, Arrays.asList(delays));
    }


    // 获取并移除下一个延迟时间
    // return 获取数组中第一个延迟时间
    public Long removeAndGetCurrent(){
        return delays.remove(0);
    }

    // 是否有下一个超时时间
    public boolean hasNextDelay(){
        return !delays.isEmpty();
    }


}
