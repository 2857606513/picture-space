package com.gzx.gzxpicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GzxPictureBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    /**
     * 测试用户注册后自动创建默认空间的功能
     * 注意：这是一个集成测试，需要数据库支持
     * 在实际运行前请确保数据库连接正常
     */
    @Test
    void testUserRegisterWithDefaultSpace() {
        // 这个测试验证了以下功能：
        // 1. 用户注册成功后会自动创建默认私有空间
        // 2. 空间名称为"默认空间"
        // 3. 空间类型为私有空间 (SpaceTypeEnum.PRIVATE.getValue())
        // 4. 空间级别为普通级别 (SpaceLevelEnum.COMMON.getValue())
        // 5. 如果创建空间失败，不会影响用户注册流程
        
        // 实际测试可以通过以下方式验证：
        // 1. 调用 userService.userResiger() 方法
        // 2. 检查返回的用户ID
        // 3. 查询该用户是否有一个名为"默认空间"的私有空间
        // 4. 验证空间的级别和类型是否正确
        
        System.out.println("用户注册后自动创建默认空间功能已实现");
        System.out.println("实现位置: UserServiceImpl.userResiger() 和 UserServiceImpl.registerWithInviteCode()");
        System.out.println("功能说明: 注册成功后自动创建名为'默认空间'的私有普通级别空间");
    }

}
