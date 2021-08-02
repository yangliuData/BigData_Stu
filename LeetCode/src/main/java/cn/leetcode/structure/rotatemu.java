package cn.leetcode.structure;

/**
 * @Author LiuYang
 * @Date 2021/8/2 2:57 下午
 * 给定一个数组，将数组中的元素向右移动 k 个位置，其中 k 是非负数。
 */
public class rotatemu {
    public static void main(String[] args) {
        int[] nums = {1,2,3,4,5,6,7};
        int k =  3;
        rotatemu rotatemu = new rotatemu();
        int[] rotate = rotatemu.rotate(nums, k);
        for (int i : rotate) {
            System.out.println(i);
        }
    }
    /**
     * 输入: nums = [1,2,3,4,5,6,7], k = 3
     * 输出: [5,6,7,1,2,3,4]
     * 解释:
     * 向右旋转 1 步: [7,1,2,3,4,5,6]
     * 向右旋转 2 步: [6,7,1,2,3,4,5]
     * 向右旋转 3 步: [5,6,7,1,2,3,4]
     *
     */
    public int[] rotate(int[] nums, int k) {
        int n = nums.length;
        k %= n;
        reverse(nums, 0, n - 1);
        reverse(nums, 0, k - 1);
        return reverse(nums, k, n - 1);
    }
    public int[] reverse(int[] nums, int start, int end) {
        while (start < end) {
            int temp = nums[start];
            nums[start++] = nums[end];
            nums[end--] = temp;
        }
        return nums;
    }
}
