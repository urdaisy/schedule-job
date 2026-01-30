package com.schedule.job.admin.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo<T> {
    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 构造分页结果
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @param total 总记录数
     * @param list 数据列表
     */
    public PageInfo(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        // 计算总页数
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        // 计算是否有上一页和下一页
        this.hasPrevious = pageNum > 1;
        this.hasNext = pageNum < totalPages;
    }
}
