package io.github.yangyaofei.modelmapper;

import com.google.common.reflect.TypeToken;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.record.RecordModule;

/**
 * 使用 ModelMapper 进行 Entity 和 DTO 的转换, 提供两个转换方式, 一个是默认的, 一个是排除 null 值的.
 */

@SuppressWarnings("unused")
public class DtoMapperUtils {
    private static final ModelMapper defaultModelMapper = getModelMapper();

    private static final ModelMapper copyNonnullModelMapper = getCopyNonnullModelMapper();

    /**
     * 获取默认的 ModelMapper, 匹配为严格模式
     *
     * @return modelMapper
     */
    protected static ModelMapper getModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.registerModule(new RecordModule())
                .getConfiguration()
                .setCollectionsMergeEnabled(false)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    /**
     * 获取排除 null 值的 ModelMapper, 匹配为严格模式.
     *
     * @return modelMapper
     */
    protected static ModelMapper getCopyNonnullModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.registerModule(new RecordModule())
                .getConfiguration()
                .setCollectionsMergeEnabled(false)
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setPropertyCondition(Conditions.isNotNull());
        return modelMapper;
    }

    /**
     * 默认的 Mapper, 不排除 null 值.
     *
     * @param <Entity>    the type parameter
     * @param <Dto>       the type parameter
     * @param dto         the dto
     * @param entityClass the entity class
     * @return the entity
     */
    public static <Entity, Dto> Entity map(Dto dto, Class<Entity> entityClass) {
        return defaultModelMapper.map(dto, entityClass);
    }

    /**
     * 指定 Entity, 转换 DTO 中的非空值进行转换.
     *
     * @param <Entity> Entity 类型
     * @param <Dto>    DTO 类型r
     * @param dto      DTO 实例
     * @param entity   已有的 Entity 实例, 将DTO的值复制此实例中
     * @return the entity
     */
    public static <Entity, Dto> Entity map(Dto dto, Entity entity) {
        copyNonnullModelMapper.map(dto, entity);
        return entity;
    }

    /**
     * DtoMapperInterface 接口, 实现 Dto 转换为 Entity.
     * 实现此接口的 DTO 可以使用 {@link #map(Object entity)} 方法进行非空转换, {@link #map()} 方法进行默认转换.
     *
     * @param <Entity> DTO 对应的 Entity 类型
     */
    public interface DtoMapperInterface<Entity> {

        /**
         * 实现自定义的 ModelMapper, 默认此函数返回空, 若非空则 map 函数使用此接口
         *
         * @return modelMapper
         */
        default ModelMapper modelMapper() {
            return null;
        }
        /**
         * 默认的转换方式, 不排除 null 值.
         *
         * @return entity
         */
        default Entity map() {
            TypeToken<Entity> typeToken = new TypeToken<>(getClass()) {
            };
            ModelMapper modelMapper = modelMapper();
            if (modelMapper == null) {
                modelMapper = defaultModelMapper;
            }
            return modelMapper.map(this, typeToken.getType());
        }

        /**
         * 指定 Entity, 转换 DTO 中的非空值进行转换.
         *
         * @param entity 已有的 Entity 实例, 将DTO的值复制此实例中
         * @return entity
         */
        default Entity map(Entity entity) {
            ModelMapper modelMapper = modelMapper();
            if (modelMapper == null) {
                modelMapper = copyNonnullModelMapper;
            }
            modelMapper.map(this, entity);
            return entity;
        }

    }
}
