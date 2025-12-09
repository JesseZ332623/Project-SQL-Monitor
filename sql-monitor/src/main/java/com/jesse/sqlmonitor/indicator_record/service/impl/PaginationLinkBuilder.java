package com.jesse.sqlmonitor.indicator_record.service.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import  io.github.jessez332623.reactive_response_builder.pojo.Link;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** HATEOAS 分页链接构造器。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class PaginationLinkBuilder
{
    public static @NotNull Set<Link>
    buildPaginationLinks(
        String basePath,
        Map<String, String> queryParams,
        long currentPage, long totalPages, long pageSize
    )
    {
        Set<Link> links = new LinkedHashSet<>();

        // self 链接
        links.add(
            new Link(
                "self",
                buildUri(basePath, queryParams, currentPage, pageSize),
                HttpMethod.GET
            )
        );

        // first 链接（只有不是第一页时才添加）
        if (currentPage > 1)
        {
            links.add(
                new Link(
                    "first",
                    buildUri(basePath, queryParams, 1, pageSize),
                    HttpMethod.GET
                )
            );
        }

        // prev 链接
        if (currentPage > 1)
        {
            links.add(
                new Link(
                    "prev",
                    buildUri(basePath, queryParams, currentPage - 1, pageSize),
                    HttpMethod.GET
                )
            );
        }

        // next 链接
        if (currentPage < totalPages)
        {
            links.add(
                new Link(
                    "next",
                    buildUri(basePath, queryParams, currentPage + 1, pageSize),
                    HttpMethod.GET
                )
            );
        }

        // last 链接
        if (currentPage < totalPages)
        {
            links.add(new Link(
                "last",
                buildUri(basePath, queryParams, totalPages, pageSize),
                HttpMethod.GET)
            );
        }

        return links;
    }

    private static @NotNull String
    buildUri(
        String basePath,
        @NotNull Map<String, String> params,
        long page, long size
    )
    {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);

        // 复制基础参数
        params.forEach(builder::queryParam);

        // 设置分页参数
        builder.queryParam("pageNo", page);
        builder.queryParam("perPageLimit", size);

        return builder.build().toUriString();
    }
}