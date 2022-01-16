package com.phz.prpc.netty.loadBalance;

import com.phz.prpc.util.CollectionUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * <p>
 * 一致性哈希算法选择器，原理参考博文 @see <a href="https://blog.csdn.net/qq_43509535/article/details/122513401">扒一扒一致性Hash算法+Java版案例演示</a>
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 18:33
 */
@Data
@NoArgsConstructor
public class ConsistenceHashChooser {
    /**
     * 虚拟节点数量
     **/
    private int virtualNodeNum;

    /**
     * 物理节点
     **/
    private List<InetSocketAddress> realNodes;

    /**
     * 对应关系
     **/
    private final Map<InetSocketAddress, List<Integer>> real2VirtualNodeMap = new HashMap<>();

    /**
     * 一致性Hash环
     **/
    private final SortedMap<Integer, InetSocketAddress> hashRing = new TreeMap<>();

    /**
     * 构造方法
     *
     * @param virtualNodeNum 虚拟节点数
     * @param newNodes       服务节点
     **/
    public ConsistenceHashChooser(int virtualNodeNum, List<InetSocketAddress> newNodes) {
        this.virtualNodeNum = virtualNodeNum;
        if (realNodes == null) {
            this.realNodes = newNodes;
            newNodes.forEach(this::addServer);
            return;
        }
        newNodes.forEach(realNode -> {
            boolean oldFlag = CollectionUtils.isEmpty(this.realNodes);
            boolean newFlag = CollectionUtils.isEmpty(newNodes);
            if (oldFlag && !newFlag) {
                newNodes.forEach(this::addServer);
            } else if (!oldFlag && newFlag) {
                this.realNodes.forEach(this::removeServer);
            } else {
                Map<InetSocketAddress, Integer> compareResult = CollectionUtil.mapCompare(this.realNodes, newNodes);
                compareResult.forEach((key, value) -> {
                    if (value == 1) {
                        removeServer(key);
                    } else if (value == 3) {
                        addServer(key);
                    }
                });
            }
        });
        this.realNodes = newNodes;
    }

    /**
     * 加服务
     *
     * @param inetSocketAddress 真实节点
     **/
    public void addServer(InetSocketAddress inetSocketAddress) {
        //虚拟出多少个虚拟节点
        String visualNode;
        List<Integer> virNodes = new ArrayList<>(virtualNodeNum);
        real2VirtualNodeMap.put(inetSocketAddress, virNodes);
        for (int i = 0; i < virtualNodeNum; i++) {
            visualNode = inetSocketAddress + "-" + i;
            //放到环上
            //1、求Hash值，由于Java默认提供的HashCode方法得到的hash值散列度不是很好，所以我们采用新的HashCode算法
            int hash = FNV1_32_HASH.getHash(visualNode);
            //2、放到环上去
            hashRing.put(hash, inetSocketAddress);
            //3、保存对应关系
            virNodes.add(hash);
        }
    }

    /**
     * 找到数据的存放节点
     *
     * @param virtualNode 虚拟节点key
     * @return 返回真实节点
     */
    public InetSocketAddress getServer(String virtualNode) {
        int hashValue = FNV1_32_HASH.getHash(virtualNode);
        SortedMap<Integer, InetSocketAddress> subMap = hashRing.tailMap(hashValue);
        if (subMap.isEmpty()) {
            //数据应该放在最小的节点
            return hashRing.get(hashRing.firstKey());
        }
        return subMap.get(subMap.firstKey());
    }

    /**
     * 移除节点
     *
     * @param inetSocketAddress 节点名称
     **/
    public void removeServer(InetSocketAddress inetSocketAddress) {
        realNodes.remove(inetSocketAddress);
        real2VirtualNodeMap.remove(inetSocketAddress);
        String visualNode;
        for (int i = 0; i < virtualNodeNum; i++) {
            visualNode = inetSocketAddress + "-" + i;
            int hash = FNV1_32_HASH.getHash(visualNode);
            hashRing.remove(hash);
        }
    }
}