package com.token.delongaizerocode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");


    private final String name;
    private final String value;

    UserRoleEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value){
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)){
                return anEnum;
            }
        }
        return null;
    }


}
