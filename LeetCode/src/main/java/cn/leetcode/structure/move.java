package cn.leetcode.structure;

/**
 * @Author LiuYang
 * @Date 2021/8/3 9:58 上午
 */
public class move {
    public static void main(String[] args) {
        int[] nums = {0,1,0,3,12};
        int[] ints = new move().moveZeroes(nums);

        for (int anInt : ints) {
            System.out.println(anInt);
        }

    }

    /**
     * 给定一个数组 nums，编写一个函数将所有 0 移动到数组的末尾，同时保持非零元素的相对顺序。
     * 输入: [0,1,0,3,12]
     * 输出: [1,3,12,0,0]
     * @param nums
     * @return
     */
    public int[] moveZeroes(int[] nums) {
        for (int i = 0,count = 0; i < nums.length; i++) {
            if(nums[i] != 0){
                // 执行替换
                if(count != i) {
                    nums[count] = nums[i];
                    nums[i] = 0;
                }
                count ++;
            }

        }
        return nums;
    }
}
