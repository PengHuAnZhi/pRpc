package com.phz.prpc.netty.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 消息序号生成类
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月12日 19:20
 */
public abstract class SequenceIdGenerator {
    /**
     * 原子操作{@code Integer}对象
     **/
    private static final AtomicInteger ID = new AtomicInteger();

    /**
     * 获取一个自增的{@code Id}
     *
     * @return int 生成的{@code Id}
     **/
    public static int nextId() {
        return ID.incrementAndGet();
    }
}