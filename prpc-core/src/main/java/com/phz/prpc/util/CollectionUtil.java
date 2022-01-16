package com.phz.prpc.util;

import com.google.common.collect.Lists;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 18:53
 */
public class CollectionUtil {
    /**
     * 对比两个list取出差并和的集合
     *
     * @param oldList 旧集合
     * @param newList 新集合
     * @param flag    1,删掉的数据;2,重复的数据;3,新增的数据
     * @return 返回指定的集合数据
     */
    public static List<InetSocketAddress> getCompareList(List<InetSocketAddress> oldList, List<InetSocketAddress> newList, Integer flag) {

        Map<InetSocketAddress, Integer> map = mapCompare(oldList, newList);
        List<InetSocketAddress> result;

        List<InetSocketAddress> oldData = Lists.newArrayList();
        List<InetSocketAddress> addData = Lists.newArrayList();
        List<InetSocketAddress> repeatData = Lists.newArrayList();

        map.forEach((key, value) -> {
            if (value == 1) {
                oldData.add(key);
            } else if (value == 2) {
                repeatData.add(key);
            } else {
                addData.add(key);
            }
        });

        if (flag.equals(1)) {
            result = oldData;
        } else if (flag.equals(2)) {
            result = repeatData;
        } else {
            result = addData;
        }
        return result;

    }

    /**
     * 单独获取两个不同集合的数据，高效率
     *
     * @param list1 集合1
     * @param list2 集合2
     * @return 返回差异数据
     */
    public static List<InetSocketAddress> getDifferentList(List<InetSocketAddress> list1, List<InetSocketAddress> list2) {
        List<InetSocketAddress> diff = Lists.newArrayList();
        //优先使用数据量大的list，提高效率
        List<InetSocketAddress> maxList = list1;
        List<InetSocketAddress> minList = list2;
        if (list2.size() > list1.size()) {
            maxList = list2;
            minList = list1;
        }
        Map<InetSocketAddress, Integer> map = new HashMap<>(maxList.size());
        for (InetSocketAddress inetSocketAddress : maxList) {
            map.put(inetSocketAddress, 1);
        }

        for (InetSocketAddress inetSocketAddress : minList) {
            if (map.get(inetSocketAddress) != null) {
                map.put(inetSocketAddress, 2);
                continue;
            }
            diff.add(inetSocketAddress);
        }
        for (Map.Entry<InetSocketAddress, Integer> entry : map.entrySet()) {
            if (entry.getValue() == 1) {
                diff.add(entry.getKey());
            }
        }
        return diff;

    }

    /**
     * 对比两个list，返回list并集
     *
     * @param oldList 旧数据
     * @param newList 新数据
     * @return 删掉的数据:1 重复的数据:2 新增的数据:3
     */
    public static Map<InetSocketAddress, Integer> mapCompare(List<InetSocketAddress> oldList, List<InetSocketAddress> newList) {
        //若知道两个list大小区别较大，以大的list优先处理
        Map<InetSocketAddress, Integer> map = new HashMap<>(oldList.size());

        //lambda for循环数据量越大，效率越高，小数据建议用普通for循环
        oldList.forEach(s -> map.put(s, 1));

        newList.forEach(s -> {
            if (map.get(s) != null) {
                //相同的数据
                map.put(s, 2);
            } else {
                //若只是比较不同数据，不需要此步骤，浪费资源
                map.put(s, 3);
            }
        });

        return map;
    }
}