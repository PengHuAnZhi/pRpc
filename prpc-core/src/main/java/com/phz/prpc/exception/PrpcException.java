package com.phz.prpc.exception;

/**
 * <p>
 * 定义我们自己的运行时异常
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月09日 13:59
 */
public class PrpcException extends RuntimeException {
    /**
     * 构造方法，构造方法，传入程序预定义类型错误信息
     **/
    public PrpcException(ErrorMsg msg) {
        super(msg.getMessage());
    }
}