package cn.lambda;

/**
 * @Author LiuYang
 * @Date 2021/8/19 3:00 下午
 */
public class Testlambda2 {

    public static void main(String[] args) {
        Ilove love = null;
        // 1.lambda表示简化
//            love = (int a) ->{
//                System.out.println("I Love you -->" + a);
//            };
//        // 简化1.参数类型
//         love = ( a) ->{
//            System.out.println("I Love you -->" + a);
//        };

        // 简化2.括号
        love = a ->{
            System.out.println("I Love you -->" + a);
        };
        // 简化3.去掉花括号

        love = a -> System.out.println("I Love you -->" + a);

           love.love(520);
    }
}
interface Ilove{
    void love(int a);
}


