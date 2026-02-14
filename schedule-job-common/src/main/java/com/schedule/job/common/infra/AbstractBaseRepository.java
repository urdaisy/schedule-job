package com.schedule.job.common.infra;

import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.common.infra.orm.Column;
import com.schedule.job.common.infra.orm.Entity;
import com.schedule.job.common.infra.orm.Id;
import com.schedule.job.common.util.TypeConvertorUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractBaseRepository<T, ID> implements BaseRepository<T, ID> {
    protected final Class<T> entityClass;
    protected String tableName;

    protected String idColumnName;

    protected String idFieldName;

    protected Map<String, String> fieldToColumn;

    @SuppressWarnings("unchecked")
    public AbstractBaseRepository() {
        this.entityClass =
                (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        initTableName();
        initIdInfo();
        initFieldToColumn();
    }

    // 初始化表名
    private void initTableName() {
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation != null && !entityAnnotation.name().isEmpty()) {
            this.tableName = entityAnnotation.name();
        } else {
            this.tableName = camelToUnderline(entityClass.getSimpleName());
        }
        if (this.tableName == null || this.tableName.isEmpty()) {
            throw new RuntimeException("实体类" + entityClass.getName() + "未标注@Table主键注解");
        }
    }

    // 每个字符串遍历，识别到大写字母添加下划线后转小写
    private String camelToUnderline(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(str.charAt(0)));
        for (int i = 1; i < str.length(); i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                sb.append("_").append(Character.toLowerCase(str.charAt(i)));
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    // 将主键的类名idFieldName和主键数据库的表名idColumnName保存起来
    private void initIdInfo() {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            // 识别是否是主键
            if (field.isAnnotationPresent(Id.class)) {
                this.idFieldName = field.getName();
                Column column = field.getAnnotation(Column.class);
                if (column != null && !column.name().isEmpty()) {
                    this.idColumnName = column.name();
                } else {
                    this.idColumnName = camelToUnderline(field.getName());
                }
            }
        }

        if (this.idFieldName == null || this.idFieldName.isEmpty()) {
            throw new RuntimeException("实体类" + entityClass.getName() + "未标注@Id主键注解");
        }
    }

    private void initFieldToColumn() {
        Field[] fields = entityClass.getDeclaredFields();
        this.fieldToColumn = new HashMap<>();
        for (Field field : fields) {
            String fieldName = field.getName();
            Column column = field.getAnnotation(Column.class);
            String columnName = (column != null && !column.name().isEmpty()) ? column.name() : camelToUnderline(fieldName);
            this.fieldToColumn.put(fieldName, columnName);
        }
    }

    // 从SQL ResultSet实体转换为Java Entity
    protected T mapResultSetToEntity(ResultSet rs) {
        try {
            T entity = this.entityClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, String> entry : this.fieldToColumn.entrySet()) {
                String fieldName = entry.getKey();
                String columnName = entry.getValue();
                Field field = this.entityClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object sourceValue = rs.getObject(columnName);
                // 强制类型转换
                Object targetValue = TypeConvertorUtil.convert(field.getType(), sourceValue);
                field.set(entity, targetValue);
            }
            return entity;

        } catch (Exception e) {
            throw new RuntimeException("结果集映射实体失败, 实体类：" + entityClass.getName() + "异常: " + e.getMessage());
        }
    }

    protected Object getFieldValue(T entity, String fieldName) {
        try {
            Field field = this.entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            throw new RuntimeException("反射获取实体值失败, 实体类：" + entityClass.getName() + "异常: " + e.getMessage());
        }
    }

    protected void setFieldValue(T entity, String fieldName, Long fieldValue) {
        try {
            Field field = this.entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException("反射设置实体值失败, 实体类：" + entityClass.getName() + ", 字段：" + fieldName + ", 异常: " + e.getMessage(), e);
        }
    }

    protected boolean isAutoIncrementPrimaryKey() {
        try {
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    Id annotation = field.getAnnotation(Id.class);
                    return annotation.autoIncrement();
                }
            }
            return false;
        } catch (Exception e) {
            log.error("反射判断主键id失败, 实体类：" + entityClass.getName() + "异常: " + e.getMessage());
            return false;
        }
    }

    // ====================== CRUD 实现 ======================
    @Override
    public boolean save(T entity) {
        try {
            // 检查实体是否有ID，如果有ID则执行更新，否则执行插入
            Object id = getFieldValue(entity, idFieldName);
            if (id != null) {
                return update(entity);
            }

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(this.tableName).append(" (");
            StringBuilder value = new StringBuilder(" VALUES (");
            List<Object> params = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, String> entry : this.fieldToColumn.entrySet()) {
                String fieldName = entry.getKey();
                String columnName = entry.getValue();
                // 跳过主键Id
                if (fieldName.equals(idFieldName)) {
                    continue;
                }
                
                Object fieldValue = getFieldValue(entity, fieldName);
                if (count > 0) {
                    sql.append(",");
                    value.append(",");
                }
                sql.append(columnName);
                value.append("?");
                params.add(fieldValue);
                count++;
            }
            sql.append(")").append(value).append(")");
            
            // 检查ID字段是否为自增主键
            boolean isAutoIncrement = isAutoIncrementPrimaryKey();
            Long generatedId = null;
            
            if (isAutoIncrement) {
                // 使用支持返回自增ID的方法
                generatedId = JdbcUtil.executeInsertAndGetId(sql.toString(), params.toArray());
                if (generatedId != null) {
                    // 将自增ID设置回实体对象
                    setFieldValue(entity, idFieldName, generatedId);
                    log.debug("插入成功，已设置自增ID：{}", generatedId);
                }
            } else {
                // 非自增主键，使用普通更新方法
                int rows = JdbcUtil.executeUpdate(sql.toString(), params.toArray());
                return rows > 0;
            }
            
            return generatedId != null;
        } catch (Exception e) {
            throw new RuntimeException("保存实体失败, 实体类：" + entityClass.getName() + "异常: " + e.getMessage(), e);
        }
    }

    @Override
    public T findById(ID id) {
        try {
            // 使用正确的ID列名
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName)
                    .append(" WHERE ").append(this.idColumnName).append(" = ?");
            List<T> result = JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, id);
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            throw new RuntimeException("查询实体失败, 实体id：" + id + "异常: " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> findAll() {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(this.tableName);
            return JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity);
        } catch (Exception e) {
            throw new RuntimeException("列表查询失败, 异常: " + e.getMessage());
        }
    }

    @Override
    public boolean update(T entity) {
        try {
            Object id = getFieldValue(entity, idFieldName);
            if (id == null) {
                throw new RuntimeException("更新失败：实体主键值为空");
            }
            StringBuilder sql = new StringBuilder("UPDATE ").append(this.tableName).append(" SET ");
            List<Object> params = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, String> entry: this.fieldToColumn.entrySet()) {
                if (entry.getKey().equals(idFieldName)) {
                    continue;
                }
                String fieldName = entry.getKey();
                String columnName = entry.getValue();
                Object fieldValue = getFieldValue(entity, fieldName);
                if (count > 0) {
                    sql.append(",");
                }
                sql.append(columnName).append(" = ?");
                count++;
                params.add(fieldValue);
            }
            // 使用正确的ID列名
            sql.append(" WHERE ").append(idColumnName).append(" = ?");
            params.add(id);
            int row = JdbcUtil.executeUpdate(sql.toString(), params.toArray());
            return row > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新实体失败, 实体类：" + entityClass.getName() + "异常: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        try {
            // 使用正确的ID列名
            StringBuilder sql = new StringBuilder("DELETE FROM ").append(this.tableName)
                    .append(" WHERE ").append(this.idColumnName).append(" = ?");
            int rows = JdbcUtil.executeUpdate(sql.toString(), id);
            return rows > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除实体失败, 实体id：" + id + "异常: " + e.getMessage(), e);
        }
    }
}
