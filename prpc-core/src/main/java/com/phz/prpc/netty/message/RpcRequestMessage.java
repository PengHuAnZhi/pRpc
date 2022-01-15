package com.phz.prpc.netty.message;

import lombok.*;

/**
 * <p>
 * {@code rpc}请求消息类型
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:36
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class RpcRequestMessage extends Message {
    /**
     * 调用的接口全限定名，服务端根据它找到实现
     */
    private String interfaceName;
    /**
     * 调用接口中的方法名
     */
    private String methodName;
    /**
     * 服务组名
     **/
    private String groupName;
    /**
     * 方法返回类型
     */
    private Class<?> returnType;
    /**
     * 方法参数类型数组
     */
    private Class<?>[] parameterTypes;
    /**
     * 方法参数值数组
     */
    private Object[] parameterValue;

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_REQUEST;
    }
}
