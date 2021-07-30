package cn.leetcode.structure

import java.util
import java.util.{HashMap, Map}

/**
 * @Author LiuYang
 * @Date 2021/7/30 3:46 下午
 */
object binarySearch_s {

  def main(args: Array[String]): Unit = {
    val nums = Array[Int](0, 1, 3, 4, 9, 12)
    val target = -9
    val i: Int = searchTwo(nums, target)
    println(i)
  }


  def search(nums: Array[Int], target: Int): Int = {
    var left = 0
    var right = nums.length
    while (left < right) {
      var mid = left + ((right - left) >> 1)
      if (nums(mid) == target) {
        return mid
      } else if (nums(mid) < target) {
        left = left + 1
      } else if (nums(mid) > target) {
        right = mid
      }
    }
    -1
  }

  /**
   * 给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出 和为目标值 的那 两个 整数，并返回它们的数组下标。
   *
   * 你可以假设每种输入只会对应一个答案。但是，数组中同一个元素在答案里不能重复出现。
   *
   * 你可以按任意顺序返回答案。
   *
   * 输入：nums = [2,7,11,15], target = 9
   * 输出：[0,1]
   * 解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。
   */
  def twoadd(nums: Array[Int], target: Int): Array[Int] = {

    val indexs = new Array[Int](2)

    // 建立k-v ，一一对应的哈希表
    val hash = new util.HashMap[Integer, Integer]
    for (i <- 0 until nums.length) {
      val numberA = Integer.valueOf(nums(i))
      if (hash.containsKey(numberA)) {
        indexs(0) = i
        indexs(1) = hash.get(numberA)
        return indexs
      }
      // 将数据存入 key为补数 ，value为下标
      hash.put(target - nums(i), i)
    }
     indexs
  }

  def searchTwo(nums: Array[Int], target: Int): Int = {
    for (i <- 0 until nums.length) {
      if (nums(i) == target) {
       return i
      }
      else if (nums(nums.length - 1) < target) {
        return  nums.length
      }
      else if (nums(0) > target) return 0
      else if (nums(i) > target) {
        return i
      }
    }
    0
  }

}
