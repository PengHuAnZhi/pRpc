package com.phz.prpc.netty.compress;

import com.phz.prpc.extension.Spi;

/**
 * @author PengHuanZhi
 * @date 2022年01月16日 11:34
 */
@Spi
public interface Compress {
    /**
     * 将字节数组压缩返回
     *
     * @param bytes 原始字节
     * @return byte 压缩后字节
     **/
    byte[] compress(byte[] bytes);

    /**
     * 将压缩字节数组解压缩返回
     *
     * @param bytes 压缩字节数组
     * @return byte 原始字节数组
     **/
    byte[] decompress(byte[] bytes);
}