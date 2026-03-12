package com.token.delongaizerocode.aop;


import com.token.delongaizerocode.annotation.AuthnCheck;
import com.token.delongaizerocode.entity.User;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.UserRoleEnum;
import com.token.delongaizerocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 在调用接口前判断权限是否足够
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     *  执行拦截
     * @param joinPoint 切入点
     * @param authnCheck 权限校验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authnCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthnCheck authnCheck) throws Throwable {
        String mustRole = authnCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //以下的代码必须有权限才能通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //没有权限直接拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        //要求必须有管理员权限，但前登录用户没有
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        // 通过普通用户的权限校验，放行
        return joinPoint.proceed();
    }
}
