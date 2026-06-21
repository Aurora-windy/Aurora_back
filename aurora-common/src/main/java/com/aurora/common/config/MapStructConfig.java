package com.aurora.common.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct 全局配置
 *
 * <p>所有模块的 {@code *Struct} 转换器接口用 {@code @Mapper(config = MapStructConfig.class)} 引用此配置，
 * 自动使用 Spring 容器、忽略未映射字段。</p>
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface MapStructConfig {
}
