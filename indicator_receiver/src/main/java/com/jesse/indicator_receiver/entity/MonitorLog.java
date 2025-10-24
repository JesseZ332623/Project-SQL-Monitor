package com.jesse.indicator_receiver.entity;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** 监控指标实体。*/
@Data
@ToString
@Builder(builderClassName = "Builder")
@Table(name = "monitor_log")
public class MonitorLog
{
    @Id
    @Column("log_id")
    private Long logId;

    @Column("datetime")
    private LocalDateTime datetime;

    @Column("server_ip")
    private Long serverIP;

    @Column("indicator")
    private String indicator;

    @Column("indicator_type")
    private IndicatorType indicatorType;
}