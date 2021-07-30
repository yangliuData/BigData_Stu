package cn.leetcode.structure;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author LiuYang
 * @Date 2021/7/30 3:08 下午
 * 给定一个 n 个元素有序的（升序）整型数组 nums 和一个目标值 target  ，写一个函数搜索 nums 中的 target，如果目标值存在返回下标，否则返回 -1。
 * <p>
 * 输入: nums = [-1,0,3,5,9,12], target = 9
 * 输出: 4
 * 解释: 9 出现在 nums 中并且下标为 4
 * <p>
 * 输入: nums = [-1,0,3,5,9,12], target = 2
 * 输出: -1
 * 解释: 2 不存在 nums 中因此返回 -1
 */
public class binarySearch {

    public static void main(String[] args) {
        int[] nums = {-1, 0, 3, 13, 9, 12};
        int target = 13;
        int s = search(nums, target);

        System.out.println(7 >> 1);

    }

    public static int search(int[] nums, int target) {
        int left = 0, right = nums.length;
        while (left < right) {
            int mid = left + ((right - left) >> 1);
            if (nums[mid] == target)
                return mid;
            else if (nums[mid] < target)
                left = mid + 1;
            else if (nums[mid] > target)
                right = mid;
        }
        return -1;

    }

    /**
     * 给定一个排序数组和一个目标值，在数组中找到目标值，并返回其索引。如果目标值不存在于数组中，返回它将会被按顺序插入的位置。
     * <p>
     * 请必须使用时间复杂度为 O(log n) 的算法。
     * <p>
     * 输入: nums = [1,3,5,6], target = 7
     * 输出: 4
     *
     * @param nums
     * @param target
     * @return
     */
    public static int searchTwo(int[] nums, int target) {
        int temp = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == target) {
                temp = i;
                break;
            } else if (nums[nums.length - 1] < target) {
                temp = nums.length;
                break;
            } else if (nums[0] > target) {
                temp = 0;
            } else if (nums[i] > target) {
                temp = i;
                break;
            }
        }
        return temp;
    }

    /**
     * 给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出 和为目标值 的那 两个 整数，并返回它们的数组下标。
     * <p>
     * 你可以假设每种输入只会对应一个答案。但是，数组中同一个元素在答案里不能重复出现。
     * <p>
     * 你可以按任意顺序返回答案。
     * <p>
     * 输入：nums = [2,7,11,15], target = 9
     * 输出：[0,1]
     * 解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。
     */
    public int[] twoSum(int[] nums, int target) {
        int[] indexs = new int[2];

        // 建立k-v ，一一对应的哈希表
        Map<Integer, Integer> hash = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer numberA = Integer.valueOf(nums[i]);
            if (hash.containsKey(numberA)) {
                indexs[0] = i;
                indexs[1] = hash.get(numberA);
                return indexs;
            }
            // 将数据存入 key为补数 ，value为下标
            hash.put(target - nums[i], i);
        }
        return indexs;
    }
}


