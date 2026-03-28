package com.token.delongaizerocode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {

    HTML("原生HTML模式", "html"),
    MULTI_FILE("原生多文件模式", "multi_file"),
    VUE_PROJECT("Vue工程模式", "vue_project"),;

    private final String text;
    private final String value;


    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    /**
     *
     *
     *
     * @param value
     * @return
     */
    public static CodeGenTypeEnum getEnumByValue(String value){
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            if(anEnum.value.equals(value)){
                return anEnum;
            }
        }
        return null;
    }



}
