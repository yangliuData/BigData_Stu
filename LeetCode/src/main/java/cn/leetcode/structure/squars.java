package cn.leetcode.structure;

/**
 * @Author LiuYang
 * @Date 2021/8/2 10:36 上午
 * 有序数组的平方
 * 给你一个按 非递减顺序 排序的整数数组 nums，返回 每个数字的平方 组成的新数组，要求也按 非递减顺序 排序。
 */
public class squars {
    public static  void main(String[] args) {
        int[] nums = {-4,-1,0,3,10};
        squars squars = new squars();

        int[] ints = squars.sortedSquares(nums);
        System.out.println(ints);
        for (int anInt : ints) {
            System.out.println(anInt);
        }
    }

    /**
     * 输入：nums = [-4,-1,0,3,10]
     * 输出：[0,1,9,16,100]
     * 解释：平方后，数组变为 [16,1,0,9,100]
     * 排序后，数组变为 [0,1,9,16,100]
     *
     * @param A
     * @return
     */
    public int[] sortedSquares(int[] A) {
        int start = 0;
        int end = A.length;
        int i = end - 1;
        int[] nums = new int[end--];
        while (i >= 0) {
            nums[i--] = A[start] * A[start] >= A[end] * A[end] ? A[start] * A[start++] :A[end]*A[end--];
        }
        return nums;
    }
}
