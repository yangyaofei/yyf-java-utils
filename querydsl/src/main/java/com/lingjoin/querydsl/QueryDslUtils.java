package com.lingjoin.querydsl;


import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import cz.jirutka.rsql.parser.RSQLParserException;
import io.github.perplexhub.rsql.RSQLException;
import io.github.perplexhub.rsql.RSQLQueryDslSupport;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

/**
 * QueryDSL 工具类, 包括 rsql 支持
 */
@SuppressWarnings("unused")
@UtilityClass
public class QueryDslUtils {
    /**
     * 根据提供的分页信息,QueryDSL和RSQL查询字符串,从数据库中检索实体并返回分页结果
     *
     * @param <T>                       实体类型
     * @param <ID>                      实体标识符的类型
     * @param pageable                  分页参数
     * @param predicate                 谓词表达式
     * @param rsql                      rsql查询字符串
     * @param qClazz                    QEntity类型的类，表示查询的实体类型
     * @param jpaRepository             JPA仓库接口，用于操作数据库中的实体
     * @param querydslPredicateExecutor 提供基于Querydsl的查询功能的接口。
     * @return 分页结果的Page对象
     * @throws IllegalArgumentException 如果解析rsql字符串时发生异常，则抛出此异常，并附带错误信息
     */
    public <T, ID> Page<T> listEntity(
            Pageable pageable, Predicate predicate, String rsql,
            Path<T> qClazz,
            JpaRepository<T, ID> jpaRepository,
            QuerydslPredicateExecutor<T> querydslPredicateExecutor
    ) {
        try {
            predicate = ExpressionUtils.and(predicate, RSQLQueryDslSupport.toPredicate(rsql, qClazz));
            return listEntity(pageable, predicate, jpaRepository, querydslPredicateExecutor);
        } catch (RSQLParserException | RSQLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * QueryDSL 的 repository.findAll 空判断实现, 根据predicate是否为空选择使用函数并返回
     *
     * @param <T>                       实体类型
     * @param <ID>                      实体标识符的类型
     * @param pageable                  分页请求，包含分页信息和排序信息。
     * @param predicate                 查询条件，使用Querydsl的Predicate表示。
     * @param jpaRepository             jpaRepository
     * @param querydslPredicateExecutor 提供基于Querydsl的查询功能的接口。
     * @return 符合条件的实体列表，以分页形式返回。
     */
    public <T, ID> Page<T> listEntity(
            Pageable pageable, Predicate predicate,
            JpaRepository<T, ID> jpaRepository,
            QuerydslPredicateExecutor<T> querydslPredicateExecutor
    ) {
        return Optional.ofNullable(predicate)
                .map(p -> querydslPredicateExecutor.findAll(p, pageable))
                .orElse(jpaRepository.findAll(pageable));
    }

}
