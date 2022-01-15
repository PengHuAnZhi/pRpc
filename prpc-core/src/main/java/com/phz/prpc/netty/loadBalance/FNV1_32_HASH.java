package com.phz.prpc.netty.loadBalance;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 18:26
 */
public class FNV1_32_HASH {
    /**
     * FNV1_32_HASH算法
     *
     * @param string key
     * @return int 返回Hash值
     **/
    public static int getHash(String string) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < string.length(); i++) {
            hash = (hash ^ string.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return hash < 0 ? Math.abs(hash) : hash;
    }
}