/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/6/9 20:41
 */

package com.xxxxx.xxxxxxxx.project.utility;

import com.xxxxx.xxxxxxxx.project.model.Protocol;
import com.xxxxx.xxxxxxxx.project.model.exception.BoundException;
import com.xxxxx.xxxxxxxx.project.model.exception.RequestParamException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class IdAttrComparator implements Comparator<Protocol.IdAttr> {
    private static final String kAsc = "asc";
    private static final String kDesc = "desc";

    private List<Map<String, Protocol.SortOption>> sortAttrs;

    public IdAttrComparator(List<Map<String, Protocol.SortOption>> sortAttrs) {
        this.sortAttrs = sortAttrs.stream().filter(elem -> !elem.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public int compare(Protocol.IdAttr o1, Protocol.IdAttr o2) {
        Map<String, Object> attrs1 = o1.getAttrs();
        Map<String, Object> attrs2 = o2.getAttrs();

        for (Map<String, Protocol.SortOption> attrOpt : sortAttrs) {
            if (attrOpt.size() != 1) {
                throw new RequestParamException("More than one attribute in a sort option.");
            }

            String attrName = attrOpt.keySet().iterator().next();
            Protocol.SortOption option = attrOpt.get(attrName);

            int res = compMap(attrs1, attrs2, attrName);
            if (res != 0) {
                if (kAsc.equals(option.getOrder())) {
                    return res;
                } else if (kDesc.equals(option.getOrder())) {
                    return -res;
                } else {
                    throw new RequestParamException(
                            "Invalid `order` in sort options, must be `asc` or `desc`");
                }
            }
        }
        return 0;
    }

    private int compMap(Map<String, Object> attrs1, Map<String, Object> attrs2,
                        String key) {
        Object attrVal1 = attrs1.get(key);
        Object attrVal2 = attrs2.get(key);
        if (attrVal1 == null && attrVal2 == null) {
            return 0;
        } else if (attrVal1 == null) {
            return -1;
        } else if (attrVal2 == null) {
            return 1;
        } else {
            return compObject(attrVal1, attrVal2, key);
        }
    }

    private int compObject(Object v1, Object v2, String key) {
        if (v1.getClass() != v2.getClass()) {
            throw new BoundException(String.format(
                    "Cannot compare objects of different class, key: %s", key));
        }
        if (v1 instanceof List) {
            throw new RequestParamException(String.format(
                    "Not supported to compare on array type by now, key: %s", key));
        }
        try {
            if (v1 instanceof Map) {
                Map<String, Object> m1 = (Map<String, Object>) v1;
                Map<String, Object> m2 = (Map<String, Object>) v2;
                Set<String> sortedKeys = new TreeSet<>(m1.keySet());
                sortedKeys.addAll(m2.keySet());

                for (String k : sortedKeys) {
                    int res = compMap(m1, m2, k);
                    if (res != 0) {
                        return res;
                    }
                }
                return 0;
            } else {
                return ((Comparable) v1).compareTo(v2);
            }
        } catch (ClassCastException e) {
            throw new RequestParamException(String.format(
                    "Not supported to compare on type %s by now, key: %s",
                    v1.getClass().getSimpleName(), key));
        }
    }
}
