package cn.leetcode.structure

/**
 * @Author LiuYang
 * @Date 2021/8/3 10:12 上午
 */
object move_s {
  def moveZeroes(nums: Array[Int]): Array[Int] = {
    var i = 0
    var count = 0
    while ( {
      i < nums.length
    }) {
      if (nums(i) != 0) { // 执行替换
        if (count != i) {
          nums(count) = nums(i)
          nums(i) = 0
        }
        count += 1
      }
      i += 1
    }
    nums
  }

}
