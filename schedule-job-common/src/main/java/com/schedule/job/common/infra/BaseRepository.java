package com.schedule.job.common.infra;

import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * 模仿JpaRepository写一个基础通用的Repository接口
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> {
    boolean save(T entity);

    T findById(ID id);

    List<T> findAll();

    boolean update(T entity);

    boolean deleteById(ID id);
}
